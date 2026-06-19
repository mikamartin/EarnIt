package com.earnit.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earnit.app.data.SettingsRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * End-to-end UI test using a real in-memory Room database (no mocks).
 * Exercises the full happy path through the live Compose UI:
 *   create task → create reward → link task → log from Prizes home card → open detail → claim → verify History
 *
 * Intermediate assertIsDisplayed() calls act as sync barriers (each triggers waitForIdle()) and
 * as diagnostic checkpoints — if a step fails silently upstream, the first failing assertion
 * identifies exactly where the chain broke rather than timing out at the CLAIM step.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class UiHappyPathTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var settingsRepository: SettingsRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking { settingsRepository.updateNotesMandatory(false) }
    }

    @Test
    fun createTask_createReward_linkTask_log_claim_appearsInHistory() {
        // ── Step 1: Create a task ──────────────────────────────────────────────
        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithContentDescription("New Task").performClick()

        composeTestRule.onNodeWithText("Task name").performClick()
        composeTestRule.onNodeWithText("Task name").performTextInput("Morning Run")

        composeTestRule.onNodeWithText("SAVE").performClick()

        // ── Step 2: Create a reward ────────────────────────────────────────────
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription("New Reward").performClick()

        composeTestRule.onNodeWithText("Reward name").performClick()
        composeTestRule.onNodeWithText("Reward name").performTextInput("Coffee Treat")

        composeTestRule.onNodeWithText("Point cost").performClick()
        composeTestRule.onNodeWithText("Point cost").performTextInput("4")

        composeTestRule.onNodeWithText("SAVE").performClick()

        // ── Step 3: Open reward detail and link the task ───────────────────────
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithText("Coffee Treat").performClick()

        composeTestRule.onNodeWithText("Add task").performClick()

        // AddTaskToRewardDialog: check the task checkbox
        composeTestRule.onNodeWithText("Morning Run").performClick()

        composeTestRule.onNodeWithText("ADD SELECTED").performClick()

        // Checkpoint 1: task link persisted — Morning Run must be visible in the task list.
        composeTestRule.onNodeWithText("Morning Run").assertIsDisplayed()

        // ── Step 4: Log from the Prizes home-screen card ───────────────────────
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()

        // Checkpoint 2: + LOG must be present (allTasks non-empty; if still empty the card
        // shows "Add tasks" instead, meaning ADD SELECTED's Room write hasn't propagated).
        composeTestRule.onNodeWithText("+ LOG").assertIsDisplayed()
        composeTestRule.onNodeWithText("+ LOG").performClick()

        // LogTaskDialog: two nodes match "Morning Run" (dialog row + card background);
        // filter to the clickable dialog row.
        composeTestRule.onAllNodesWithText("Morning Run").filterToOne(hasClickAction()).performClick()
        composeTestRule.onNodeWithText("LOG").performClick()

        // ── Step 5: Open reward detail ────────────────────────────────────────
        composeTestRule.onNodeWithText("Coffee Treat").performClick()

        // Checkpoint 3: Morning Run must appear in Recent activity.
        // assertIsDisplayed() calls waitForIdle() — if Morning Run is visible here but CLAIM
        // is not, the log was not created (not a timing issue; something broke upstream).
        composeTestRule.onNodeWithText("Morning Run").assertIsDisplayed()

        // ── Step 6: Claim the reward ───────────────────────────────────────────
        composeTestRule.onNodeWithText("CLAIM").performClick()

        // ClaimDialog: archive without starting over
        composeTestRule.onNodeWithText("Archive Only").performClick()

        // ── Step 7: Verify History ─────────────────────────────────────────────
        composeTestRule.onNodeWithContentDescription("History").performClick()
        composeTestRule.onNodeWithText("Claimed Rewards").performClick()

        composeTestRule.onNodeWithText("Coffee Treat").assertIsDisplayed()
    }
}
