package com.earnit.app

import com.earnit.app.data.CompletionLogDao
import com.earnit.app.data.EarnItDatabase
import com.earnit.app.data.EarnItRepository
import com.earnit.app.data.HistoryDao
import com.earnit.app.data.RewardDao
import com.earnit.app.data.RewardTaskCrossRefDao
import com.earnit.app.data.TaskDao
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test

class CleanupTest {
    private val database = mockk<EarnItDatabase>(relaxed = true)
    private val rewardDao = mockk<RewardDao>()
    private val logDao = mockk<CompletionLogDao>()
    private val historyDao = mockk<HistoryDao>()
    private val rewardTaskDao = mockk<RewardTaskCrossRefDao>()
    private val taskDao = mockk<TaskDao>()

    init {
        every { database.rewardDao() } returns rewardDao
        every { database.completionLogDao() } returns logDao
        every { database.historyDao() } returns historyDao
        every { database.rewardTaskCrossRefDao() } returns rewardTaskDao
        every { database.taskDao() } returns taskDao
    }

    private val repository = EarnItRepository(database)

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
