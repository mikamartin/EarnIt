package com.earnit.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earnit.app.data.MascotId
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
 * Covers EARNIT_SPEC.md §6 App Settings behaviours that had no automated coverage before the
 * SettingsScreen split: nickname/greeting, random nickname override, show-quote toggle, the
 * default unlocked-mascot set, mascot selection persistence, max reward count's default value
 * and enforcement through the Settings slider itself (not just the repository), and the
 * About/Data/Clean Up nav rows. Colour scheme persistence and Notes Required are already
 * covered by SettingsUiTest.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingsScreenUiTest {
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
    fun nickname_editedInSettings_displaysInHomeGreeting() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Babe").performScrollTo().performTextReplacement("Zorro")
        composeTestRule.onNodeWithText("Zorro").performImeAction()

        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithText("Earn It, Zorro!").assertIsDisplayed()
    }

    @Test
    fun nickname_clearedField_greetingShowsNoAddress() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Babe").performScrollTo().performTextClearance()
        composeTestRule.onNodeWithText(Strings.SETTINGS_NAME_PLACEHOLDER).performImeAction()

        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithText("Earn It!").assertIsDisplayed()
    }

    @Test
    fun useRandomNickname_enabled_overridesTypedNicknameOnHomeGreeting() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Babe").performScrollTo().performTextReplacement("Zorro")
        composeTestRule.onNodeWithText("Zorro").performImeAction()

        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithText("Earn It, Zorro!").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithContentDescription("Random name").performScrollTo().performClick()

        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Earn It, Zorro!").fetchSemanticsNodes().isEmpty()
        }
        assertEquals(
            0,
            composeTestRule.onAllNodesWithText("Earn It, Zorro!").fetchSemanticsNodes().size,
        )
    }

    @Test
    fun showQuote_toggleOff_hidesQuoteSection_toggleOn_showsItAgain() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithContentDescription(Strings.SETTINGS_QUOTE_TOGGLE).performScrollTo().performClick()

        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        assertEquals(
            0,
            composeTestRule.onAllNodesWithText(Strings.HOME_QUOTE_SECTION).fetchSemanticsNodes().size,
        )

        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithContentDescription(Strings.SETTINGS_QUOTE_TOGGLE).performScrollTo().performClick()

        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithText(Strings.HOME_QUOTE_SECTION).assertIsDisplayed()
    }

    @Test
    fun maxRewardCount_freshInstall_defaultsToFive() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule
            .onNodeWithText("${Strings.SETTINGS_MAX_LABEL}: 5")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun maxRewardCount_editedInSettingsSlider_enforcesCapOnHomeFab() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule
            .onNodeWithContentDescription("${Strings.SETTINGS_MAX_LABEL} 1")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription("New Reward").performClick()
        composeTestRule.onNodeWithText("Reward name").performTextInput("Solo Reward")
        composeTestRule.onNodeWithText("Point cost").performTextClearance()
        composeTestRule.onNodeWithText("Point cost").performTextInput("5")
        composeTestRule.onNodeWithText("SAVE").performClick()

        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription("New Reward").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithText(Strings.MAX_REWARD_TOOLTIP)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText(Strings.MAX_REWARD_TOOLTIP).assertIsDisplayed()
    }

    @Test
    fun mascotPicker_defaultUnlockedSet_onlyPugslyAndTabbySelectable() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText(Strings.MASCOT_SECTION_TITLE).performScrollTo().performClick()

        // The row behind the dialog also shows the current mascot's name ("Pugsly"), so match
        // within all nodes rather than onNodeWithText, which requires a single unique match.
        assertEquals(true, composeTestRule.onAllNodesWithText("Pugsly").fetchSemanticsNodes().isNotEmpty())
        assertEquals(true, composeTestRule.onAllNodesWithText("Tabby").fetchSemanticsNodes().isNotEmpty())
        // Panda is the first locked mascot, so its unlock hint is shown; further-locked mascots
        // (e.g. Penguin) show neither a name nor a hint per the "only the next slot" reveal rule.
        composeTestRule.onNodeWithText("Claim your 1st reward").assertIsDisplayed()
        assertEquals(
            0,
            composeTestRule.onAllNodesWithText("Penguin").fetchSemanticsNodes().size,
        )
    }

    @Test
    fun selectedMascot_choiceOfUnlockedMascot_persistsAfterRecreate() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText(Strings.MASCOT_SECTION_TITLE).performScrollTo().performClick()
        composeTestRule.onNodeWithText("Tabby").performClick()

        composeTestRule.activityRule.scenario.recreate()

        val saved = runBlocking { settingsRepository.settings.first() }
        assertEquals(MascotId.TABBY, saved.selectedMascotId)
    }

    @Test
    fun aboutRow_navigatesToAboutScreen() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText(Strings.APP_NAME).performClick()

        composeTestRule.onNodeWithText(Strings.ABOUT_RATE_LABEL).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun dataRow_navigatesToDataScreen() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText(Strings.SETTINGS_DATA_TITLE).performScrollTo().performClick()

        composeTestRule.onNodeWithText(Strings.SETTINGS_DATA_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings.DATA_EXPORT_TITLE).assertIsDisplayed()
    }

    @Test
    fun cleanUpRow_navigatesToCleanUpScreen() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText(Strings.SETTINGS_CLEANUP_ROW).performScrollTo().performClick()

        composeTestRule.onNodeWithText(Strings.CLEANUP_SCREEN_TITLE).assertIsDisplayed()
    }
}
