package com.earnit.app

import com.earnit.app.data.CompletionLogEntity
import com.earnit.app.data.TaskEntity
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Tests how points are attributed at log time (auto vs manual) and what gets snapshotted.
class LogAttributionTest : RepositoryTestBase() {
    // ── Auto points ───────────────────────────────────────────────────────────

    @Test
    fun `logCompletion uses auto-point formula when useAutoPoints is true`() =
        runBlocking {
            val task =
                TaskEntity(
                    id = 1,
                    name = "Run",
                    useAutoPoints = true,
                    time = 2,
                    difficulty = 3,
                    preparation = 1,
                    points = 0,
                )
            val captured = mutableListOf<CompletionLogEntity>()
            coEvery { logDao.insertLog(capture(captured)) } returns 1L

            repository.logCompletion(task, rewardId = 10L, detail = "")

            // ((2+1)*(3+1)*(1+1)+7)/8 = 31/8 = 3, no dimension==5 so no bonus
            assertEquals(3, captured[0].points)
        }

    @Test
    fun `logCompletion uses manual points when useAutoPoints is false`() =
        runBlocking {
            val task = TaskEntity(id = 1, name = "Run", useAutoPoints = false, points = 7)
            val captured = mutableListOf<CompletionLogEntity>()
            coEvery { logDao.insertLog(capture(captured)) } returns 1L

            repository.logCompletion(task, rewardId = 10L, detail = "")

            assertEquals(7, captured[0].points)
        }

    // ── Snapshot integrity ────────────────────────────────────────────────────

    @Test
    fun `logCompletion snapshots task name at log time`() =
        runBlocking {
            val task = TaskEntity(id = 5, name = "Yoga", useAutoPoints = false, points = 3)
            val captured = mutableListOf<CompletionLogEntity>()
            coEvery { logDao.insertLog(capture(captured)) } returns 1L

            repository.logCompletion(task, rewardId = 42L, detail = "morning flow")

            assertEquals("Yoga", captured[0].taskName)
        }

    @Test
    fun `logCompletion records correct rewardId and detail`() =
        runBlocking {
            val task = TaskEntity(id = 5, name = "Yoga", useAutoPoints = false, points = 3)
            val captured = mutableListOf<CompletionLogEntity>()
            coEvery { logDao.insertLog(capture(captured)) } returns 1L

            repository.logCompletion(task, rewardId = 42L, detail = "morning flow")

            assertEquals(42L, captured[0].rewardId)
            assertEquals("morning flow", captured[0].detail)
        }

    @Test
    fun `logCompletion log is active (historyEntryId is null)`() =
        runBlocking {
            val task = TaskEntity(id = 1, name = "Run", useAutoPoints = false, points = 5)
            val captured = mutableListOf<CompletionLogEntity>()
            coEvery { logDao.insertLog(capture(captured)) } returns 1L

            repository.logCompletion(task, rewardId = 1L, detail = "")

            assertNull(captured[0].historyEntryId)
        }
}
