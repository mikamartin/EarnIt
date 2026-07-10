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
    fun debugBackdateLastLog_capsEveryRecentLog_soGlobalMaxActuallyDropsBelowCutoff() =
        runBlocking {
            // Reproduces the real bug: seeded/real data commonly has several near-simultaneous
            // "most recent" logs (e.g. from "Load full test data"). Updating only a single row
            // left the next-newest one as the new global max, so idle time never actually
            // crossed the threshold and NudgeWorker silently no-opped.
            val oldId = database.completionLogDao().insertLog(log(timestamp = 1000L))
            val recentIds =
                listOf(
                    database.completionLogDao().insertLog(log(timestamp = System.currentTimeMillis() - 1000L)),
                    database.completionLogDao().insertLog(log(timestamp = System.currentTimeMillis() - 2000L)),
                    database.completionLogDao().insertLog(log(timestamp = System.currentTimeMillis() - 3000L)),
                )

            repository.debugBackdateLastLog(hoursAgo = 49)

            val logs = database.completionLogDao().getAllLogs().associateBy { it.id }
            val cutoff = System.currentTimeMillis() - 49 * 60 * 60 * 1000L
            recentIds.forEach { id ->
                assertTrue(
                    "Every previously-recent log must be capped at or before the cutoff",
                    logs.getValue(id).timestamp <= cutoff + 5000L,
                )
            }
            assertEquals("Genuinely old log must be untouched", 1000L, logs.getValue(oldId).timestamp)
            assertTrue(
                "Global max must actually drop to/below the cutoff, not just the single newest row",
                (database.completionLogDao().getLastLogTimestamp() ?: 0L) <= cutoff + 5000L,
            )
        }

    @Test
    fun debugBackdateLastLog_isSafeNoOp_whenNoLogsExist() =
        runBlocking {
            repository.debugBackdateLastLog(hoursAgo = 49) // must not throw
            assertEquals(0, database.completionLogDao().getAllLogs().size)
        }
}
