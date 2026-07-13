package com.earnit.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI journey: import a task template from the Task Library and confirm the
 * resulting tasks land in the Tasks list.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TaskLibraryImportUiTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        resetAppState()
    }

    @Test
    fun importTemplate_addsTasksToTaskList() {
        // ── Open the Task Library from the Tasks tab ────────────────────────────
        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithText("Library").performClick()

        // ── Expand the "Healthy Living" template and add all 10 tasks ───────────
        composeTestRule.onNodeWithText("Healthy Living").performClick()
        composeTestRule.onNodeWithText("ADD 10 TASKS").performClick()

        // ── Back on the Tasks tab, imported tasks are visible ───────────────────
        composeTestRule.onNodeWithText("Morning Run").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cold Shower").assertIsDisplayed()
    }
}
