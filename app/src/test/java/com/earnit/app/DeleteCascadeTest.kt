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
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test

class DeleteCascadeTest {
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
    fun `deleteTask clears cross refs before deleting the task`() =
        runBlocking {
            coEvery { rewardTaskDao.clearRewardsForTask(5L) } just Runs
            coEvery { taskDao.deleteTask(5L) } just Runs

            repository.deleteTask(5L)

            coVerifyOrder {
                rewardTaskDao.clearRewardsForTask(5L)
                taskDao.deleteTask(5L)
            }
        }

    @Test
    fun `deleteReward clears cross refs and active logs before deleting the reward`() =
        runBlocking {
            coEvery { rewardTaskDao.clearTasksForReward(3L) } just Runs
            coEvery { logDao.deleteActiveLogsForReward(3L) } just Runs
            coEvery { rewardDao.deleteReward(3L) } just Runs

            repository.deleteReward(3L)

            coVerifyOrder {
                rewardTaskDao.clearTasksForReward(3L)
                logDao.deleteActiveLogsForReward(3L)
                rewardDao.deleteReward(3L)
            }
        }
}
