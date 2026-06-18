package com.earnit.app

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earnit.app.data.AppColorScheme
import com.earnit.app.data.SettingsRepository
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

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingsUiTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var settingsRepository: SettingsRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            settingsRepository.updateColorScheme(AppColorScheme.WARM_GOLD)
            settingsRepository.updateNotesMandatory(false)
            settingsRepository.disableDevMode()
        }
    }

    @Test
    fun colorScheme_selectionPersistsAfterRecreate() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Ocean Blue").performScrollTo().performClick()

        composeTestRule.activityRule.scenario.recreate()

        val saved = runBlocking { settingsRepository.settings.first() }
        assertEquals(AppColorScheme.OCEAN_BLUE, saved.colorScheme)
    }

    @Test
    fun notesMandatory_logButtonDisabledUntilNoteEntered() {
        // Create task
        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithContentDescription("New Task").performClick()
        composeTestRule.onNodeWithText("Task name").performTextInput("Push-ups")
        composeTestRule.onNodeWithText("SAVE").performClick()

        // Create reward
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription("New Reward").performClick()
        composeTestRule.onNodeWithText("Reward name").performTextInput("Movie Night")
        composeTestRule.onNodeWithText("Point cost").performTextInput("5")
        composeTestRule.onNodeWithText("SAVE").performClick()

        // Link task to reward via Reward Detail
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithText("Movie Night").performClick()
        composeTestRule.onNodeWithText("Add task").performClick()
        composeTestRule.onNodeWithText("Push-ups").performClick()
        composeTestRule.onNodeWithText("ADD SELECTED").performClick()

        // Enable Notes required in Settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithContentDescription("Notes required").performScrollTo().performClick()

        // Back to reward detail and open log dialog
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithText("Movie Night").performClick()
        composeTestRule.onNodeWithText("+ LOG").performClick()
        composeTestRule.onNodeWithText("Push-ups").performClick()

        // LOG must be disabled with no note entered
        composeTestRule.onNodeWithText("LOG").assertIsNotEnabled()

        // Entering a note should enable it
        composeTestRule.onNodeWithText("Note*").performTextInput("Done 10 reps")
        composeTestRule.onNodeWithText("LOG").assertIsEnabled()
    }
}
