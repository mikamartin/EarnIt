package com.earnit.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earnit.app.data.RewardEntity
import com.earnit.app.data.TaskEntity
import com.earnit.app.tags.RepositoryTest
import com.earnit.app.tags.Reward
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards against logging completions against a reward that has already been archived (claimed).
 * Uses a real in-memory Room database; no mocks.
 */
@RepositoryTest
@Reward
@RunWith(AndroidJUnit4::class)
class LogAgainstArchivedRewardTest : RoomIntegrationBase() {
    @Test
    fun logCompletion_onArchivedReward_isSkipped() =
        runBlocking {
            val taskId =
                repository.upsertTask(
                    TaskEntity(name = "Morning Run", points = 5, icon = "🏃"),
                )
            assertNotEquals(0L, taskId)

            val rewardId =
                repository.upsertReward(
                    RewardEntity(name = "Coffee Treat", cost = 5),
                )
            repository.saveRewardTasks(rewardId, listOf(Triple(taskId, true, false)))

            val task = repository.getTaskOrNull(taskId)!!
            repository.logCompletion(task, rewardId = rewardId, detail = "Felt great")

            // Claim the reward the real way, so it becomes archived.
            repository.claimReward(rewardId, startOver = false)
            assertTrue(
                "Reward should be archived after claiming",
                repository.getRewardOrNull(rewardId)?.isArchived == true,
            )

            // A stale UI (e.g. a "Log" button tapped just before recomposition) tries to log
            // against the now-archived reward. This must be a no-op, not a second log.
            repository.logCompletion(task, rewardId = rewardId, detail = "Too late")

            assertEquals(
                "No log row should be written for an archived reward",
                1,
                database.completionLogDao().getAllLogs().size,
            )

            val state = repository.observeUiState().first()
            assertEquals(1, state.historyEntries.size)
            assertEquals(1, state.historyEntries[0].logs.size)
            assertEquals(
                "Felt great",
                state.historyEntries[0].logs[0].detail,
            )
        }
}
