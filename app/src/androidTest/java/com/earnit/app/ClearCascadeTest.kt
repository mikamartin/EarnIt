package com.earnit.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earnit.app.data.EarnItDatabase
import com.earnit.app.data.EarnItRepository
import com.earnit.app.data.RewardEntity
import com.earnit.app.data.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies cascade / cleanup behaviour:
 *   clearAllLogs, clearAllTasks, clearAllRewards, deleteTask, deleteReward.
 * Uses a real in-memory Room database; no mocks.
 */
@RunWith(AndroidJUnit4::class)
class ClearCascadeTest {
    private lateinit var database: EarnItDatabase
    private lateinit var repository: EarnItRepository

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    EarnItDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        repository = EarnItRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** Creates a task + reward linked together, logs the task once. Returns taskId to rewardId. */
    private suspend fun seedLinkedData(): Pair<Long, Long> {
        val taskId = repository.upsertTask(TaskEntity(name = "Run", points = 5))
        val rewardId = repository.upsertReward(RewardEntity(name = "Coffee", cost = 5))
        repository.saveRewardTasks(rewardId, listOf(Triple(taskId, false, false)))
        repository.logCompletion(repository.getTaskOrNull(taskId)!!, rewardId, detail = "")
        return taskId to rewardId
    }

    @Test
    fun clearAllLogs_removesActiveAndArchivedLogs() =
        runBlocking {
            val (_, rewardId) = seedLinkedData()
            // Archive the log via a claim so we have both active and archived logs
            repository.claimReward(rewardId, startOver = true)
            repository.logCompletion(
                repository.getTaskOrNull(
                    repository
                        .observeUiState()
                        .first()
                        .tasks[0]
                        .id,
                )!!,
                rewardId,
                "",
            )

            repository.clearAllLogs()

            val allLogs = repository.observeUiState().first().allLogs
            assertEquals("All logs (active and archived) should be deleted", 0, allLogs.size)
        }

    @Test
    fun clearAllTasks_removesTasksAndCrossRefs_rewardStillExists() =
        runBlocking {
            val (_, rewardId) = seedLinkedData()

            repository.clearAllTasks()

            val state = repository.observeUiState().first()
            assertEquals("Tasks should be empty", 0, state.tasks.size)
            assertNotNull("Reward should still exist", repository.getRewardOrNull(rewardId))
            val progress = state.rewardProgressList.find { it.reward.id == rewardId }!!
            assertEquals("Cross refs should be cleared — reward has no tasks", 0, progress.allTasks.size)
        }

    @Test
    fun clearAllRewards_removesRewardsAndActiveLogs_taskStillExists() =
        runBlocking {
            val (taskId, _) = seedLinkedData()

            repository.clearAllRewards()

            val state = repository.observeUiState().first()
            assertEquals("Rewards list should be empty", 0, state.rewardProgressList.size)
            assertNotNull("Task should still exist", repository.getTaskOrNull(taskId))
            val activeLogs = state.allLogs.filter { it.historyEntryId == null }
            assertEquals("Active logs should be deleted with rewards", 0, activeLogs.size)
        }

    @Test
    fun deleteTask_removesCrossRefs_rewardHasNoTasks() =
        runBlocking {
            val (taskId, rewardId) = seedLinkedData()

            repository.deleteTask(taskId)

            val state = repository.observeUiState().first()
            assertNull("Task should be deleted", repository.getTaskOrNull(taskId))
            val progress = state.rewardProgressList.find { it.reward.id == rewardId }!!
            assertEquals("Cross ref should be removed after task deletion", 0, progress.allTasks.size)
        }

    @Test
    fun deleteReward_removesActiveLogsAndCrossRefs_taskStillExists() =
        runBlocking {
            val (taskId, rewardId) = seedLinkedData()

            repository.deleteReward(rewardId)

            val state = repository.observeUiState().first()
            assertNull("Reward should be deleted", repository.getRewardOrNull(rewardId))
            assertNotNull("Task should still exist", repository.getTaskOrNull(taskId))
            val logsForDeletedReward = state.allLogs.filter { it.rewardId == rewardId }
            assertEquals("Active logs for deleted reward should be removed", 0, logsForDeletedReward.size)
        }
}
