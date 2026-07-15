package com.earnit.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onSiblings
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earnit.app.ui.Strings
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI regression tests for the sections of RewardEditScreen split into private sub-composables
 * on refactor/split-reward-edit-screen: delete confirmation, icon picker, the cost field's
 * digit filter, the included-task row's mandatory/repeatable toggles and uncheck-to-remove
 * behavior (including isolation across multiple rows), editing an existing reward's fields,
 * task-link pre-population on an already-linked reward, selecting an existing task through
 * AddTaskToRewardDialog's own checkbox list (as opposed to the "create new" shortcut), and
 * the Browse Library entry point. Also pins a known pre-existing bug (see CLEANUP_BACKLOG.md):
 * adding two new tasks in a row via "Create your own" on an unsaved reward silently drops the
 * first one's inclusion.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RewardEditScreenUiTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        resetAppState()
    }

    private fun waitForRewardDetail() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(Strings.REWARD_DETAIL_NO_TASKS).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun deleteReward_confirmRemovesRewardAndNavigatesBack() {
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription(Strings.NEW_REWARD_DESC).performClick()
        composeTestRule.onNodeWithText(Strings.REWARD_NAME_LABEL).performTextInput("Delete Me Reward")
        composeTestRule.onNodeWithText("SAVE").performClick()
        waitForRewardDetail()

        composeTestRule.onNodeWithContentDescription(Strings.EDIT_REWARD_DESC).performClick()
        composeTestRule.onNodeWithText(Strings.REWARD_EDIT_EXISTING).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(Strings.DELETE_REWARD_DESC).performClick()
        composeTestRule.onNodeWithText(Strings.REWARD_DELETE_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings.rewardDeleteBody("Delete Me Reward")).assertIsDisplayed()

        composeTestRule.onNodeWithText("DELETE").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Delete Me Reward").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun iconPicker_selectingEmojiUpdatesButtonAndDismissesDialog() {
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription(Strings.NEW_REWARD_DESC).performClick()

        // Default icon button shows the placeholder target emoji until one is picked.
        composeTestRule.onNodeWithText("🎯").performClick()
        composeTestRule.onNodeWithText(Strings.TASK_ICON_PICKER_TITLE).assertIsDisplayed()

        composeTestRule.onNodeWithText("🏆").performClick()

        composeTestRule.onNodeWithText(Strings.TASK_ICON_PICKER_TITLE).assertDoesNotExist()
        composeTestRule.onNodeWithText("🏆").assertIsDisplayed()
    }

    @Test
    fun costField_digitFilterStripsNonDigits() {
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription(Strings.NEW_REWARD_DESC).performClick()

        // Cost defaults to "10" on a new reward — clear it before typing the filter case.
        composeTestRule.onNodeWithText(Strings.REWARD_COST_LABEL).performTextClearance()
        composeTestRule.onNodeWithText(Strings.REWARD_COST_LABEL).performTextInput("12ab34")

        composeTestRule.onNodeWithText("1234").assertIsDisplayed()
        composeTestRule.onNodeWithText("12ab34").assertDoesNotExist()
    }

    @Test
    fun taskRow_mandatoryRepeatableTogglesAndUncheckRemoves() {
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription(Strings.NEW_REWARD_DESC).performClick()
        composeTestRule.onNodeWithText(Strings.REWARD_NAME_LABEL).performTextInput("Study Time")

        composeTestRule.onNodeWithText(Strings.REWARD_ADD_TASK_BTN).performClick()
        composeTestRule.onNodeWithText(Strings.ADD_TASK_CREATE).performClick()
        composeTestRule.onNodeWithText(Strings.TASK_NAME_LABEL).performTextInput("Read Book")
        composeTestRule.onNodeWithText("SAVE").performClick()

        // Back on Reward Edit — the newly created task is auto-included, defaulting to
        // optional + not-repeatable.
        composeTestRule.onNodeWithText("Read Book").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(Strings.REWARD_OPTIONAL_DESC).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(Strings.REWARD_NOT_REPEATABLE_DESC).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(Strings.REWARD_OPTIONAL_DESC).performClick()
        composeTestRule.onNodeWithContentDescription(Strings.REWARD_MANDATORY_DESC).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(Strings.REWARD_NOT_REPEATABLE_DESC).performClick()
        composeTestRule.onNodeWithContentDescription(Strings.REWARD_REPEATABLE_DESC).assertIsDisplayed()

        // Unchecking the row removes the task entirely (mandatory/repeatable reset with it).
        composeTestRule.onNodeWithContentDescription(Strings.REWARD_INCLUDED_DESC).performClick()
        composeTestRule.onNodeWithText("Read Book").assertDoesNotExist()
    }

    @Test
    fun editExistingReward_updatesFieldsAndPersists() {
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription(Strings.NEW_REWARD_DESC).performClick()
        composeTestRule.onNodeWithText(Strings.REWARD_NAME_LABEL).performTextInput("Original Reward")
        composeTestRule.onNodeWithText("SAVE").performClick()
        waitForRewardDetail()

        composeTestRule.onNodeWithContentDescription(Strings.EDIT_REWARD_DESC).performClick()
        composeTestRule.onNodeWithText(Strings.REWARD_EDIT_EXISTING).assertIsDisplayed()

        composeTestRule.onNodeWithText("Original Reward").performTextClearance()
        composeTestRule.onNodeWithText(Strings.REWARD_NAME_LABEL).performTextInput("Updated Reward")

        composeTestRule.onNodeWithText(Strings.REWARD_COST_LABEL).performTextClearance()
        composeTestRule.onNodeWithText(Strings.REWARD_COST_LABEL).performTextInput("25")

        composeTestRule.onNodeWithText(Strings.REWARD_DESC_LABEL).performTextInput("A well-earned break")

        composeTestRule.onNodeWithText("🎯").performClick()
        composeTestRule.onNodeWithText("🏆").performClick()

        composeTestRule.onNodeWithText("SAVE").performClick()

        // Editing an existing reward pops back to Reward Detail rather than navigating forward.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Updated Reward").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("🏆").assertIsDisplayed()
        composeTestRule.onNodeWithText("Original Reward").assertDoesNotExist()
    }

    @Test
    fun editExistingReward_taskLinksPrepopulateFromExistingLinks() {
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription(Strings.NEW_REWARD_DESC).performClick()
        composeTestRule.onNodeWithText(Strings.REWARD_NAME_LABEL).performTextInput("Movie Night")

        composeTestRule.onNodeWithText(Strings.REWARD_ADD_TASK_BTN).performClick()
        composeTestRule.onNodeWithText(Strings.ADD_TASK_CREATE).performClick()
        composeTestRule.onNodeWithText(Strings.TASK_NAME_LABEL).performTextInput("Clean Room")
        composeTestRule.onNodeWithText("SAVE").performClick()

        // Mark the auto-included task mandatory before saving the reward.
        composeTestRule.onNodeWithContentDescription(Strings.REWARD_OPTIONAL_DESC).performClick()
        composeTestRule.onNodeWithText("SAVE").performClick()

        // This reward has a task, so Reward Detail shows the task list, not the empty-state
        // copy waitForRewardDetail() waits for — wait for the task itself to confirm the save
        // landed before reopening the edit screen.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Clean Room").fetchSemanticsNodes().isNotEmpty()
        }

        // Reopening the reward must pre-populate the task row as included + mandatory.
        composeTestRule.onNodeWithContentDescription(Strings.EDIT_REWARD_DESC).performClick()
        composeTestRule.onNodeWithText("Clean Room").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(Strings.REWARD_MANDATORY_DESC).assertIsDisplayed()
    }

    @Test
    fun taskRows_multipleTasksToggleAndRemoveIndependently() {
        // Create two standalone tasks first, then add both in a single dialog session via the
        // existing-task checkbox list. Adding them one at a time via "Create your own" instead
        // would round-trip through TaskEditScreen twice, which currently drops the first task's
        // inclusion when the second one lands (see sequentialCreateNewTasks_... below and
        // CLEANUP_BACKLOG.md) — an unrelated, pre-existing bug this test intentionally avoids so
        // it can isolate what it's actually testing: per-row toggle independence.
        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithContentDescription(Strings.NEW_TASK_DESC).performClick()
        composeTestRule.onNodeWithText(Strings.TASK_NAME_LABEL).performTextInput("Vacuum")
        composeTestRule.onNodeWithText("SAVE").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(Strings.TASK_DETAIL_POINTS_LABEL).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithContentDescription(Strings.NEW_TASK_DESC).performClick()
        composeTestRule.onNodeWithText(Strings.TASK_NAME_LABEL).performTextInput("Dishes")
        composeTestRule.onNodeWithText("SAVE").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(Strings.TASK_DETAIL_POINTS_LABEL).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription(Strings.NEW_REWARD_DESC).performClick()
        composeTestRule.onNodeWithText(Strings.REWARD_NAME_LABEL).performTextInput("Cleanup Weekend")

        composeTestRule.onNodeWithText(Strings.REWARD_ADD_TASK_BTN).performClick()
        composeTestRule.onNodeWithText("Vacuum").performClick()
        composeTestRule.onNodeWithText("Dishes").performClick()
        composeTestRule.onNodeWithText(Strings.ADD_TASK_CONFIRM_BTN).performClick()

        // Scope every toggle/assertion to the specific row via onSiblings() rather than a bare
        // content-description lookup — REWARD_OPTIONAL_DESC etc. aren't unique per row, and the
        // LazyColumn's viewport may not keep both rows composed at once on a small screen, so a
        // global onNodeWithContentDescription() query can't reliably tell the rows apart.
        composeTestRule
            .onNodeWithText("Vacuum")
            .performScrollTo()
            .onSiblings()
            .filterToOne(hasContentDescription(Strings.REWARD_OPTIONAL_DESC))
            .performClick()

        // Vacuum's row is now mandatory — Dishes' row must be untouched by that toggle.
        composeTestRule
            .onNodeWithText("Vacuum")
            .onSiblings()
            .filterToOne(hasContentDescription(Strings.REWARD_MANDATORY_DESC))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Dishes")
            .performScrollTo()
            .onSiblings()
            .filterToOne(hasContentDescription(Strings.REWARD_OPTIONAL_DESC))
            .assertIsDisplayed()

        // Removing Vacuum's row via its own checkbox must not remove Dishes' row.
        composeTestRule
            .onNodeWithText("Vacuum")
            .performScrollTo()
            .onSiblings()
            .filterToOne(hasContentDescription(Strings.REWARD_INCLUDED_DESC))
            .performClick()
        composeTestRule.onNodeWithText("Vacuum").assertDoesNotExist()
        composeTestRule.onNodeWithText("Dishes").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun sequentialCreateNewTasks_onUnsavedReward_bothStayIncluded() {
        // Each "Create your own" tap round-trips through TaskEditScreen and back, disposing and
        // recreating RewardEditScreen's composition — taskState must survive that via
        // rememberSaveable, or the earlier task silently drops out.
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription(Strings.NEW_REWARD_DESC).performClick()
        composeTestRule.onNodeWithText(Strings.REWARD_NAME_LABEL).performTextInput("Cleanup Weekend")

        composeTestRule.onNodeWithText(Strings.REWARD_ADD_TASK_BTN).performClick()
        composeTestRule.onNodeWithText(Strings.ADD_TASK_CREATE).performClick()
        composeTestRule.onNodeWithText(Strings.TASK_NAME_LABEL).performTextInput("Vacuum")
        composeTestRule.onNodeWithText("SAVE").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Vacuum").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText(Strings.REWARD_ADD_TASK_BTN).performClick()
        composeTestRule.onNodeWithText(Strings.ADD_TASK_CREATE).performClick()
        composeTestRule.onNodeWithText(Strings.TASK_NAME_LABEL).performTextInput("Dishes")
        composeTestRule.onNodeWithText("SAVE").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Dishes").fetchSemanticsNodes().isNotEmpty()
        }

        // Both tasks must have survived the two round-trips through TaskEditScreen.
        composeTestRule.onNodeWithText("Vacuum").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Dishes").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun existingTaskSelection_viaDialogCarriesMandatoryFlagThroughToIncludedList() {
        // Create a standalone task first — not linked to any reward yet.
        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithContentDescription(Strings.NEW_TASK_DESC).performClick()
        composeTestRule.onNodeWithText(Strings.TASK_NAME_LABEL).performTextInput("Walk Dog")
        composeTestRule.onNodeWithText("SAVE").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(Strings.TASK_DETAIL_POINTS_LABEL).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription(Strings.NEW_REWARD_DESC).performClick()
        composeTestRule.onNodeWithText(Strings.REWARD_NAME_LABEL).performTextInput("Evening Walk")

        composeTestRule.onNodeWithText(Strings.REWARD_ADD_TASK_BTN).performClick()

        // Select the existing task via the dialog's own checkbox list (not "create new"), and
        // mark it mandatory using the dialog's inline star toggle before confirming.
        composeTestRule.onNodeWithText("Walk Dog").performClick()
        composeTestRule.onNodeWithContentDescription(Strings.REWARD_OPTIONAL_DESC).performClick()
        composeTestRule.onNodeWithText(Strings.ADD_TASK_CONFIRM_BTN).performClick()

        // Back on Reward Edit — the mandatory flag set inside the dialog must carry through.
        composeTestRule.onNodeWithText("Walk Dog").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(Strings.REWARD_MANDATORY_DESC).assertIsDisplayed()
    }

    @Test
    fun addTaskDialog_browseLibraryNavigatesToTaskLibrary() {
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription(Strings.NEW_REWARD_DESC).performClick()
        composeTestRule.onNodeWithText(Strings.REWARD_NAME_LABEL).performTextInput("Something")

        composeTestRule.onNodeWithText(Strings.REWARD_ADD_TASK_BTN).performClick()
        // No tasks exist yet (fresh app state), so the dialog shows its empty state with the
        // Browse Library shortcut rather than the checkbox list.
        composeTestRule.onNodeWithText(Strings.ADD_TASK_EMPTY).assertIsDisplayed()

        composeTestRule.onNodeWithText(Strings.ADD_TASK_BROWSE).performClick()

        composeTestRule.onNodeWithText(Strings.LIBRARY_TITLE).assertIsDisplayed()
    }
}
