package com.earnit.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earnit.app.data.AppColorScheme
import com.earnit.app.data.EarnItRepository
import com.earnit.app.data.MascotId
import com.earnit.app.data.RewardEntity
import com.earnit.app.data.SettingsRepository
import com.earnit.app.data.TaskEntity
import com.earnit.app.tags.CleanUp
import com.earnit.app.tags.UiTest
import com.earnit.app.ui.Strings
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * CleanUpScreen's four destructive-action confirmation dialogs had no automated coverage at any
 * level before this — each test seeds a task, reward, and log, cancels the dialog, and confirms
 * via the repository directly that nothing was actually cleared.
 */
@UiTest
@CleanUp
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CleanUpScreenUiTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var repository: EarnItRepository

    @Inject lateinit var settingsRepository: SettingsRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        resetAppState()
        runBlocking {
            val taskId = repository.upsertTask(TaskEntity(name = "Keep Task", points = 5))
            val rewardId = repository.upsertReward(RewardEntity(name = "Keep Reward", cost = 5))
            repository.saveRewardTasks(rewardId, listOf(Triple(taskId, false, false)))
            val task = repository.getTaskOrNull(taskId)!!
            repository.logCompletion(task, rewardId = rewardId, detail = "kept")
        }

        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText(Strings.SETTINGS_CLEANUP_ROW).performScrollTo().performClick()
        composeTestRule.onNodeWithText(Strings.CLEANUP_SCREEN_TITLE).assertIsDisplayed()
    }

    private fun assertCancelClearsNothing(
        openButtonText: String,
        dialogTitle: String,
    ) {
        // DangerButton uppercases its label at render time (SettingsScreen.kt) — match
        // case-insensitively rather than hardcoding the transformed text here.
        composeTestRule.onNodeWithText(openButtonText, ignoreCase = true).performScrollTo().performClick()
        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()

        composeTestRule.cancelDialogAndAssertDismissed(dialogTitle)

        val state = runBlocking { repository.observeUiState().first() }
        assertEquals(1, state.tasks.size)
        assertEquals(1, state.rewardProgressList.size)
        assertEquals(1, state.allLogs.size)
    }

    @Test
    fun clearLogsDialog_cancel_clearsNothing() {
        assertCancelClearsNothing(Strings.CLEANUP_BTN_LOGS, Strings.CLEANUP_DIALOG_LOGS_TITLE)
    }

    @Test
    fun clearTasksDialog_cancel_clearsNothing() {
        assertCancelClearsNothing(Strings.CLEANUP_BTN_TASKS, Strings.CLEANUP_DIALOG_TASKS_TITLE)
    }

    @Test
    fun clearRewardsDialog_cancel_clearsNothing() {
        assertCancelClearsNothing(Strings.CLEANUP_BTN_REWARDS, Strings.CLEANUP_DIALOG_REWARDS_TITLE)
    }

    @Test
    fun clearAllDialog_cancel_clearsNothing() {
        assertCancelClearsNothing(Strings.CLEANUP_BTN_ALL, Strings.CLEANUP_DIALOG_ALL_TITLE)
    }

    @Test
    fun clearAllDialog_confirm_wipesDataAndResetsSettingsExceptBackupChoice() {
        runBlocking {
            settingsRepository.updateColorScheme(AppColorScheme.OCEAN_BLUE)
            settingsRepository.updateNickname("Custom")
            settingsRepository.enableDevMode()
            settingsRepository.updateUnlockedMascots(setOf(MascotId.PUGSLY, MascotId.TABBY, MascotId.MASCOT_3))
            settingsRepository.updateCloudBackupEnabled(false)
        }

        composeTestRule.onNodeWithText(Strings.CLEANUP_BTN_ALL, ignoreCase = true).performScrollTo().performClick()
        composeTestRule.onNodeWithText(Strings.CLEANUP_DIALOG_ALL_TITLE).assertIsDisplayed()
        // The card's open button and the dialog's confirm button share the same rendered text
        // ("WIPE EVERYTHING"); the dialog's popup is added after the underlying screen, so its
        // button is the last matching node.
        val allWipeButtons = composeTestRule.onAllNodesWithText(Strings.CLEANUP_BTN_ALL, ignoreCase = true)
        allWipeButtons[allWipeButtons.fetchSemanticsNodes().size - 1].performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(Strings.CLEANUP_SNACKBAR_ALL).fetchSemanticsNodes().isNotEmpty()
        }

        val state = runBlocking { repository.observeUiState().first() }
        assertTrue(state.tasks.isEmpty())
        assertTrue(state.rewardProgressList.isEmpty())
        assertTrue(state.allLogs.isEmpty())

        val settings = runBlocking { settingsRepository.settings.first() }
        assertEquals(AppColorScheme.WARM_GOLD, settings.colorScheme)
        assertEquals("Babe", settings.nickname)
        assertFalse(settings.devModeEnabled)
        assertEquals(setOf(MascotId.PUGSLY, MascotId.TABBY), settings.unlockedMascotIds)
        // The cloud backup opt-out is a privacy choice, not app data — it must survive the wipe.
        assertFalse(settings.cloudBackupEnabled)
    }
}
