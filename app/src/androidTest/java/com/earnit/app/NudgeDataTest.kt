package com.earnit.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earnit.app.data.CompletionLogEntity
import com.earnit.app.data.RewardEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real in-memory Room coverage for the SQL the nudge feature depends on. NudgeWorkerTest mocks
 * these DAO queries, so a subtly wrong subquery (wrong row picked, wrong filter) wouldn't be
 * caught there. debugBackdateLastLog in particular backs MANUAL_TEST_PLAN.md's only remaining
 * verification of NudgeWorker's real wiring — if its SQL were wrong, that manual journey would
 * silently validate against corrupted state instead of catching the bug.
 */
@RunWith(AndroidJUnit4::class)
class NudgeDataTest : RoomIntegrationBase() {
    private fun log(
        timestamp: Long,
        rewardId: Long = 1L,
    ) = CompletionLogEntity(taskId = 1L, rewardId = rewardId, timestamp = timestamp, points = 1)

    @Test
    fun getLastLogTimestamp_returnsNull_whenNoLogs() =
        runBlocking {
            assertNull(database.completionLogDao().getLastLogTimestamp())
        }

    @Test
    fun getLastLogTimestamp_returnsMaxTimestamp_amongOutOfOrderLogs() =
        runBlocking {
            database.completionLogDao().insertLog(log(timestamp = 1000L))
            database.completionLogDao().insertLog(log(timestamp = 3000L))
            database.completionLogDao().insertLog(log(timestamp = 2000L))

            assertEquals(3000L, database.completionLogDao().getLastLogTimestamp())
        }

    @Test
    fun getActiveRewardCount_returnsZero_whenNoRewards() =
        runBlocking {
            assertEquals(0, database.rewardDao().getActiveRewardCount())
        }

    @Test
    fun getActiveRewardCount_countsOnlyNonArchivedRewards() =
        runBlocking {
            repository.upsertReward(RewardEntity(name = "Active", cost = 5))
            repository.upsertReward(RewardEntity(name = "Also active", cost = 5))
            repository.upsertReward(RewardEntity(name = "Archived", cost = 5, isArchived = true))

            assertEquals(2, database.rewardDao().getActiveRewardCount())
        }

    @Test
    fun debugBackdateLastLog_updatesOnlyTheMostRecentLog() =
        runBlocking {
            val olderId = database.completionLogDao().insertLog(log(timestamp = 1000L))
            val newestId = database.completionLogDao().insertLog(log(timestamp = 5000L))

            repository.debugBackdateLastLog(hoursAgo = 49)

            val logs = database.completionLogDao().getAllLogs().associateBy { it.id }
            val expected = System.currentTimeMillis() - 49 * 60 * 60 * 1000L
            assertTrue(
                "Backdated timestamp should land within 5s of the expected value",
                kotlin.math.abs(logs.getValue(newestId).timestamp - expected) < 5000L,
            )
            assertEquals("Older log must be untouched", 1000L, logs.getValue(olderId).timestamp)
        }

    @Test
    fun debugBackdateLastLog_isSafeNoOp_whenNoLogsExist() =
        runBlocking {
            repository.debugBackdateLastLog(hoursAgo = 49) // must not throw
            assertEquals(0, database.completionLogDao().getAllLogs().size)
        }
}
