package com.earnit.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earnit.app.data.SettingsRepository
import com.earnit.app.tags.Reward
import com.earnit.app.tags.Task
import com.earnit.app.tags.UiTest
import com.earnit.app.ui.Strings
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Once every task linked to a reward is non-repeatable and already logged,
 * RewardProgress.loggableTasks is empty (RewardProgressTest covers that boundary directly).
 * This is the rendering-level check that both the Reward Detail screen and the Prizes home
 * card read it correctly: LOG disables and an explanatory hint appears, instead of the button
 * silently doing nothing or opening an empty log dialog.
 */
@UiTest
@Reward
@Task
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RewardAllTasksLoggedHintUiTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var settingsRepository: SettingsRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        resetAppState()
        runBlocking { settingsRepository.updateNotesMandatory(false) }
    }

    /** Creates "Coffee Treat" with a single non-repeatable, optional task and logs it. */
    private fun createRewardWithOneLoggedTask() {
        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithContentDescription("New Task").performClick()
        composeTestRule.onNodeWithText("Task name").performClick()
        composeTestRule.onNodeWithText("Task name").performTextInput("Morning Run")
        composeTestRule.onNodeWithText("SAVE").performClick()

        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription("New Reward").performClick()
        composeTestRule.onNodeWithText("Reward name").performClick()
        composeTestRule.onNodeWithText("Reward name").performTextInput("Coffee Treat")
        composeTestRule.onNodeWithText("SAVE").performClick()

        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithText("Coffee Treat").performClick()
        composeTestRule.onNodeWithText("Add task").performClick()
        composeTestRule.onNodeWithText("Morning Run").performClick()
        // Tasks default to repeatable when added to a reward — flip it off so logging it once
        // exhausts loggableTasks, which is this test class's entire premise.
        composeTestRule.onNodeWithContentDescription(Strings.REWARD_REPEATABLE_DESC).performClick()
        composeTestRule.onNodeWithText("ADD SELECTED").performClick()
        composeTestRule.onNodeWithText("Morning Run").assertIsDisplayed()

        // Log the task — now non-repeatable, so this exhausts loggableTasks.
        composeTestRule.onNodeWithText(Strings.LOG_BTN).performClick()
        composeTestRule.onAllNodesWithText("Morning Run").filterToOne(hasClickAction()).performClick()
        composeTestRule.onNodeWithText(Strings.DIALOG_LOG_BTN).performClick()
    }

    @Test
    fun rewardDetail_allOneTimeTasksLogged_disablesLogAndShowsHint() {
        createRewardWithOneLoggedTask()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithText(Strings.REWARD_ALL_TASKS_LOGGED_HINT)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText(Strings.REWARD_ALL_TASKS_LOGGED_HINT).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings.LOG_BTN).assertIsNotEnabled()
    }

    @Test
    fun homeCard_allOneTimeTasksLogged_disablesLogAndShowsHint() {
        createRewardWithOneLoggedTask()

        // Back out to the Prizes list, where the reward's card renders its own LOG button.
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithText(Strings.REWARD_ALL_TASKS_LOGGED_HINT)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText(Strings.REWARD_ALL_TASKS_LOGGED_HINT).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings.LOG_BTN).assertIsNotEnabled()
    }
}
