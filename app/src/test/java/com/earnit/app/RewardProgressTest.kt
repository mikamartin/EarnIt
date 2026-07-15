package com.earnit.app

import com.earnit.app.data.CompletionLogEntity
import com.earnit.app.data.RewardEntity
import com.earnit.app.data.RewardProgress
import com.earnit.app.data.RewardTaskCrossRef
import com.earnit.app.data.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardProgressTest {
    private fun task(id: Long) = TaskEntity(id = id, name = "T$id", points = 5)

    private fun log(
        taskId: Long,
        points: Int,
    ) = CompletionLogEntity(taskId = taskId, rewardId = 1L, timestamp = 0L, points = points)

    private fun ref(
        taskId: Long,
        isMandatory: Boolean = false,
        isRepeatable: Boolean = false,
    ) = RewardTaskCrossRef(rewardId = 1L, taskId = taskId, isMandatory = isMandatory, isRepeatable = isRepeatable)

    private fun reward(cost: Int) = RewardEntity(id = 1, name = "R", cost = cost)

    @Test
    fun `totalPoints sums active log points`() {
        val progress =
            RewardProgress(
                reward = reward(20),
                taskRefs = emptyList(),
                mandatoryTasks = emptyList(),
                optionalTasks = emptyList(),
                activeLogs = listOf(log(1L, 5), log(2L, 7)),
            )
        assertEquals(12, progress.totalPoints)
    }

    @Test
    fun `totalPoints is zero with no logs`() {
        val progress =
            RewardProgress(
                reward = reward(10),
                taskRefs = emptyList(),
                mandatoryTasks = emptyList(),
                optionalTasks = emptyList(),
                activeLogs = emptyList(),
            )
        assertEquals(0, progress.totalPoints)
    }

    @Test
    fun `canClaim false when points below cost`() {
        val progress =
            RewardProgress(
                reward = reward(20),
                taskRefs = emptyList(),
                mandatoryTasks = emptyList(),
                optionalTasks = emptyList(),
                activeLogs = listOf(log(1L, 5)),
            )
        assertFalse(progress.canClaim)
    }

    @Test
    fun `canClaim false when mandatory task not logged despite enough points`() {
        val mandatory = task(1L)
        val progress =
            RewardProgress(
                reward = reward(5),
                taskRefs = listOf(ref(1L, isMandatory = true)),
                mandatoryTasks = listOf(mandatory),
                optionalTasks = emptyList(),
                activeLogs = listOf(log(2L, 10)),
            )
        assertFalse(progress.canClaim)
    }

    @Test
    fun `canClaim true when points met and all mandatory tasks logged`() {
        val mandatory = task(1L)
        val progress =
            RewardProgress(
                reward = reward(5),
                taskRefs = listOf(ref(1L, isMandatory = true)),
                mandatoryTasks = listOf(mandatory),
                optionalTasks = emptyList(),
                activeLogs = listOf(log(1L, 10)),
            )
        assertTrue(progress.canClaim)
    }

    @Test
    fun `canClaim true when no mandatory tasks and points met`() {
        val progress =
            RewardProgress(
                reward = reward(5),
                taskRefs = emptyList(),
                mandatoryTasks = emptyList(),
                optionalTasks = listOf(task(1L)),
                activeLogs = listOf(log(1L, 5)),
            )
        assertTrue(progress.canClaim)
    }

    @Test
    fun `showsProgressNumbers true when points below cost`() {
        val progress =
            RewardProgress(
                reward = reward(20),
                taskRefs = emptyList(),
                mandatoryTasks = emptyList(),
                optionalTasks = emptyList(),
                activeLogs = listOf(log(1L, 5)),
            )
        assertTrue(progress.showsProgressNumbers)
    }

    @Test
    fun `showsProgressNumbers false when points meet cost and mandatory task unlogged`() {
        val mandatory = task(1L)
        val progress =
            RewardProgress(
                reward = reward(5),
                taskRefs = listOf(ref(1L, isMandatory = true)),
                mandatoryTasks = listOf(mandatory),
                optionalTasks = emptyList(),
                activeLogs = listOf(log(2L, 10)),
            )
        assertFalse(progress.showsProgressNumbers)
    }

    @Test
    fun `showsProgressNumbers false when reward can be claimed`() {
        val progress =
            RewardProgress(
                reward = reward(5),
                taskRefs = emptyList(),
                mandatoryTasks = emptyList(),
                optionalTasks = listOf(task(1L)),
                activeLogs = listOf(log(1L, 5)),
            )
        assertFalse(progress.showsProgressNumbers)
    }

    @Test
    fun `showsProgressNumbers false when cost is zero and no mandatory tasks`() {
        val progress =
            RewardProgress(
                reward = reward(0),
                taskRefs = emptyList(),
                mandatoryTasks = emptyList(),
                optionalTasks = emptyList(),
                activeLogs = emptyList(),
            )
        assertFalse(progress.showsProgressNumbers)
    }

    @Test
    fun `loggableTasks includes task not yet logged`() {
        val t1 = task(1L)
        val progress =
            RewardProgress(
                reward = reward(0),
                taskRefs = listOf(ref(1L)),
                mandatoryTasks = listOf(t1),
                optionalTasks = emptyList(),
                activeLogs = emptyList(),
            )
        assertEquals(listOf(t1), progress.loggableTasks)
    }

    @Test
    fun `loggableTasks excludes non-repeatable task already logged`() {
        val t1 = task(1L)
        val progress =
            RewardProgress(
                reward = reward(0),
                taskRefs = listOf(ref(1L, isRepeatable = false)),
                mandatoryTasks = listOf(t1),
                optionalTasks = emptyList(),
                activeLogs = listOf(log(1L, 5)),
            )
        assertTrue(progress.loggableTasks.isEmpty())
    }

    @Test
    fun `loggableTasks includes repeatable task even when already logged`() {
        val t1 = task(1L)
        val progress =
            RewardProgress(
                reward = reward(0),
                taskRefs = listOf(ref(1L, isRepeatable = true)),
                mandatoryTasks = listOf(t1),
                optionalTasks = emptyList(),
                activeLogs = listOf(log(1L, 5)),
            )
        assertEquals(listOf(t1), progress.loggableTasks)
    }

    @Test
    fun `loggableTasks filters correctly across mixed task set`() {
        val repeatable = task(1L)
        val nonRepeatable = task(2L)
        val unlogged = task(3L)
        val progress =
            RewardProgress(
                reward = reward(0),
                taskRefs =
                    listOf(
                        ref(1L, isRepeatable = true),
                        ref(2L, isRepeatable = false),
                        ref(3L, isRepeatable = false),
                    ),
                mandatoryTasks = emptyList(),
                optionalTasks = listOf(repeatable, nonRepeatable, unlogged),
                activeLogs = listOf(log(1L, 5), log(2L, 5)),
            )
        // repeatable(1) logged but repeatable → in; nonRepeatable(2) logged → out; unlogged(3) → in
        assertEquals(listOf(repeatable, unlogged), progress.loggableTasks)
    }

    @Test
    fun `allTasks returns mandatory followed by optional`() {
        val t1 = task(1L)
        val t2 = task(2L)
        val t3 = task(3L)
        val progress =
            RewardProgress(
                reward = reward(0),
                taskRefs = emptyList(),
                mandatoryTasks = listOf(t1, t2),
                optionalTasks = listOf(t3),
                activeLogs = emptyList(),
            )
        assertEquals(listOf(t1, t2, t3), progress.allTasks)
    }
}
