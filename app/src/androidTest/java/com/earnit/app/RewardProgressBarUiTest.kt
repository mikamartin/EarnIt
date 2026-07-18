package com.earnit.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earnit.app.data.SettingsRepository
import com.earnit.app.tags.Reward
import com.earnit.app.tags.UiTest
import com.earnit.app.ui.Strings
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Confirms RewardDetailScreen actually wires RewardProgress.showsProgressNumbers to the progress
 * bar's number overlay. The boundary cases for that property are unit-tested in
 * RewardProgressTest.kt; this is the rendering-level check that the Composable reads it correctly.
 */
@UiTest
@Reward
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RewardProgressBarUiTest {
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

    private fun waitForTaskDetail() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Points:").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun pointsMeetCostButMandatoryTaskUnlogged_hidesProgressBarNumbers() {
        // Reward cost matches the default 4-point task so a single log reaches full progress.
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription("New Reward").performClick()
        composeTestRule.onNodeWithText("Reward name").performTextInput("Study Time")
        composeTestRule.onNodeWithText("Point cost").performTextClearance()
        composeTestRule.onNodeWithText("Point cost").performTextInput("4")
        composeTestRule.onNodeWithText("SAVE").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(Strings.REWARD_DETAIL_NO_TASKS).fetchSemanticsNodes().isNotEmpty()
        }

        // A mandatory task linked to the reward, deliberately left unlogged.
        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithContentDescription("New Task").performClick()
        composeTestRule.onNodeWithText(Strings.TASK_NAME_LABEL).performTextInput("Read Book")
        composeTestRule.onNodeWithText("Study Time").performClick()
        composeTestRule.onNodeWithContentDescription(Strings.TASK_OPTIONAL_DESC).performClick()
        composeTestRule.onNodeWithText("SAVE").performClick()
        waitForTaskDetail()

        // A second, optional task worth the same 4 points — logging it alone meets the cost.
        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithContentDescription("New Task").performClick()
        composeTestRule.onNodeWithText(Strings.TASK_NAME_LABEL).performTextInput("Do Homework")
        composeTestRule.onNodeWithText("Study Time").performClick()
        composeTestRule.onNodeWithText("SAVE").performClick()
        waitForTaskDetail()

        // Checkpoint: both tasks linked to the reward before logging either.
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithText("Study Time").performClick()
        composeTestRule.onNodeWithText("Read Book").assertIsDisplayed()
        composeTestRule.onNodeWithText("Do Homework").assertIsDisplayed()

        // Log only the optional task — mandatory "Read Book" stays undone.
        composeTestRule.onNodeWithText(Strings.LOG_BTN).performClick()
        composeTestRule.onAllNodesWithText("Do Homework").filterToOne(hasClickAction()).performClick()
        composeTestRule.onNodeWithText(Strings.DIALOG_LOG_BTN).performClick()

        // Checkpoint: points now meet the cost, but canClaim is gated on the mandatory task —
        // confirm that precondition explicitly, so the final assertion below can't pass for the
        // wrong reason (canClaim's own overlay path also hides these numbers).
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithText(Strings.REWARD_MANDATORY_TASKS_HINT)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText(Strings.REWARD_MANDATORY_TASKS_HINT).assertIsDisplayed()
        assertTrue(
            "Precondition: reward must not be claimable yet (mandatory task still unlogged)",
            composeTestRule.onAllNodesWithText(Strings.REWARD_DETAIL_CLAIM_BTN).fetchSemanticsNodes().isEmpty(),
        )

        // Neither the current-points nor the cost overlay ("4" bare, not "+4") should render.
        assertTrue(
            "Progress bar must hide the point/cost numbers once the bar is full but blocked on a mandatory task",
            composeTestRule.onAllNodesWithText("4").fetchSemanticsNodes().isEmpty(),
        )
    }
}
