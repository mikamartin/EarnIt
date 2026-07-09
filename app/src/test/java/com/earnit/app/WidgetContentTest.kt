package com.earnit.app

import androidx.compose.ui.graphics.Color
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.assertHasClickAction
import androidx.glance.testing.unit.assertHasText
import androidx.glance.testing.unit.hasTestTag
import androidx.glance.unit.ColorProvider
import com.earnit.app.data.CompletionLogEntity
import com.earnit.app.data.RewardEntity
import com.earnit.app.data.RewardProgress
import com.earnit.app.data.RewardTaskCrossRef
import com.earnit.app.data.TaskEntity
import com.earnit.app.widget.ClaimedState
import com.earnit.app.widget.EmptyState
import com.earnit.app.widget.FlashContent
import com.earnit.app.widget.StandardContent
import com.earnit.app.widget.WidgetColors
import com.earnit.app.widget.WidgetTestTags
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Renders the widget's Glance content directly (bypassing provideGlance's Hilt/Room pipeline)
 * and asserts on the resulting node tree via glance-testing. Complements WidgetActionButtonTest,
 * which covers the CLAIM/LOG/ADD_TASK/NONE decision as a pure function; this file catches the
 * next layer up — wrong/missing text, or a button rendered without its click action wired.
 *
 * Note: actionStartActivity(Intent) builds a StartActivityIntentAction, which
 * glance-testing's hasStartActivityClickAction() matcher does not recognize (it only matches the
 * class/componentName-based overloads) — so these tests can confirm a button exists and has SOME
 * click action wired, not that it targets the exact intent/extras.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetContentTest {
    private val context get() = RuntimeEnvironment.getApplication()

    private val colors =
        WidgetColors(
            primary = ColorProvider(Color(0xFFB06000)),
            surface = ColorProvider(Color.White),
            track = ColorProvider(Color(0xFFFFF5DC)),
            onSurface = ColorProvider(Color.Black),
            onSurfaceVar = ColorProvider(Color.DarkGray),
            secondary = ColorProvider(Color.Blue),
        )

    private fun task(
        id: Long,
        repeatable: Boolean = false,
    ) = TaskEntity(id = id, name = "T$id", repeatable = repeatable)

    private fun log(
        taskId: Long,
        points: Int = 1,
    ) = CompletionLogEntity(taskId = taskId, rewardId = 1L, timestamp = 0L, points = points)

    private fun ref(
        taskId: Long,
        mandatory: Boolean = false,
        repeatable: Boolean = false,
    ) = RewardTaskCrossRef(rewardId = 1L, taskId = taskId, isMandatory = mandatory, isRepeatable = repeatable)

    private fun reward(cost: Int = 10) = RewardEntity(id = 1L, name = "Movie Night", cost = cost)

    // ── StandardContent: action button selection ───────────────────────────────

    @Test
    fun standardContent_noTasks_showsOnlyAddTaskButton() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            val progress = RewardProgress(reward(10), emptyList(), emptyList(), emptyList(), emptyList())
            provideComposable { StandardContent(context, progress, "", colors) }

            onNode(hasTestTag(WidgetTestTags.ADD_TASK_BUTTON)).assertExists().assertHasClickAction()
            onNode(hasTestTag(WidgetTestTags.CLAIM_BUTTON)).assertDoesNotExist()
            onNode(hasTestTag(WidgetTestTags.LOG_BUTTON)).assertDoesNotExist()
        }

    @Test
    fun standardContent_unloggedTask_showsOnlyLogButton() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            val progress = RewardProgress(reward(10), listOf(ref(1L)), listOf(task(1L)), emptyList(), emptyList())
            provideComposable { StandardContent(context, progress, "", colors) }

            onNode(hasTestTag(WidgetTestTags.LOG_BUTTON)).assertExists().assertHasClickAction()
            onNode(hasTestTag(WidgetTestTags.ADD_TASK_BUTTON)).assertDoesNotExist()
            onNode(hasTestTag(WidgetTestTags.CLAIM_BUTTON)).assertDoesNotExist()
        }

    @Test
    fun standardContent_canClaim_showsOnlyClaimButton() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            val progress =
                RewardProgress(
                    reward(5),
                    listOf(ref(1L, mandatory = true, repeatable = true)),
                    listOf(task(1L, repeatable = true)),
                    emptyList(),
                    listOf(log(1L, points = 5)),
                )
            provideComposable { StandardContent(context, progress, "", colors) }

            onNode(hasTestTag(WidgetTestTags.CLAIM_BUTTON)).assertExists().assertHasClickAction()
            onNode(hasTestTag(WidgetTestTags.LOG_BUTTON)).assertDoesNotExist()
            onNode(hasTestTag(WidgetTestTags.ADD_TASK_BUTTON)).assertDoesNotExist()
        }

    @Test
    fun standardContent_allTasksDoneBelowCost_showsNoActionButton() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            val progress =
                RewardProgress(
                    reward(10),
                    listOf(ref(1L)),
                    listOf(task(1L)),
                    emptyList(),
                    listOf(log(1L, points = 1)),
                )
            provideComposable { StandardContent(context, progress, "", colors) }

            onNode(hasTestTag(WidgetTestTags.CLAIM_BUTTON)).assertDoesNotExist()
            onNode(hasTestTag(WidgetTestTags.LOG_BUTTON)).assertDoesNotExist()
            onNode(hasTestTag(WidgetTestTags.ADD_TASK_BUTTON)).assertDoesNotExist()
        }

    // ── StandardContent: text content ───────────────────────────────────────────

    @Test
    fun standardContent_showsRewardNameAndCurrentPoints() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            val progress = RewardProgress(reward(10), emptyList(), emptyList(), emptyList(), listOf(log(1L, points = 4)))
            provideComposable { StandardContent(context, progress, "", colors) }

            onNode(hasTestTag(WidgetTestTags.REWARD_NAME)).assertHasText("Movie Night")
            onNode(hasTestTag(WidgetTestTags.PROGRESS_CURRENT)).assertHasText("4")
        }

    @Test
    fun standardContent_customWidgetLabel_overridesRewardName() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            val progress = RewardProgress(reward(10), emptyList(), emptyList(), emptyList(), emptyList())
            provideComposable { StandardContent(context, progress, "My Label", colors) }

            onNode(hasTestTag(WidgetTestTags.REWARD_NAME)).assertHasText("My Label")
        }

    @Test
    fun standardContent_mandatoryTaskUnloggedButPointsMet_showsHint() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            val progress =
                RewardProgress(
                    reward(5),
                    listOf(ref(1L, mandatory = true)),
                    listOf(task(1L)),
                    emptyList(),
                    listOf(log(2L, points = 5)), // points from an unrelated task; mandatory task 1 never logged
                )
            provideComposable { StandardContent(context, progress, "", colors) }

            onNode(hasTestTag(WidgetTestTags.MANDATORY_HINT)).assertExists()
        }

    @Test
    fun standardContent_notBlockedOnMandatoryTask_hidesHint() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            val progress = RewardProgress(reward(10), emptyList(), emptyList(), emptyList(), emptyList())
            provideComposable { StandardContent(context, progress, "", colors) }

            onNode(hasTestTag(WidgetTestTags.MANDATORY_HINT)).assertDoesNotExist()
        }

    // ── Other states ─────────────────────────────────────────────────────────────

    @Test
    fun flashContent_showsCheckAndLoggedMessage() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            provideComposable { FlashContent(context, 1L, colors) }

            onNode(hasTestTag(WidgetTestTags.FLASH_CHECK)).assertHasText("✓")
            onNode(hasTestTag(WidgetTestTags.FLASH_MESSAGE)).assertHasText("Logged!")
        }

    @Test
    fun emptyState_showsConfigurePrompt() =
        runGlanceAppWidgetUnitTest {
            provideComposable { EmptyState(colors) }

            onNode(hasTestTag(WidgetTestTags.EMPTY_TITLE)).assertHasText("EarnIt")
            onNode(hasTestTag(WidgetTestTags.EMPTY_SUBTITLE)).assertHasText("Long-press to configure")
        }

    @Test
    fun claimedState_showsRewardNameAndClaimedSubtitle() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            provideComposable { ClaimedState(context, "Movie Night", colors) }

            onNode(hasTestTag(WidgetTestTags.CLAIMED_NAME)).assertHasText("Movie Night")
            onNode(hasTestTag(WidgetTestTags.CLAIMED_SUBTITLE)).assertHasText("Earned and Claimed")
        }

    @Test
    fun claimedState_blankRewardName_fallsBackToGenericLabel() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            provideComposable { ClaimedState(context, "", colors) }

            onNode(hasTestTag(WidgetTestTags.CLAIMED_NAME)).assertHasText("Reward")
        }
}
