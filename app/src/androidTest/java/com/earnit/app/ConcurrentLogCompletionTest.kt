package com.earnit.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earnit.app.data.RewardEntity
import com.earnit.app.data.TaskEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards against two near-simultaneous LOG taps for the same non-repeatable task both
 * succeeding before the UI's loggable-state re-evaluation disables the button. Uses a real
 * in-memory Room database; no mocks.
 */
@RunWith(AndroidJUnit4::class)
class ConcurrentLogCompletionTest : RoomIntegrationBase() {
    @Test
    fun logCompletion_calledTwiceConcurrently_forSameNonRepeatableTask_writesOnlyOneLog() =
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
            // isRepeatable = false: this task should only ever be loggable once.
            repository.saveRewardTasks(rewardId, listOf(Triple(taskId, false, false)))

            val task = repository.getTaskOrNull(taskId)!!

            val first = async { repository.logCompletion(task, rewardId = rewardId, detail = "Tap 1") }
            val second = async { repository.logCompletion(task, rewardId = rewardId, detail = "Tap 2") }
            first.await()
            second.await()

            assertEquals(
                "Only the first of two concurrent logs for a non-repeatable task should be written",
                1,
                database.completionLogDao().getAllLogs().size,
            )
        }
}
