package com.earnit.app

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.earnit.app.ui.Strings

/**
 * Creates a task via the Tasks tab's New Task FAB, filling only the name field, and saves it.
 * Flows that set additional fields (group, reward links, mandatory/repeatable) or reach the
 * form via a different entry point stay inline at their call site.
 */
fun ComposeTestRule.createTask(name: String) {
    onNodeWithContentDescription("Tasks").performClick()
    onNodeWithContentDescription(Strings.NEW_TASK_DESC).performClick()
    onNodeWithText(Strings.TASK_NAME_LABEL).performTextInput(name)
    onNodeWithText("SAVE").performClick()
}

/**
 * Creates a reward via the Prizes tab's New Reward FAB, filling the name (and optionally
 * overriding the point cost), and saves it. Flows that link a task before saving or reach the
 * form via a different entry point stay inline at their call site.
 */
fun ComposeTestRule.createReward(
    name: String,
    cost: String? = null,
) {
    onNodeWithContentDescription("Prizes").performClick()
    onNodeWithContentDescription(Strings.NEW_REWARD_DESC).performClick()
    onNodeWithText(Strings.REWARD_NAME_LABEL).performTextInput(name)
    if (cost != null) {
        onNodeWithText(Strings.REWARD_COST_LABEL).performTextClearance()
        onNodeWithText(Strings.REWARD_COST_LABEL).performTextInput(cost)
    }
    onNodeWithText("SAVE").performClick()
}

/** Waits for Task Detail to appear, identified by its unique "Points:" label. */
fun ComposeTestRule.waitForTaskDetail() {
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText(Strings.TASK_DETAIL_POINTS_LABEL).fetchSemanticsNodes().isNotEmpty()
    }
}

/** Waits for Reward Detail to appear with no tasks yet linked, identified by its empty-state text. */
fun ComposeTestRule.waitForRewardDetail() {
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText(Strings.REWARD_DETAIL_NO_TASKS).fetchSemanticsNodes().isNotEmpty()
    }
}
