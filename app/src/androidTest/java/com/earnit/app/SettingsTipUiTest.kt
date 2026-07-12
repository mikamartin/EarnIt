package com.earnit.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earnit.app.data.SettingsRepository
import com.earnit.app.ui.Strings
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Verifies the Settings screen discoverability tip: shown on first visit, dismissible,
 * and permanently hidden afterward.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingsTipUiTest {
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
    fun tip_shownOnFirstVisit_thenDismissiblePersistently() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText(Strings.SETTINGS_TIP).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(Strings.SETTINGS_TIP_DISMISS_DESC).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(Strings.SETTINGS_TIP).fetchSemanticsNodes().isEmpty()
        }

        composeTestRule.activityRule.scenario.recreate()
        val saved = runBlocking { settingsRepository.settings.first() }
        assertEquals(true, saved.settingsTipDismissed)

        // Re-entering Settings must not show the tip again.
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        assertEquals(
            0,
            composeTestRule.onAllNodesWithText(Strings.SETTINGS_TIP).fetchSemanticsNodes().size,
        )
    }
}
