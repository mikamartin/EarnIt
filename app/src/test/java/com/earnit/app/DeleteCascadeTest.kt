package com.earnit.app

import com.earnit.app.data.RewardEntity
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Cross-ref cleanup for both deleteTask and deleteReward is handled declaratively by the FK
// cascade on RewardTaskCrossRef (see Entities.kt), not by an explicit repository call — MockK
// can't simulate SQLite cascade behaviour, so these tests only assert the repository no longer
// does it manually. Real cascade behaviour is verified against a live in-memory Room database in
// ClearCascadeTest (deleteTask_removesCrossRefs_rewardHasNoTasks and
// deleteReward_removesActiveLogsAndCrossRefs_taskStillExists).
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

    // A widget tracking a deleted-but-never-claimed reward needs to find out live, not just at
    // the moment it first asked — this guards against observeRewardExists silently degrading
    // back into a one-shot snapshot of whatever the row looked like when first subscribed.
    @Test
    fun `observeRewardExists reflects live changes to the underlying row, not just its value at subscribe time`() =
        runBlocking {
            val rewardRow = MutableStateFlow<RewardEntity?>(RewardEntity(id = 1L, name = "Movie Night", cost = 10))
            every { rewardDao.observeReward(1L) } returns rewardRow

            assertTrue(repository.observeRewardExists(1L).first())

            rewardRow.value = null

            assertFalse(repository.observeRewardExists(1L).first())
        }
}
