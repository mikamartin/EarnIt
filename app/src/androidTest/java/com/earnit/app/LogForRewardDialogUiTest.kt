package com.earnit.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.ComposeTestRule
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
 * Coverage for `LogForRewardDialog`'s multi-reward branch (TasksScreen.kt) — the reward picker
 * shown when a task is linked to more than one reward. Previously untested: every other
 * `LogForRewardDialog` test links a task to exactly one reward, which takes the dialog's
 * single-reward branch and never renders the `RadioRow` list.
 */
@UiTest
@Task
@Reward
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LogForRewardDialogUiTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        resetAppState()
    }

    /**
     * Scoped to the dialog's own window — TaskDetailScreen's "Used in Rewards" list behind
     * `LogForRewardDialog` repeats the same reward names, which would otherwise match twice.
     */
    private fun ComposeTestRule.dialogNodeWithText(text: String) = onNode(hasAnyAncestor(isDialog()) and hasText(text))

    @Test
    fun multiReward_requiresSelection_andLogsAgainstChosenRewardOnly() {
        composeTestRule.createTask("Push-ups")

        // Link the same task to two rewards, each costing exactly the task's default 4 points,
        // so a single log makes the chosen reward (and only that one) immediately claimable.
        for (rewardName in listOf("Movie Night", "Book Time")) {
            composeTestRule.onNodeWithContentDescription("Prizes").performClick()
            composeTestRule.onNodeWithContentDescription(Strings.NEW_REWARD_DESC).performClick()
            composeTestRule.onNodeWithText(Strings.REWARD_NAME_LABEL).performTextInput(rewardName)
            composeTestRule.onNodeWithText(Strings.REWARD_COST_LABEL).performTextClearance()
            composeTestRule.onNodeWithText(Strings.REWARD_COST_LABEL).performTextInput("4")
            composeTestRule.onNodeWithText(Strings.REWARD_ADD_TASK_BTN).performClick()
            composeTestRule.onNodeWithText("Push-ups").performClick()
            composeTestRule.onNodeWithText(Strings.ADD_TASK_CONFIRM_BTN).performClick()
            composeTestRule.onNodeWithText("SAVE").performClick()
        }

        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithText("Push-ups").performClick()
        composeTestRule.onNodeWithText(Strings.LOG_BTN).performClick()

        // Multi-reward branch: the select-reward prompt and both reward names as options.
        composeTestRule.onNodeWithText(Strings.TASKS_LOG_SELECT_REWARD).assertIsDisplayed()
        composeTestRule.dialogNodeWithText("Movie Night").assertIsDisplayed()
        composeTestRule.dialogNodeWithText("Book Time").assertIsDisplayed()

        // No reward pre-selected — LOG stays disabled until the user picks one.
        composeTestRule.onNodeWithText(Strings.DIALOG_LOG_BTN).assertIsNotEnabled()

        composeTestRule.dialogNodeWithText("Book Time").performClick()
        composeTestRule.onNodeWithText(Strings.DIALOG_LOG_BTN).assertIsEnabled()
        composeTestRule.onNodeWithText(Strings.DIALOG_LOG_BTN).performClick()

        // Credited to the chosen reward only.
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithText("Book Time").performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText(Strings.REWARD_DETAIL_CLAIM_BTN).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(Strings.REWARD_DETAIL_CLAIM_BTN).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithText("Movie Night").performClick()
        composeTestRule.onNodeWithText(Strings.REWARD_DETAIL_CLAIM_BTN).assertDoesNotExist()
    }
}
