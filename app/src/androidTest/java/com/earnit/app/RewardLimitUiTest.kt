package com.earnit.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earnit.app.data.SettingsRepository
import com.earnit.app.ui.Strings
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Verifies max-reward-count gating: when the active reward list reaches maxRewardCount,
 * tapping the FAB shows the max-limit tooltip instead of navigating to the reward edit screen.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RewardLimitUiTest {
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
            // Set the cap to 1 so a single reward triggers the limit.
            settingsRepository.updateMaxRewardCount(1)
            settingsRepository.updateNotesMandatory(false)
        }
    }

    @Test
    fun atMaxRewardCount_fabShowsTooltipInsteadOfNavigating() {
        // ── Create one reward (reaches the cap of 1) ───────────────────────────
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription("New Reward").performClick()

        composeTestRule.onNodeWithText("Reward name").performTextInput("Solo Reward")
        composeTestRule.onNodeWithText("Point cost").performTextClearance()
        composeTestRule.onNodeWithText("Point cost").performTextInput("5")
        composeTestRule.onNodeWithText("SAVE").performClick()

        // Navigate back to the Prizes home screen
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()

        // ── Tap the FAB while at max ───────────────────────────────────────────
        composeTestRule.onNodeWithContentDescription("New Reward").performClick()

        // The tooltip must appear — MAX_REWARD_TOOLTIP text becomes visible
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithText(Strings.MAX_REWARD_TOOLTIP)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText(Strings.MAX_REWARD_TOOLTIP).assertIsDisplayed()

        // We must NOT have navigated to the reward edit screen
        assertTrue(
            "Should not navigate to reward edit when at max",
            composeTestRule
                .onAllNodesWithText(Strings.REWARD_EDIT_NEW)
                .fetchSemanticsNodes()
                .isEmpty(),
        )
    }
}
