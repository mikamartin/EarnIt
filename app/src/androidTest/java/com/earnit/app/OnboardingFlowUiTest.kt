package com.earnit.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earnit.app.tags.Reward
import com.earnit.app.tags.UiTest
import com.earnit.app.ui.Strings
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end coverage for the first-launch onboarding tutorial ("Pugsly's Quest Scroll"): it
 * triggers on first launch, each spotlight step's real field is reachable through the cutout,
 * the starter chip fills the real reward-name field, completing the flow persists the seen
 * flag and leaves a real reward + task behind, and Settings → Replay tutorial re-triggers it.
 */
@UiTest
@Reward
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OnboardingFlowUiTest {
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
    fun firstLaunch_completingTutorial_setsFlagAndLeavesRealRewardOnHome() {
        resetOnboardingFlag()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(Strings.ONBOARDING_INTRO_LINE_1).fetchSemanticsNodes().isNotEmpty()
        }

        // Permission beat — tap through all three lines via the real Continue button (the
        // bubble text itself isn't tappable, only the Continue affordance advances it).
        composeTestRule.onNodeWithText(Strings.ONBOARDING_INTRO_LINE_1).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings.ONBOARDING_CONTINUE).performClick()
        composeTestRule.onNodeWithText(Strings.ONBOARDING_INTRO_LINE_2).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings.ONBOARDING_CONTINUE).performClick()
        composeTestRule.onNodeWithText(Strings.ONBOARDING_INTRO_LINE_3).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings.ONBOARDING_CONTINUE).performClick()

        // Name spotlight — starter chip fills the real reward-name field.
        composeTestRule.onNodeWithText(Strings.ONBOARDING_NAME_LINE).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings.ONBOARDING_STARTER_CHIP_2).performClick()
        composeTestRule.onNodeWithText(Strings.ONBOARDING_CONTINUE).performClick()

        // Cost spotlight — type directly into the real cost field through the cutout.
        composeTestRule.onNodeWithText(Strings.ONBOARDING_COST_LINE).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings.REWARD_COST_LABEL).performTextClearance()
        composeTestRule.onNodeWithText(Strings.REWARD_COST_LABEL).performTextInput("40")
        composeTestRule.onNodeWithText(Strings.ONBOARDING_CONTINUE).performClick()

        // Task spotlight — create a real task through the real Add Task flow.
        composeTestRule.onNodeWithText(Strings.ONBOARDING_TASK_LINE).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings.REWARD_ADD_TASK_BTN).performClick()
        composeTestRule.onNodeWithText(Strings.ADD_TASK_CREATE).performClick()

        // Task-creation detour — its own two-beat coaching bubble (name, then points).
        composeTestRule.onNodeWithText(Strings.ONBOARDING_TASK_NAME_LINE).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings.TASK_NAME_LABEL).performTextInput("Clean the garage")
        composeTestRule.onNodeWithText(Strings.ONBOARDING_CONTINUE).performClick()
        composeTestRule.onNodeWithText(Strings.ONBOARDING_TASK_POINTS_LINE).assertIsDisplayed()
        composeTestRule.onNodeWithText("SAVE").performClick()
        composeTestRule.onNodeWithText("Clean the garage").assertIsDisplayed()

        // Back on the reward screen, the coaching line switches to the required/repeatable icons.
        composeTestRule.onNodeWithText(Strings.ONBOARDING_TASK_LINKED_LINE).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings.ONBOARDING_CONTINUE).performClick()

        // Awaiting save — spotlight hands off to the real Save button, no overlay Continue.
        composeTestRule.onNodeWithText(Strings.ONBOARDING_READY_TO_SAVE).assertIsDisplayed()
        composeTestRule.onNodeWithText("SAVE").performClick()

        // Outro — tap through, lands on Reward Detail with the real reward and task.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(Strings.ONBOARDING_OUTRO_LINE).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(Strings.ONBOARDING_OUTRO_LINE).performClick()

        // Not waitForRewardDetail() — that waits for the empty-tasks state, but this reward
        // already has "Clean the garage" linked by the time onboarding hands off.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Clean the garage").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("The trip you keep postponing").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clean the garage").assertIsDisplayed()

        // Onboarding is marked seen — a second reward creation shouldn't show the tutorial again.
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription(Strings.NEW_REWARD_DESC).performClick()
        composeTestRule.onNodeWithText(Strings.REWARD_NAME_LABEL).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings.ONBOARDING_INTRO_LINE_1).assertDoesNotExist()
    }

    @Test
    fun replayTutorial_fromSettings_retriggersFlow() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText(Strings.SETTINGS_REPLAY_TUTORIAL_LABEL).performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(Strings.ONBOARDING_INTRO_LINE_1).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(Strings.ONBOARDING_INTRO_LINE_1).assertIsDisplayed()
    }

    @Test
    fun replayTutorial_duplicateRewardName_blocksSaveWithExplanation() {
        // Seed a reward whose name matches a starter chip, so picking that chip during a replay
        // collides with it — reproduces a real scenario: replaying the tutorial and reusing the
        // same suggestion a prior run already saved.
        composeTestRule.createReward(Strings.ONBOARDING_STARTER_CHIP_1, cost = "5")
        composeTestRule.waitForRewardDetail()

        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText(Strings.SETTINGS_REPLAY_TUTORIAL_LABEL).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(Strings.ONBOARDING_INTRO_LINE_1).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(Strings.ONBOARDING_CONTINUE).performClick()
        composeTestRule.onNodeWithText(Strings.ONBOARDING_CONTINUE).performClick()
        composeTestRule.onNodeWithText(Strings.ONBOARDING_CONTINUE).performClick()

        composeTestRule.onNodeWithText(Strings.ONBOARDING_STARTER_CHIP_1).performClick()
        composeTestRule.onNodeWithText(Strings.ONBOARDING_CONTINUE).performClick()
        composeTestRule.onNodeWithText(Strings.REWARD_COST_LABEL).performTextClearance()
        composeTestRule.onNodeWithText(Strings.REWARD_COST_LABEL).performTextInput("40")
        composeTestRule.onNodeWithText(Strings.ONBOARDING_CONTINUE).performClick()

        composeTestRule.onNodeWithText(Strings.REWARD_ADD_TASK_BTN).performClick()
        composeTestRule.onNodeWithText(Strings.ADD_TASK_CREATE).performClick()
        composeTestRule.onNodeWithText(Strings.TASK_NAME_LABEL).performTextInput("Read a chapter")
        composeTestRule.onNodeWithText(Strings.ONBOARDING_CONTINUE).performClick()
        composeTestRule.onNodeWithText("SAVE").performClick()
        composeTestRule.onNodeWithText("Read a chapter").assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings.ONBOARDING_CONTINUE).performClick()

        // Awaiting save — the duplicate name blocks it, and the bubble explains why instead of
        // confidently pointing at a button that won't do anything.
        composeTestRule.onNodeWithText(Strings.ONBOARDING_SAVE_BLOCKED_LINE).assertIsDisplayed()
        composeTestRule.onNodeWithText("SAVE").assertIsNotEnabled()
    }
}
