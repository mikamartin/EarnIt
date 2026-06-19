package com.earnit.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earnit.app.ui.Strings
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the empty-state copy shown across all three tabs on a fresh
 * install (no tasks, no rewards, no history) — covered manually until now.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class EmptyStateUiTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun freshInstall_showsEmptyStateOnAllTabs() {
        // ── Prizes tab (default landing screen) ────────────────────────────────
        composeTestRule.onNodeWithText(Strings.HOME_EMPTY_REWARDS).assertIsDisplayed()

        // ── Tasks tab ────────────────────────────────────────────────────────
        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithText(Strings.TASKS_EMPTY_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings.TASKS_EMPTY_BODY).assertIsDisplayed()

        // ── History tab — Completed Tasks (default sub-tab) ────────────────────
        composeTestRule.onNodeWithContentDescription("History").performClick()
        composeTestRule.onNodeWithText(Strings.HISTORY_NO_TASKS).assertIsDisplayed()

        // ── History tab — Claimed Rewards ───────────────────────────────────────
        composeTestRule.onNodeWithText(Strings.HISTORY_TAB_REWARDS).performClick()
        composeTestRule.onNodeWithText(Strings.HISTORY_NO_REWARDS).assertIsDisplayed()
    }
}
