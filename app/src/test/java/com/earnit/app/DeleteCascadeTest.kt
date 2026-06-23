package com.earnit.app

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.just
import kotlinx.coroutines.runBlocking
import org.junit.Test

class DeleteCascadeTest : RepositoryTestBase() {
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
