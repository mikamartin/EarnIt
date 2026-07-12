package com.earnit.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
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
 * UI regression tests for the sections of TaskEditScreen split into private sub-composables
 * on refactor/split-task-edit-screen: delete confirmation, icon picker, group picker,
 * auto-points sliders, manual points entry, the reward-link checkboxes/toggles, editing an
 * existing task's fields, reward-link pre-population on an already-linked task, and the
 * add-task-from-an-existing-reward's-own-Detail-screen entry point.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TaskEditScreenUiTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        resetAppState()
    }

    private fun waitForTaskDetail() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Points:").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun deleteTask_confirmRemovesTaskAndNavigatesToList() {
        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithContentDescription("New Task").performClick()
        composeTestRule.onNodeWithText(Strings.TASK_NAME_LABEL).performTextInput("Delete Me")
        composeTestRule.onNodeWithText("SAVE").performClick()
        waitForTaskDetail()

        composeTestRule.onNodeWithContentDescription(Strings.EDIT_TASK_DESC).performClick()
        composeTestRule.onNodeWithText(Strings.TASK_EDIT_EXISTING).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(Strings.DELETE_TASK_DESC).performClick()
        composeTestRule.onNodeWithText(Strings.TASK_DELETE_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings.taskDeleteBody("Delete Me")).assertIsDisplayed()

        composeTestRule.onNodeWithText("DELETE").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Delete Me").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun iconPicker_selectingEmojiUpdatesButtonAndDismissesDialog() {
        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithContentDescription("New Task").performClick()

        // Default icon button shows the placeholder checkmark until an emoji is picked.
        composeTestRule.onNodeWithText("✅").performClick()
        composeTestRule.onNodeWithText(Strings.TASK_ICON_PICKER_TITLE).assertIsDisplayed()

        composeTestRule.onNodeWithText("🎯").performClick()

        composeTestRule.onNodeWithText(Strings.TASK_ICON_PICKER_TITLE).assertDoesNotExist()
        composeTestRule.onNodeWithText("🎯").assertIsDisplayed()
        composeTestRule.onNodeWithText("✅").assertDoesNotExist()
    }

    @Test
    fun groupPicker_selectExistingGroup_createNewGroup_clearNewGroup() {
        // Seed an existing group via a first task.
        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithContentDescription("New Task").performClick()
        composeTestRule.onNodeWithText(Strings.TASK_NAME_LABEL).performTextInput("Vacuum")
        composeTestRule.onNodeWithText(Strings.TASK_GROUP_PLACEHOLDER).performTextInput("Chores")
        composeTestRule.onNodeWithText("SAVE").performClick()
        waitForTaskDetail()

        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithContentDescription("New Task").performClick()

        // Selecting the existing group updates the collapsible header label.
        composeTestRule.onNodeWithText("Chores").assertIsDisplayed()
        composeTestRule.onNodeWithText("Chores").performClick()
        composeTestRule.onNodeWithText(Strings.taskGroupLabel("Chores")).assertIsDisplayed()

        // Typing a new group name clears the existing-group selection and updates the header.
        composeTestRule.onNodeWithText(Strings.TASK_GROUP_PLACEHOLDER).performTextInput("Weekend")
        composeTestRule.onNodeWithText(Strings.taskGroupLabel("Weekend")).assertIsDisplayed()

        // Clearing the new-group text reverts the header to the optional label.
        composeTestRule.onNodeWithContentDescription(Strings.CLEAR_DESC).performClick()
        composeTestRule.onNodeWithText(Strings.TASK_GROUP_OPTIONAL).assertIsDisplayed()
    }

    @Test
    fun autoPoints_slidersUpdateComputedTotal() {
        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithContentDescription("New Task").performClick()

        composeTestRule.onNodeWithContentDescription(Strings.TASK_AUTO_POINTS_DESC).performClick()

        composeTestRule.onNodeWithContentDescription("${Strings.TASK_SLIDER_TIME} 5").performClick()
        composeTestRule.onNodeWithContentDescription("${Strings.TASK_SLIDER_DIFFICULTY} 5").performClick()
        composeTestRule.onNodeWithContentDescription("${Strings.TASK_SLIDER_PREPARATION} 5").performClick()

        // computeAutoPoints(5, 5, 5) == 30 — see PointFormulaTest. Scroll it into view first —
        // the points total isn't guaranteed to be on-screen on every emulator viewport.
        composeTestRule.onNodeWithText("30").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun manualPoints_digitFilterStripsNonDigits() {
        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithContentDescription("New Task").performClick()

        // Auto-points is off by default for a new task — the manual Points field is shown, prefilled "4".
        composeTestRule.onNodeWithText(Strings.TASK_POINTS_LABEL).performTextClearance()
        composeTestRule.onNodeWithText(Strings.TASK_POINTS_LABEL).performTextInput("12ab34")

        composeTestRule.onNodeWithText("1234").assertIsDisplayed()
        composeTestRule.onNodeWithText("12ab34").assertDoesNotExist()
    }

    @Test
    fun rewardLinks_checkboxAndMandatoryRepeatableToggles() {
        // Seed a reward so the "Use to get:" section renders on the New Task form.
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription("New Reward").performClick()
        composeTestRule.onNodeWithText("Reward name").performTextInput("Bike Ride")
        composeTestRule.onNodeWithText("Point cost").performTextClearance()
        composeTestRule.onNodeWithText("Point cost").performTextInput("5")
        composeTestRule.onNodeWithText("SAVE").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithText(Strings.REWARD_DETAIL_NO_TASKS)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithContentDescription("New Task").performClick()

        composeTestRule.onNodeWithText(Strings.TASK_USE_TO_GET).assertIsDisplayed()
        composeTestRule.onNodeWithText("Bike Ride").assertIsDisplayed()

        // Star/refresh toggles start disabled — the task isn't linked to the reward yet.
        composeTestRule.onNodeWithContentDescription(Strings.TASK_OPTIONAL_DESC).assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription(Strings.TASK_ONCE_DESC).assertIsNotEnabled()

        // Checking the reward card includes the task and enables the toggles.
        composeTestRule.onNodeWithText("Bike Ride").performClick()
        composeTestRule.onNodeWithContentDescription(Strings.TASK_OPTIONAL_DESC).assertIsEnabled()
        composeTestRule.onNodeWithContentDescription(Strings.TASK_ONCE_DESC).assertIsEnabled()

        // Toggling mandatory/repeatable flips their icon and content description.
        composeTestRule.onNodeWithContentDescription(Strings.TASK_OPTIONAL_DESC).performClick()
        composeTestRule.onNodeWithContentDescription(Strings.TASK_MANDATORY_DESC).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(Strings.TASK_ONCE_DESC).performClick()
        composeTestRule.onNodeWithContentDescription(Strings.TASK_REPEATABLE_DESC).assertIsDisplayed()

        // Unchecking the reward card resets included/mandatory/repeatable together.
        composeTestRule.onNodeWithText("Bike Ride").performClick()
        composeTestRule.onNodeWithContentDescription(Strings.TASK_OPTIONAL_DESC).assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription(Strings.TASK_ONCE_DESC).assertIsNotEnabled()
    }

    @Test
    fun editExistingTask_updatesFieldsAndPersists() {
        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithContentDescription("New Task").performClick()
        composeTestRule.onNodeWithText(Strings.TASK_NAME_LABEL).performTextInput("Original Name")
        composeTestRule.onNodeWithText("SAVE").performClick()
        waitForTaskDetail()

        composeTestRule.onNodeWithContentDescription(Strings.EDIT_TASK_DESC).performClick()
        composeTestRule.onNodeWithText(Strings.TASK_EDIT_EXISTING).assertIsDisplayed()

        composeTestRule.onNodeWithText("Original Name").performTextClearance()
        composeTestRule.onNodeWithText(Strings.TASK_NAME_LABEL).performTextInput("Updated Name")

        composeTestRule.onNodeWithText("✅").performClick()
        composeTestRule.onNodeWithText("🏆").performClick()

        composeTestRule.onNodeWithText(Strings.TASK_GROUP_PLACEHOLDER).performTextInput("Errands")

        composeTestRule.onNodeWithText(Strings.TASK_POINTS_LABEL).performTextClearance()
        composeTestRule.onNodeWithText(Strings.TASK_POINTS_LABEL).performTextInput("7")

        composeTestRule.onNodeWithText("SAVE").performClick()

        // Editing an existing task pops back to Task Detail rather than navigating forward.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Updated Name").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("🏆").assertIsDisplayed()
        composeTestRule.onNodeWithText("Errands").assertIsDisplayed()
        composeTestRule.onNodeWithText("+7").assertIsDisplayed()
        composeTestRule.onNodeWithText("Original Name").assertDoesNotExist()
    }

    @Test
    fun editExistingTask_rewardLinksPrepopulateFromExistingLinks() {
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription("New Reward").performClick()
        composeTestRule.onNodeWithText("Reward name").performTextInput("Study Time")
        composeTestRule.onNodeWithText("Point cost").performTextClearance()
        composeTestRule.onNodeWithText("Point cost").performTextInput("5")
        composeTestRule.onNodeWithText("SAVE").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithText(Strings.REWARD_DETAIL_NO_TASKS)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Create a task linked to that reward as mandatory.
        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithContentDescription("New Task").performClick()
        composeTestRule.onNodeWithText(Strings.TASK_NAME_LABEL).performTextInput("Read Book")
        composeTestRule.onNodeWithText("Study Time").performClick()
        composeTestRule.onNodeWithContentDescription(Strings.TASK_OPTIONAL_DESC).performClick()
        composeTestRule.onNodeWithText("SAVE").performClick()
        waitForTaskDetail()

        // Reopening the task must pre-populate the reward link as included + mandatory.
        composeTestRule.onNodeWithContentDescription(Strings.EDIT_TASK_DESC).performClick()
        composeTestRule.onNodeWithText(Strings.TASK_USE_TO_GET).assertIsDisplayed()
        composeTestRule.onNodeWithText("Study Time").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(Strings.TASK_MANDATORY_DESC).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(Strings.TASK_MANDATORY_DESC).assertIsEnabled()
    }

    @Test
    fun addTaskFromExistingRewardDetail_showsUsedInAndLinksOnSave() {
        // Create and save a reward first so its Reward Detail screen is reachable with a real id.
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription("New Reward").performClick()
        composeTestRule.onNodeWithText("Reward name").performTextInput("Piano Practice")
        composeTestRule.onNodeWithText("Point cost").performTextClearance()
        composeTestRule.onNodeWithText("Point cost").performTextInput("5")
        composeTestRule.onNodeWithText("SAVE").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithText(Strings.REWARD_DETAIL_NO_TASKS)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // From the existing reward's own Detail screen — not a new, unsaved reward form.
        composeTestRule.onNodeWithText(Strings.REWARD_ADD_TASK_BTN).performClick()
        composeTestRule.onNodeWithText(Strings.ADD_TASK_CREATE).performClick()

        // This entry point shows the "used in" line, not the reward-link checkbox list.
        composeTestRule.onNodeWithText(Strings.taskUsedIn("Piano Practice")).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings.TASK_USE_TO_GET).assertDoesNotExist()

        composeTestRule.onNodeWithText(Strings.TASK_NAME_LABEL).performTextInput("Practice Scales")
        composeTestRule.onNodeWithText("SAVE").performClick()

        // Must pop back to Reward Detail (not forward to TaskDetailScreen) with the task linked.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Practice Scales").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Piano Practice").assertIsDisplayed()
    }
}
