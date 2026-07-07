package com.earnit.app

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.just
import kotlinx.coroutines.runBlocking
import org.junit.Test

// Cross-ref cleanup for both deleteTask and deleteReward is handled declaratively by the FK
// cascade on RewardTaskCrossRef (see Entities.kt), not by an explicit repository call — MockK
// can't simulate SQLite cascade behaviour, so these tests only assert the repository no longer
// does it manually. Real cascade behaviour is covered by MANUAL_TEST_PLAN.md / TESTING.md.
class DeleteCascadeTest : RepositoryTestBase() {
    @Test
    fun `deleteTask deletes the task`() =
        runBlocking {
            coEvery { taskDao.deleteTask(5L) } just Runs

            repository.deleteTask(5L)

            coVerify(exactly = 1) { taskDao.deleteTask(5L) }
        }

    @Test
    fun `deleteReward clears active logs before deleting the reward, without manually clearing cross refs`() =
        runBlocking {
            coEvery { logDao.deleteActiveLogsForReward(3L) } just Runs
            coEvery { rewardDao.deleteReward(3L) } just Runs

            repository.deleteReward(3L)

            coVerifyOrder {
                logDao.deleteActiveLogsForReward(3L)
                rewardDao.deleteReward(3L)
            }
            coVerify(exactly = 0) { rewardTaskDao.clearTasksForReward(any()) }
        }
}
