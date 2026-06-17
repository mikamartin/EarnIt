package com.earnit.app

import com.earnit.app.data.CompletionLogEntity
import com.earnit.app.data.RewardEntity
import com.earnit.app.data.RewardProgress
import com.earnit.app.data.RewardTaskCrossRef
import com.earnit.app.data.TaskEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GatekeeperTest {
    private fun task(id: Long) = TaskEntity(id = id, name = "T$id", points = 5)

    private fun log(
        taskId: Long,
        points: Int,
    ) = CompletionLogEntity(taskId = taskId, rewardId = 1L, timestamp = 0L, points = points)

    private fun ref(taskId: Long) = RewardTaskCrossRef(rewardId = 1L, taskId = taskId, isMandatory = true)

    private fun reward(cost: Int) = RewardEntity(id = 1L, name = "R", cost = cost)

    // ── Points boundary ───────────────────────────────────────────────────────

    @Test
    fun `canClaim true when points exactly equal cost`() {
        val progress =
            RewardProgress(
                reward = reward(10),
                taskRefs = emptyList(),
                mandatoryTasks = emptyList(),
                optionalTasks = emptyList(),
                activeLogs = listOf(log(1L, 10)),
            )
        assertTrue(progress.canClaim)
    }

    @Test
    fun `canClaim false when points one below cost`() {
        val progress =
            RewardProgress(
                reward = reward(10),
                taskRefs = emptyList(),
                mandatoryTasks = emptyList(),
                optionalTasks = emptyList(),
                activeLogs = listOf(log(1L, 9)),
            )
        assertFalse(progress.canClaim)
    }

    @Test
    fun `canClaim true when reward cost is zero and no mandatory tasks`() {
        val progress =
            RewardProgress(
                reward = reward(0),
                taskRefs = emptyList(),
                mandatoryTasks = emptyList(),
                optionalTasks = emptyList(),
                activeLogs = emptyList(),
            )
        assertTrue(progress.canClaim)
    }

    // ── Multiple mandatory tasks ───────────────────────────────────────────────

    @Test
    fun `canClaim false when one of multiple mandatory tasks not logged`() {
        val t1 = task(1L)
        val t2 = task(2L)
        val progress =
            RewardProgress(
                reward = reward(5),
                taskRefs = listOf(ref(1L), ref(2L)),
                mandatoryTasks = listOf(t1, t2),
                optionalTasks = emptyList(),
                activeLogs = listOf(log(1L, 10)), // t2 never logged
            )
        assertFalse(progress.canClaim)
    }

    @Test
    fun `canClaim true when all mandatory tasks logged and points met`() {
        val t1 = task(1L)
        val t2 = task(2L)
        val progress =
            RewardProgress(
                reward = reward(5),
                taskRefs = listOf(ref(1L), ref(2L)),
                mandatoryTasks = listOf(t1, t2),
                optionalTasks = emptyList(),
                activeLogs = listOf(log(1L, 3), log(2L, 3)),
            )
        assertTrue(progress.canClaim)
    }

    @Test
    fun `canClaim true when mandatory task logged multiple times`() {
        val t1 = task(1L)
        val progress =
            RewardProgress(
                reward = reward(5),
                taskRefs = listOf(ref(1L)),
                mandatoryTasks = listOf(t1),
                optionalTasks = emptyList(),
                activeLogs = listOf(log(1L, 3), log(1L, 3)),
            )
        assertTrue(progress.canClaim)
    }

    @Test
    fun `canClaim false when cost zero but mandatory task not logged`() {
        val t1 = task(1L)
        val progress =
            RewardProgress(
                reward = reward(0),
                taskRefs = listOf(ref(1L)),
                mandatoryTasks = listOf(t1),
                optionalTasks = emptyList(),
                activeLogs = emptyList(),
            )
        assertFalse(progress.canClaim)
    }
}
