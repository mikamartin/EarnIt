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
 * Uses Compose test v2 (StandardTestDispatcher) — eliminates the deprecated UnconfinedTestDispatcher.
 * Room's InvalidationTracker fires on a real background thread, so the post-LOG step uses waitUntil()
 * to poll in real time; each advanceByFrame() (v2) drives Compose recompositions so CLAIM becomes
 * visible as soon as the StateFlow propagates canClaim = true.
 *
 * Note: the Point cost field defaults to "10" in the UI — performTextClearance() before input is
 * required to set 4, not 104. assertIsDisplayed() calls elsewhere are synchronous checkpoints.
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
        resetAppState()
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
        composeTestRule.onNodeWithText("Point cost").performTextClearance()
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

        // ── Step 6: Claim the reward ───────────────────────────────────────────
        // Room's InvalidationTracker fires on a real background thread; waitUntil polls in
        // real time while each advanceByFrame() (v2 behaviour) drives Compose recompositions,
        // so CLAIM becomes visible as soon as canClaim = true propagates through the StateFlow.
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("CLAIM").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("CLAIM").performClick()

        // ClaimDialog: archive without starting over
        composeTestRule.onNodeWithText("Archive Only").performClick()

        // ── Step 7: Verify History ─────────────────────────────────────────────
        composeTestRule.onNodeWithContentDescription("History").performClick()
        composeTestRule.onNodeWithText("Claimed Rewards").performClick()

        composeTestRule.onNodeWithText("Coffee Treat").assertIsDisplayed()
    }
}
