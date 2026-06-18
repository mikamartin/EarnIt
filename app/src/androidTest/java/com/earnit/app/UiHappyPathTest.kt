package com.earnit.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end UI test using a real in-memory Room database (no mocks).
 * Exercises the full happy path through the live Compose UI:
 *   create task → create reward → link task → log task → claim reward → verify History
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class UiHappyPathTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

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
        composeTestRule.onNodeWithText("Point cost").performTextInput("5")

        composeTestRule.onNodeWithText("SAVE").performClick()

        // ── Step 3: Navigate back to home and open the reward detail ───────────
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithText("Coffee Treat").performClick()

        // ── Step 4: Link the task from Reward Detail ───────────────────────────
        composeTestRule.onNodeWithText("Add task").performClick()

        // AddTaskToRewardDialog: check the task checkbox
        composeTestRule.onNodeWithText("Morning Run").performClick()

        composeTestRule.onNodeWithText("ADD SELECTED").performClick()

        // ── Step 5: Log the task ───────────────────────────────────────────────
        composeTestRule.onNodeWithText("+ LOG").performClick()

        // LogTaskDialog: select the task and confirm
        composeTestRule.onNodeWithText("Morning Run").performClick()
        composeTestRule.onNodeWithText("LOG").performClick()

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
