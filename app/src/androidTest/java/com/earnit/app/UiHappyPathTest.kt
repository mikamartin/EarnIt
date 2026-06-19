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
 * Logging happens on the Prizes home screen (reward card + LOG button) so that the reward detail
 * is opened AFTER the log write, avoiding a race between the async Room insert and the CLAIM
 * button appearing on the same screen that triggered the log. waitUntil polls until canClaim
 * propagates through the ViewModel StateFlow before the click.
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

        // ── Step 4: Log from the Prizes home-screen card ───────────────────────
        // Navigate back to the Prizes home screen where the reward card exposes + LOG.
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithText("+ LOG").performClick()

        // LogTaskDialog: two nodes match "Morning Run" (dialog row + card background);
        // filter to the clickable dialog row.
        composeTestRule.onAllNodesWithText("Morning Run").filterToOne(hasClickAction()).performClick()
        composeTestRule.onNodeWithText("LOG").performClick()

        // ── Step 5: Open reward detail; wait for canClaim to propagate ─────────
        // Room IO from the log is async — open the detail then poll until CLAIM appears
        // rather than assuming the navigation completes after the write.
        composeTestRule.onNodeWithText("Coffee Treat").performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("CLAIM").fetchSemanticsNodes().isNotEmpty()
        }

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
