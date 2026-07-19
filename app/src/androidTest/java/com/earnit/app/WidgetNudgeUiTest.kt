package com.earnit.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earnit.app.data.SettingsRepository
import com.earnit.app.tags.Nudge
import com.earnit.app.tags.Reward
import com.earnit.app.tags.UiTest
import com.earnit.app.tags.Widget
import com.earnit.app.ui.Strings
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Verifies the widget nudge banner on RewardDetailScreen: hidden while a reward has no
 * tasks, shown once the first task is linked, and permanently dismissible.
 */
@UiTest
@Widget
@Nudge
@Reward
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WidgetNudgeUiTest {
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
    fun nudge_hiddenUntilFirstTaskLinked_thenDismissiblePersistently() {
        // Create a reward with no tasks — nudge must not appear yet.
        composeTestRule.createReward("Movie Night", cost = "5")
        composeTestRule.waitForRewardDetail()
        assertTrue(
            "Widget nudge must not show before any task is linked",
            composeTestRule.onAllNodesWithText(Strings.WIDGET_NUDGE_BODY).fetchSemanticsNodes().isEmpty(),
        )

        // Create a task and link it to the reward.
        composeTestRule.onNodeWithText("Add task").performClick()
        composeTestRule.onNodeWithText("Create your own").performClick()
        composeTestRule.onNodeWithText("Task name").performTextInput("Push-ups")
        composeTestRule.onNodeWithText("SAVE").performClick()

        // Nudge appears now that the reward has its first task.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(Strings.WIDGET_NUDGE_BODY).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(Strings.WIDGET_NUDGE_BODY).assertIsDisplayed()

        // Dismiss it.
        composeTestRule.onNodeWithContentDescription(Strings.WIDGET_NUDGE_DISMISS_DESC).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(Strings.WIDGET_NUDGE_BODY).fetchSemanticsNodes().isEmpty()
        }

        // Dismissal persists across process recreation.
        composeTestRule.activityRule.scenario.recreate()
        val saved = runBlocking { settingsRepository.settings.first() }
        assertEquals(true, saved.widgetNudgeDismissed)
    }
}
