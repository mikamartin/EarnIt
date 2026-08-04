package com.earnit.app

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earnit.app.data.SettingsRepository
import com.earnit.app.tags.Reward
import com.earnit.app.tags.Task
import com.earnit.app.tags.UiTest
import com.earnit.app.ui.Strings
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * The "Complete to earn points" task row shows a checkmark reflecting completion, plus a static
 * star (mandatory) and/or repeat (repeatable) flag icon. The star and repeat icons must not
 * change with completion status — that ambiguity (a mandatory task's star dimming to look
 * "not mandatory" when it was really just "not logged yet") is the bug this row layout fixes.
 * RewardProgress.isTaskLogged's boundary cases are unit-tested directly in RewardProgressTest.
 */
@UiTest
@Reward
@Task
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RewardDetailTaskRowUiTest {
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
    fun taskRow_keepsMandatoryAndRepeatableFlagsVisible_whileCheckmarkTracksCompletion() {
        composeTestRule.createTask("Morning Run")

        composeTestRule.createReward("Study Time")
        composeTestRule.waitForRewardDetail()
        composeTestRule.onNodeWithText(Strings.REWARD_ADD_TASK_BTN).performClick()
        composeTestRule.onNodeWithText("Morning Run").performClick()
        // Tasks default to isMandatory = false, isRepeatable = true when linked — flip mandatory on
        // so this task carries both flags.
        composeTestRule.onNodeWithContentDescription(Strings.REWARD_OPTIONAL_DESC).performClick()
        composeTestRule.onNodeWithText("ADD SELECTED").performClick()
        composeTestRule.onNodeWithText("Morning Run").assertIsDisplayed()

        // Before logging: checkmark reads "not logged", both flag icons are present.
        composeTestRule.onNodeWithContentDescription(Strings.REWARD_TASK_NOT_DONE_DESC).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(Strings.REWARD_MANDATORY_DESC).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(Strings.REWARD_REPEATABLE_DESC).assertIsDisplayed()

        // Log the task.
        composeTestRule.onNodeWithText(Strings.LOG_BTN).performClick()
        composeTestRule.onAllNodesWithText("Morning Run").filterToOne(hasClickAction()).performClick()
        composeTestRule.onNodeWithText(Strings.DIALOG_LOG_BTN).performClick()

        // After logging: checkmark flips to "logged"; the flag icons are unchanged — still
        // present, not dimmed or swapped for an outline variant.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithContentDescription(Strings.REWARD_TASK_DONE_DESC)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription(Strings.REWARD_TASK_DONE_DESC).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(Strings.REWARD_MANDATORY_DESC).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(Strings.REWARD_REPEATABLE_DESC).assertIsDisplayed()
    }

    @Test
    fun taskRow_hidesFlagIcons_whenTaskIsNeitherMandatoryNorRepeatable() {
        composeTestRule.createTask("Cold Shower")

        composeTestRule.createReward("Study Time")
        composeTestRule.waitForRewardDetail()
        composeTestRule.onNodeWithText(Strings.REWARD_ADD_TASK_BTN).performClick()
        composeTestRule.onNodeWithText("Cold Shower").performClick()
        // Flip off the default isRepeatable = true, leaving isMandatory at its false default.
        composeTestRule.onNodeWithContentDescription(Strings.REWARD_REPEATABLE_DESC).performClick()
        composeTestRule.onNodeWithText("ADD SELECTED").performClick()
        composeTestRule.onNodeWithText("Cold Shower").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(Strings.REWARD_TASK_NOT_DONE_DESC).assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription(Strings.REWARD_MANDATORY_DESC).assertCountEquals(0)
        composeTestRule.onAllNodesWithContentDescription(Strings.REWARD_REPEATABLE_DESC).assertCountEquals(0)
    }
}
