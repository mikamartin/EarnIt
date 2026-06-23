package com.earnit.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earnit.app.data.RewardEntity
import com.earnit.app.data.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the "start over" claim flow:
 *   history entry is created, logs archived, reward stays active, balance resets.
 * Uses a real in-memory Room database; no mocks.
 */
@RunWith(AndroidJUnit4::class)
class StartOverTest : RoomIntegrationBase() {
    @Test
    fun claimStartOver_createsHistoryEntry_andRewardRemainsActive() =
        runBlocking {
            val taskId = repository.upsertTask(TaskEntity(name = "Meditate", points = 10))
            val rewardId = repository.upsertReward(RewardEntity(name = "Spa Day", cost = 10))
            repository.saveRewardTasks(rewardId, listOf(Triple(taskId, false, false)))
            val task = repository.getTaskOrNull(taskId)!!
            repository.logCompletion(task, rewardId, detail = "peaceful")

            repository.claimReward(rewardId, startOver = true)

            val state = repository.observeUiState().first()

            assertEquals(1, state.historyEntries.size)
            assertEquals("Spa Day", state.historyEntries[0].entry.rewardName)

            val stillActive = state.rewardProgressList.find { it.reward.id == rewardId }
            assertNotNull("Reward should remain in active list after start-over", stillActive)
            assertFalse("Reward should not be archived", stillActive!!.reward.isArchived)
        }

    @Test
    fun claimStartOver_resetsPointBalance_toZero() =
        runBlocking {
            val taskId = repository.upsertTask(TaskEntity(name = "Meditate", points = 10))
            val rewardId = repository.upsertReward(RewardEntity(name = "Spa Day", cost = 10))
            repository.saveRewardTasks(rewardId, listOf(Triple(taskId, false, false)))
            val task = repository.getTaskOrNull(taskId)!!
            repository.logCompletion(task, rewardId, detail = "")

            repository.claimReward(rewardId, startOver = true)

            val progress =
                repository
                    .observeUiState()
                    .first()
                    .rewardProgressList
                    .find { it.reward.id == rewardId }!!
            assertEquals("Balance should reset to 0 after start-over", 0, progress.totalPoints)
            assertFalse("canClaim should be false with zero points after reset", progress.canClaim)
        }

    @Test
    fun claimStartOver_allowsImmediateSecondCycle_afterReset() =
        runBlocking {
            val taskId = repository.upsertTask(TaskEntity(name = "Run", points = 5))
            val rewardId = repository.upsertReward(RewardEntity(name = "Movie Night", cost = 5))
            repository.saveRewardTasks(rewardId, listOf(Triple(taskId, false, false)))
            val task = repository.getTaskOrNull(taskId)!!

            // First cycle
            repository.logCompletion(task, rewardId, detail = "first")
            repository.claimReward(rewardId, startOver = true)

            // Second cycle
            repository.logCompletion(task, rewardId, detail = "second")

            val state = repository.observeUiState().first()
            val progress = state.rewardProgressList.find { it.reward.id == rewardId }!!
            assertEquals(5, progress.totalPoints)
            assertTrue("Should be claimable again after second cycle", progress.canClaim)
            assertEquals("First cycle should be in history", 1, state.historyEntries.size)
        }
}
