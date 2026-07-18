package com.earnit.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earnit.app.tags.Task
import com.earnit.app.tags.UiTest
import com.earnit.app.ui.Strings
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TaskLibraryScreen had no automated coverage at any level before this. Covers the
 * skipped-tasks dialog's only dismiss path — backdrop tap / system back, since it has no
 * explicit Cancel button (only "OK", which does the same navigate-back) — confirming the import
 * that already landed still lands correctly when the dialog is dismissed rather than confirmed.
 */
@UiTest
@Task
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TaskLibraryScreenUiTest {
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
    fun skippedDialog_backPressDismiss_stillNavigatesBackWithImportApplied() {
        // Pre-existing task with the same name as a template task, so the import skips it —
        // the trigger for the skipped-tasks dialog.
        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithContentDescription("New Task").performClick()
        composeTestRule.onNodeWithText(Strings.TASK_NAME_LABEL).performTextInput("Morning Run")
        composeTestRule.onNodeWithText("SAVE").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(Strings.TASK_DETAIL_POINTS_LABEL).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithText("Library").performClick()
        composeTestRule.onNodeWithText(Strings.LIBRARY_TITLE).assertIsDisplayed()

        composeTestRule.onNodeWithText("Healthy Living").performClick()
        // AnimatedVisibility's expand animation isn't driven by this test API's default idling —
        // poll in real time rather than asserting immediately, matching the pattern documented
        // in UiHappyPathTest for the same reason.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("ADD 10 TASKS").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("ADD 10 TASKS").performScrollTo().performClick()

        // importTemplate's dedup check runs as a coroutine against Room — wait for the write to
        // actually land rather than asserting immediately against Compose's idle state alone.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(Strings.librarySkippedTitle(1)).fetchSemanticsNodes().isNotEmpty()
        }

        Espresso.pressBack()

        composeTestRule.onNodeWithText(Strings.librarySkippedTitle(1)).assertDoesNotExist()
        composeTestRule.onNodeWithText(Strings.LIBRARY_TITLE).assertDoesNotExist()
        composeTestRule.onNodeWithText("Cold Shower").assertIsDisplayed()
    }
}
