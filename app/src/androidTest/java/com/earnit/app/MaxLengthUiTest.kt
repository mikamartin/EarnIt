package com.earnit.app

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earnit.app.data.SettingsRepository
import com.earnit.app.ui.NICKNAME_MAX_CHARS
import com.earnit.app.ui.REWARD_DESC_MAX_CHARS
import com.earnit.app.ui.REWARD_NAME_MAX_CHARS
import com.earnit.app.ui.Strings
import com.earnit.app.ui.TASK_GROUP_MAX_CHARS
import com.earnit.app.ui.TASK_NAME_MAX_CHARS
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Verifies that name/description fields accept input up to their character cap
 * and silently reject growth past it (fix/widget-hint-overflow's sibling gap —
 * see feat/input-validation-limits).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MaxLengthUiTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var settingsRepository: SettingsRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        resetAppState()
        runBlocking {
            settingsRepository.updateNickname("Babe")
            settingsRepository.updateUseRandomNickname(false)
        }
    }

    @Test
    fun taskNameInput_isCappedAtMaxChars() {
        val atCap = "A".repeat(TASK_NAME_MAX_CHARS)

        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithContentDescription("New Task").performClick()
        composeTestRule.onNodeWithText("Task name").performTextInput(atCap)
        composeTestRule.onNodeWithText(atCap).assertExists()

        composeTestRule.onNodeWithText(atCap).performTextInput("X")
        composeTestRule.onNodeWithText(atCap).assertExists()
        composeTestRule.onNodeWithText(atCap + "X").assertDoesNotExist()
    }

    @Test
    fun rewardNameInput_isCappedAtMaxChars() {
        val atCap = "B".repeat(REWARD_NAME_MAX_CHARS)

        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription("New Reward").performClick()
        composeTestRule.onNodeWithText("Reward name").performTextInput(atCap)
        composeTestRule.onNodeWithText(atCap).assertExists()

        composeTestRule.onNodeWithText(atCap).performTextInput("X")
        composeTestRule.onNodeWithText(atCap).assertExists()
        composeTestRule.onNodeWithText(atCap + "X").assertDoesNotExist()
    }

    @Test
    fun rewardDescriptionInput_isCappedAtMaxChars() {
        val atCap = "C".repeat(REWARD_DESC_MAX_CHARS)

        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription("New Reward").performClick()
        composeTestRule.onNodeWithText("Description (optional)").performTextInput(atCap)
        composeTestRule.onNodeWithText(atCap).assertExists()

        composeTestRule.onNodeWithText(atCap).performTextInput("X")
        composeTestRule.onNodeWithText(atCap).assertExists()
        composeTestRule.onNodeWithText(atCap + "X").assertDoesNotExist()
    }

    @Test
    fun taskGroupNameInput_isCappedAtMaxChars() {
        val atCap = "D".repeat(TASK_GROUP_MAX_CHARS)

        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithContentDescription("New Task").performClick()
        composeTestRule.onNodeWithText(Strings.TASK_GROUP_PLACEHOLDER).performTextInput(atCap)
        composeTestRule.onNodeWithText(atCap).assertExists()

        composeTestRule.onNodeWithText(atCap).performTextInput("X")
        composeTestRule.onNodeWithText(atCap).assertExists()
        composeTestRule.onNodeWithText(atCap + "X").assertDoesNotExist()
    }

    @Test
    fun nicknameInput_isCappedAtMaxChars() {
        val atCap = "E".repeat(NICKNAME_MAX_CHARS)

        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        // Nickname seeded to "Babe" in setUp() — clear it before testing the cap.
        composeTestRule.onNodeWithText("Babe").performTextClearance()
        composeTestRule.onNodeWithText(Strings.SETTINGS_NAME_PLACEHOLDER).performTextInput(atCap)
        composeTestRule.onNodeWithText(atCap).assertExists()

        composeTestRule.onNodeWithText(atCap).performTextInput("X")
        composeTestRule.onNodeWithText(atCap).assertExists()
        composeTestRule.onNodeWithText(atCap + "X").assertDoesNotExist()
    }
}
