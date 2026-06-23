package com.earnit.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
 * Verifies that the duplicate-name error is shown and the SAVE button is disabled
 * when a task or reward name conflicts with an existing one (case-insensitive).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DuplicateNameUiTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun duplicateTaskName_showsErrorAndDisablesSave() {
        // ── Create a task named "Morning Run" ──────────────────────────────────
        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithContentDescription("New Task").performClick()
        composeTestRule.onNodeWithText("Task name").performTextInput("Morning Run")
        composeTestRule.onNodeWithText("SAVE").performClick()

        // Wait for navigation away from the edit screen
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Points:").fetchSemanticsNodes().isNotEmpty()
        }

        // ── Open a second new-task form ────────────────────────────────────────
        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithContentDescription("New Task").performClick()

        // Enter a name that conflicts (lowercase — case-insensitive check)
        composeTestRule.onNodeWithText("Task name").performTextInput("morning run")

        // Error message must appear
        composeTestRule
            .onNodeWithText(Strings.taskDuplicateError("morning run"))
            .assertIsDisplayed()

        // SAVE must be disabled
        composeTestRule.onNodeWithText("SAVE").assertIsNotEnabled()
    }

    @Test
    fun duplicateRewardName_showsErrorAndDisablesSave() {
        // ── Create a reward named "Movie Night" ────────────────────────────────
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription("New Reward").performClick()
        composeTestRule.onNodeWithText("Reward name").performTextInput("Movie Night")
        composeTestRule.onNodeWithText("Point cost").performTextClearance()
        composeTestRule.onNodeWithText("Point cost").performTextInput("5")
        composeTestRule.onNodeWithText("SAVE").performClick()

        // Wait for navigation away from the edit screen
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithText(Strings.REWARD_DETAIL_NO_TASKS)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // ── Open a second new-reward form ──────────────────────────────────────
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription("New Reward").performClick()

        // Enter the same name
        composeTestRule.onNodeWithText("Reward name").performTextInput("Movie Night")

        // Error message must appear
        composeTestRule
            .onNodeWithText(Strings.rewardDuplicateError("Movie Night"))
            .assertIsDisplayed()

        // SAVE must be disabled
        composeTestRule.onNodeWithText("SAVE").assertIsNotEnabled()
    }
}
