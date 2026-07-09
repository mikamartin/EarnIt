package com.earnit.app

import com.earnit.app.data.CompletionLogEntity
import com.earnit.app.data.RewardEntity
import com.earnit.app.data.RewardProgress
import com.earnit.app.data.RewardTaskCrossRef
import com.earnit.app.data.TaskEntity
import com.earnit.app.widget.WidgetActionButton
import com.earnit.app.widget.widgetActionButtonFor
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regression coverage for the widget's action-button selection (CLAIM / LOG / ADD_TASK / NONE).
 *
 * Extracted from EarnItWidget's StandardContent specifically so this decision is unit-testable
 * without Robolectric/Glance — see WidgetContentTest for the rendered-content coverage.
 */
class WidgetActionButtonTest {
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

    private fun reward(cost: Int = 10) = RewardEntity(id = 1L, name = "R", cost = cost)

    @Test
    fun `no tasks linked returns ADD_TASK`() {
        val progress = RewardProgress(reward(10), emptyList(), emptyList(), emptyList(), emptyList())
        assertEquals(WidgetActionButton.ADD_TASK, widgetActionButtonFor(progress))
    }

    @Test
    fun `unlogged task returns LOG`() {
        val progress = RewardProgress(reward(10), listOf(ref(1L)), listOf(task(1L)), emptyList(), emptyList())
        assertEquals(WidgetActionButton.LOG, widgetActionButtonFor(progress))
    }

    @Test
    fun `repeatable task already logged still returns LOG`() {
        val progress =
            RewardProgress(
                reward(10),
                listOf(ref(1L, repeatable = true)),
                listOf(task(1L, repeatable = true)),
                emptyList(),
                listOf(log(1L)),
            )
        assertEquals(WidgetActionButton.LOG, widgetActionButtonFor(progress))
    }

    @Test
    fun `non-repeatable task already logged and points below cost returns NONE`() {
        val progress =
            RewardProgress(
                reward(10),
                listOf(ref(1L)),
                listOf(task(1L)),
                emptyList(),
                listOf(log(1L, points = 1)),
            )
        assertEquals(WidgetActionButton.NONE, widgetActionButtonFor(progress))
    }

    @Test
    fun `canClaim true returns CLAIM even if a repeatable task is still loggable`() {
        val progress =
            RewardProgress(
                reward(5),
                listOf(ref(1L, mandatory = true, repeatable = true)),
                listOf(task(1L, repeatable = true)),
                emptyList(),
                listOf(log(1L, points = 5)),
            )
        assertEquals(WidgetActionButton.CLAIM, widgetActionButtonFor(progress))
    }

    @Test
    fun `mandatory task unlogged blocks CLAIM even when points met`() {
        val progress =
            RewardProgress(
                reward(5),
                listOf(ref(1L, mandatory = true)),
                listOf(task(1L)),
                emptyList(),
                listOf(log(2L, points = 5)), // points from an unrelated task; mandatory task 1 never logged
            )
        assertEquals(WidgetActionButton.LOG, widgetActionButtonFor(progress))
    }
}
