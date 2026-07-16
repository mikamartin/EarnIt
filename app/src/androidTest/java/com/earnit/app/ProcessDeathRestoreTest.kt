package com.earnit.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earnit.app.data.EarnItRepository
import com.earnit.app.data.RewardEntity
import com.earnit.app.data.TaskEntity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Approximates a cold start after process death: closes the managed ActivityScenario and launches
 * a brand-new one with no saved-instance-state Bundle, inside a single test method. A real
 * `am force-stop` isn't usable here — this repo runs instrumented tests with no Test Orchestrator
 * configured, so the test and the app under test share one OS process, and force-stopping the
 * package would kill the test itself mid-method. This approximation still resets the
 * ViewModelStore, the nav back stack, and all `rememberSaveable` state (no Bundle to restore from),
 * while the Hilt-singleton-scoped Room database stays alive across the relaunch — the same
 * process-survival guarantee a real disk-backed database gets across a genuine cold start. What
 * this does not exercise: an actual OS-level kill of Application/Hilt-singleton state, since the
 * process itself is never terminated.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ProcessDeathRestoreTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var repository: EarnItRepository

    private var relaunchedScenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        hiltRule.inject()
        resetAppState()
    }

    @After
    fun tearDown() {
        relaunchedScenario?.close()
    }

    @Test
    fun coldRelaunch_persistedDataSurvives_navStateDoesNot() {
        runBlocking {
            val taskId = repository.upsertTask(TaskEntity(name = "Morning Run", points = 5, icon = "🏃"))
            val rewardId = repository.upsertReward(RewardEntity(name = "Coffee Treat", cost = 5))
            repository.saveRewardTasks(rewardId, listOf(Triple(taskId, true, false)))
            val task = repository.getTaskOrNull(taskId)!!
            repository.logCompletion(task, rewardId = rewardId, detail = "Felt great")
        }

        // Navigate away from the default start screen before the "kill", so a restored nav
        // Bundle would be observable if one existed.
        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithText("Morning Run").assertIsDisplayed()

        composeTestRule.activityRule.scenario.close()
        relaunchedScenario = ActivityScenario.launch(MainActivity::class.java)
        composeTestRule.waitForIdle()

        // Nav state did not survive: back on the default start screen (Prizes/Home), not Tasks.
        composeTestRule.onNodeWithText("Coffee Treat").assertIsDisplayed()

        // Persisted data did survive: the reward's linked task and its log are still there.
        composeTestRule.onNodeWithText("Coffee Treat").performClick()
        composeTestRule.onNodeWithText("Morning Run").assertIsDisplayed()
    }
}
