package com.earnit.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * UI regression tests for post-save and shortcut navigation around the reward/task forms.
 *
 * Before the fix, clicking SAVE left the user on the edit screen with a false
 * "already exists" duplicate warning. These tests verify:
 *   1. Saving a new task navigates to TaskDetailScreen.
 *   2. Saving a new reward navigates to RewardDetailScreen.
 *   3. Creating a task from the new-reward form pops back to the reward form
 *      (not to TaskDetailScreen), auto-includes the task, and saves both linked
 *      when the reward is subsequently saved.
 *   4. The home card's "+ ADD TASKS" shortcut (reward with no tasks) opens the
 *      Add Task dialog directly on Reward Detail, not the Reward Edit screen.
 */
@UiTest
@Task
@Reward
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SaveNavigationUiTest {
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

    @Test
    fun saveNewTask_navigatesToTaskDetail() {
        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithContentDescription("New Task").performClick()

        composeTestRule.onNodeWithText("Task name").performTextInput("Morning Jog")
        composeTestRule.onNodeWithText("SAVE").performClick()

        // TaskDetailScreen is uniquely identified by "Points:" — wait for it.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Points:").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Morning Jog").assertIsDisplayed()
    }

    @Test
    fun saveNewReward_navigatesToRewardDetail() {
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription("New Reward").performClick()

        composeTestRule.onNodeWithText("Reward name").performTextInput("Game Night")
        composeTestRule.onNodeWithText("SAVE").performClick()

        // RewardDetailScreen with no tasks shows "No tasks added yet." — wait for it.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithText(Strings.REWARD_DETAIL_NO_TASKS)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("Game Night").assertIsDisplayed()
    }

    @Test
    fun addTaskButton_disabledUntilRewardNameEntered() {
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription("New Reward").performClick()

        composeTestRule.onNodeWithText("Add task").assertIsNotEnabled()

        composeTestRule.onNodeWithText("Reward name").performTextInput("Game Night")
        composeTestRule.onNodeWithText("Add task").assertIsEnabled()
    }

    @Test
    fun createTaskFromNewRewardEdit_popsBackAndLinksTaskOnSave() {
        // Open a new reward form.
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription("New Reward").performClick()
        composeTestRule.onNodeWithText("Reward name").performTextInput("Pizza Night")

        // Open the task-picker dialog and choose "Create your own".
        composeTestRule.onNodeWithText("Add task").performClick()
        composeTestRule.onNodeWithText("Create your own").performClick()

        // Now on TaskEditScreen — verify the in-progress reward name is shown.
        composeTestRule.onNodeWithText("Will be added to: Pizza Night").assertIsDisplayed()

        // Create the task and save.
        composeTestRule.onNodeWithText("Task name").performTextInput("Order Pizza")
        composeTestRule.onNodeWithText("SAVE").performClick()

        // Must pop back to the reward edit form, NOT navigate to TaskDetailScreen.
        // The task is auto-included via pendingTaskId; wait for it to appear in the form.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Order Pizza").fetchSemanticsNodes().isNotEmpty()
        }
        // Confirm we're still on the reward edit form, not task detail.
        composeTestRule.onNodeWithText("Add reward").assertIsDisplayed()

        // Save the reward — both entities must end up in the DB and linked.
        composeTestRule.onNodeWithText("SAVE").performClick()

        // SAVE disappears once we leave the edit screen.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("SAVE").fetchSemanticsNodes().isEmpty()
        }
        // Verify we reached RewardDetailScreen with both items present and linked.
        composeTestRule.onNodeWithText("Pizza Night").assertIsDisplayed()
        composeTestRule.onNodeWithText("Order Pizza").assertIsDisplayed()
    }

    @Test
    fun homeCardAddTasksButton_opensAddTaskDialogDirectly() {
        // Create a reward with no tasks yet.
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription("New Reward").performClick()
        composeTestRule.onNodeWithText("Reward name").performTextInput("Movie Night")
        composeTestRule.onNodeWithText("SAVE").performClick()

        // Wait for RewardDetailScreen, then navigate back to Home.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(Strings.REWARD_DETAIL_NO_TASKS).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()

        // Tap the home card's "+ ADD TASKS" pill.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(Strings.HOME_ADD_TASKS_BTN).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(Strings.HOME_ADD_TASKS_BTN).performClick()

        // The Add Task dialog must appear on this single tap — no Reward Edit screen in between.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(Strings.ADD_TASK_CREATE).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(Strings.ADD_TASK_CREATE).assertIsDisplayed()

        // We must never have landed on the Reward Edit screen.
        assertTrue(
            "Should open the Add Task dialog directly, not the reward edit screen",
            composeTestRule
                .onAllNodesWithText(Strings.REWARD_EDIT_NEW)
                .fetchSemanticsNodes()
                .isEmpty(),
        )
    }
}
