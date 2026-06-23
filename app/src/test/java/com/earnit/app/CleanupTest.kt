package com.earnit.app

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.just
import kotlinx.coroutines.runBlocking
import org.junit.Test

class CleanupTest : RepositoryTestBase() {
    @Test
    fun `clearAllLogs deletes all completion logs including archived ones`() =
        runBlocking {
            coEvery { logDao.deleteAllLogs() } just Runs
            coEvery { historyDao.deleteAllEntries() } just Runs

            repository.clearAllLogs()

            coVerify(exactly = 1) { logDao.deleteAllLogs() }
            coVerify(exactly = 1) { historyDao.deleteAllEntries() }
            // Must NOT call the active-only variant — that would leave archived logs behind
            coVerify(exactly = 0) { logDao.deleteAllActiveLogs() }
        }

    @Test
    fun `clearAllTasks removes cross refs then deletes all tasks`() =
        runBlocking {
            coEvery { rewardTaskDao.deleteAll() } just Runs
            coEvery { taskDao.deleteAllTasks() } just Runs

            repository.clearAllTasks()

            coVerifyOrder {
                rewardTaskDao.deleteAll()
                taskDao.deleteAllTasks()
            }
        }

    @Test
    fun `clearAllRewards removes cross refs and active logs but not all logs`() =
        runBlocking {
            coEvery { rewardTaskDao.deleteAll() } just Runs
            coEvery { logDao.deleteAllActiveLogs() } just Runs
            coEvery { rewardDao.deleteAllRewards() } just Runs

            repository.clearAllRewards()

            coVerify { rewardTaskDao.deleteAll() }
            coVerify { logDao.deleteAllActiveLogs() }
            coVerify { rewardDao.deleteAllRewards() }
            // Must NOT wipe archived/history logs — that is clearAllLogs's job
            coVerify(exactly = 0) { logDao.deleteAllLogs() }
        }
}
