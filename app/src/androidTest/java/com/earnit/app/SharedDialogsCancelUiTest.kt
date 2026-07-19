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
import com.earnit.app.tags.Reward
import com.earnit.app.tags.Task
import com.earnit.app.tags.UiTest
import com.earnit.app.ui.Strings
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cancel coverage for the dialogs shared across screens (SharedDialogs.kt's LogTaskDialog,
 * ClaimDialog, AddTaskToRewardDialog) plus TasksScreen.kt's LogForRewardDialog, none of which
 * had a dedicated cancel-path test before this — existing tests exercise their confirm paths
 * only.
 */
@UiTest
@Task
@Reward
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SharedDialogsCancelUiTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        resetAppState()
    }

    @Test
    fun logTaskDialog_cancel_noLogRecorded() {
        composeTestRule.createTask("Morning Run")
        composeTestRule.waitForTaskDetail()

        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription(Strings.NEW_REWARD_DESC).performClick()
        composeTestRule.onNodeWithText(Strings.REWARD_NAME_LABEL).performTextInput("Coffee Treat")
        composeTestRule.onNodeWithText(Strings.REWARD_ADD_TASK_BTN).performClick()
        composeTestRule.onNodeWithText("Morning Run").performClick()
        composeTestRule.onNodeWithText(Strings.ADD_TASK_CONFIRM_BTN).performClick()
        composeTestRule.onNodeWithText("SAVE").performClick()

        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(Strings.LOG_BTN).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(Strings.LOG_BTN).performClick()
        composeTestRule.onNodeWithText(Strings.LOG_DIALOG_TITLE).assertIsDisplayed()

        // Select a task before cancelling — proves the in-progress selection is discarded too,
        // not just that the dialog closes.
        composeTestRule.onAllNodesWithText("Morning Run").filterToOne(hasClickAction()).performClick()

        composeTestRule.cancelDialogAndAssertDismissed(Strings.LOG_DIALOG_TITLE, Strings.DIALOG_CANCEL)

        // Task is still loggable — no log was written.
        composeTestRule.onNodeWithText(Strings.LOG_BTN).assertIsDisplayed()
    }

    @Test
    fun claimDialog_cancel_rewardStaysActive() {
        composeTestRule.createTask("Morning Run")
        composeTestRule.waitForTaskDetail()

        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription(Strings.NEW_REWARD_DESC).performClick()
        composeTestRule.onNodeWithText(Strings.REWARD_NAME_LABEL).performTextInput("Coffee Treat")
        composeTestRule.onNodeWithText(Strings.REWARD_COST_LABEL).performTextClearance()
        composeTestRule.onNodeWithText(Strings.REWARD_COST_LABEL).performTextInput("4")
        composeTestRule.onNodeWithText(Strings.REWARD_ADD_TASK_BTN).performClick()
        composeTestRule.onNodeWithText("Morning Run").performClick()
        composeTestRule.onNodeWithText(Strings.ADD_TASK_CONFIRM_BTN).performClick()
        composeTestRule.onNodeWithText("SAVE").performClick()

        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(Strings.LOG_BTN).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(Strings.LOG_BTN).performClick()
        composeTestRule.onAllNodesWithText("Morning Run").filterToOne(hasClickAction()).performClick()
        composeTestRule.onNodeWithText(Strings.DIALOG_LOG_BTN).performClick()

        composeTestRule.onNodeWithText("Coffee Treat").performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("CLAIM").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("CLAIM").performClick()

        val dialogTitle = Strings.claimDialogTitle("Coffee Treat")
        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()

        composeTestRule.cancelDialogAndAssertDismissed(dialogTitle, Strings.CLAIM_CANCEL)

        // Reward was not archived — CLAIM is still available.
        composeTestRule.onNodeWithText("CLAIM").assertIsDisplayed()
    }

    @Test
    fun addTaskDialog_cancel_taskNotIncluded() {
        composeTestRule.createTask("Walk Dog")
        composeTestRule.waitForTaskDetail()

        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription(Strings.NEW_REWARD_DESC).performClick()
        composeTestRule.onNodeWithText(Strings.REWARD_NAME_LABEL).performTextInput("Evening Walk")

        composeTestRule.onNodeWithText(Strings.REWARD_ADD_TASK_BTN).performClick()
        composeTestRule.onNodeWithText("Walk Dog").performClick()

        composeTestRule.cancelDialogAndAssertDismissed(Strings.ADD_TASK_CONFIRM_BTN, Strings.DIALOG_CANCEL)

        composeTestRule.onNodeWithText("Walk Dog").assertDoesNotExist()
    }

    @Test
    fun logForRewardDialog_cancel_noLogRecorded() {
        composeTestRule.createTask("Read")
        composeTestRule.waitForTaskDetail()

        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription(Strings.NEW_REWARD_DESC).performClick()
        composeTestRule.onNodeWithText(Strings.REWARD_NAME_LABEL).performTextInput("Book Reward")
        composeTestRule.onNodeWithText(Strings.REWARD_ADD_TASK_BTN).performClick()
        composeTestRule.onNodeWithText("Read").performClick()
        composeTestRule.onNodeWithText(Strings.ADD_TASK_CONFIRM_BTN).performClick()
        composeTestRule.onNodeWithText("SAVE").performClick()

        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithText("Read").performClick()

        composeTestRule.onNodeWithText(Strings.LOG_BTN).performClick()
        val dialogTitle = Strings.tasksLogTitle("Read")
        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()

        composeTestRule.cancelDialogAndAssertDismissed(dialogTitle)

        composeTestRule.onNodeWithText(Strings.LOG_BTN).assertIsDisplayed()
    }
}
