package com.earnit.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earnit.app.data.EarnItRepository
import com.earnit.app.data.RewardEntity
import com.earnit.app.data.SettingsRepository
import com.earnit.app.data.TaskEntity
import com.earnit.app.tags.Reward
import com.earnit.app.tags.UiTest
import com.earnit.app.ui.Strings
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Covers the Claimed Rewards tab's "Earn Again" icon button (HistoryScreen's ClaimedRewardsTab).
 * copyRewardFromEntry's own flag/description-preservation, duplicate-name guard, and
 * reward-count-cap guard are unit-tested in RepositoryBehaviourTest; these tests cover the UI
 * wiring on top of it — that tapping the on-screen icon (found by its content description,
 * since it has no visible label) reaches the repository, a new active reward shows up where
 * the user would look for it, and the button gives feedback via a snackbar rather than acting
 * silently, whichever of the three outcomes (added / duplicate / at cap) actually occurs.
 */
@UiTest
@Reward
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class EarnAgainButtonUiTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var repository: EarnItRepository

    @Inject lateinit var settingsRepository: SettingsRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        resetAppState()
    }

    @Test
    fun tapEarnAgain_createsNewActiveRewardWithSameNameAndCost() =
        runBlocking {
            val rewardId = repository.upsertReward(RewardEntity(name = "Coffee Treat", cost = 5))
            repository.claimReward(rewardId, startOver = false)

            composeTestRule.onNodeWithContentDescription("History").performClick()
            composeTestRule.onNodeWithText(Strings.HISTORY_TAB_REWARDS).performClick()
            composeTestRule.onNodeWithText("Coffee Treat").assertIsDisplayed()

            composeTestRule.onNodeWithContentDescription(Strings.HISTORY_EARN_AGAIN).performClick()

            // Checkpoint: a snackbar confirms the reward was added, so the tap isn't silent.
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule
                    .onAllNodesWithText(Strings.historyEarnAgainAdded("Coffee Treat"))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            // Checkpoint: a new, active "Coffee Treat" reward appears on Prizes.
            composeTestRule.onNodeWithContentDescription("Prizes").performClick()
            composeTestRule.onNodeWithText("Coffee Treat").assertIsDisplayed()

            val newCopies =
                repository
                    .observeUiState()
                    .first()
                    .rewardProgressList
                    .filter { it.reward.name == "Coffee Treat" }
            assertEquals("Earn Again should create exactly one new active reward", 1, newCopies.size)
            assertEquals(5, newCopies[0].reward.cost)
            assertFalse("New reward copy should not be archived", newCopies[0].reward.isArchived)
        }

    @Test
    fun tapEarnAgain_carriesOverLinkedTaskAndDescription(): Unit =
        runBlocking {
            val taskId = repository.upsertTask(TaskEntity(name = "Morning Run", points = 5))
            val rewardId =
                repository.upsertReward(
                    RewardEntity(name = "Coffee Treat", cost = 5, description = "A well-earned latte"),
                )
            repository.saveRewardTasks(rewardId, listOf(Triple(taskId, true, false)))
            val task = repository.getTaskOrNull(taskId)!!
            repository.logCompletion(task, rewardId = rewardId, detail = "Felt great")
            repository.claimReward(rewardId, startOver = false)

            composeTestRule.onNodeWithContentDescription("History").performClick()
            composeTestRule.onNodeWithText(Strings.HISTORY_TAB_REWARDS).performClick()
            composeTestRule.onNodeWithContentDescription(Strings.HISTORY_EARN_AGAIN).performClick()

            composeTestRule.onNodeWithContentDescription("Prizes").performClick()
            composeTestRule.onNodeWithText("Coffee Treat").performClick()

            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule.onAllNodesWithText("Morning Run").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("Morning Run").assertIsDisplayed()
            composeTestRule.onNodeWithText("A well-earned latte").assertIsDisplayed()
        }

    @Test
    fun tapEarnAgain_whenActiveCopyAlreadyExists_doesNotCreateASecondOne() =
        runBlocking {
            val rewardId = repository.upsertReward(RewardEntity(name = "Coffee Treat", cost = 5))
            repository.claimReward(rewardId, startOver = false)

            composeTestRule.onNodeWithContentDescription("History").performClick()
            composeTestRule.onNodeWithText(Strings.HISTORY_TAB_REWARDS).performClick()

            // First tap creates the active copy.
            composeTestRule.onNodeWithContentDescription(Strings.HISTORY_EARN_AGAIN).performClick()
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule
                    .onAllNodesWithText(Strings.historyEarnAgainAdded("Coffee Treat"))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            // Second tap on the same history row must not create a duplicate. The first
            // snackbar (SnackbarDuration.Short, ~4s) is still queued/visible at this point —
            // Material3 shows only one at a time — so this wait needs enough headroom for it
            // to finish before the second message can appear.
            composeTestRule.onNodeWithContentDescription(Strings.HISTORY_EARN_AGAIN).performClick()
            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                composeTestRule
                    .onAllNodesWithText(Strings.rewardDuplicateError("Coffee Treat"))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            val activeCopies =
                repository
                    .observeUiState()
                    .first()
                    .rewardProgressList
                    .filter { it.reward.name == "Coffee Treat" }
            assertEquals("A second tap must not create a duplicate active reward", 1, activeCopies.size)
        }

    @Test
    fun tapEarnAgain_whenAtMaxRewardCount_doesNotExceedTheLimit() =
        runBlocking {
            settingsRepository.updateMaxRewardCount(1)
            // The single active slot is already taken by an unrelated reward.
            repository.upsertReward(RewardEntity(name = "Movie Night", cost = 10))
            val rewardId = repository.upsertReward(RewardEntity(name = "Coffee Treat", cost = 5))
            repository.claimReward(rewardId, startOver = false)

            composeTestRule.onNodeWithContentDescription("History").performClick()
            composeTestRule.onNodeWithText(Strings.HISTORY_TAB_REWARDS).performClick()
            composeTestRule.onNodeWithContentDescription(Strings.HISTORY_EARN_AGAIN).performClick()

            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule
                    .onAllNodesWithText(Strings.MAX_REWARD_TOOLTIP)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            val activeRewards = repository.observeUiState().first().rewardProgressList
            assertEquals("The reward cap must not be exceeded", 1, activeRewards.size)
            assertEquals("Movie Night", activeRewards[0].reward.name)
        }
}
