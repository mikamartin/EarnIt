package com.earnit.app

import com.earnit.app.data.HistoryEntryEntity
import com.earnit.app.data.RewardEntity
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ClaimRewardStartOverTest : RepositoryTestBase() {
    @Test
    fun `claimReward startOver true still creates a history entry`() =
        runBlocking {
            val reward = RewardEntity(id = 1, name = "Trip", cost = 50, icon = "🏖️")
            coEvery { rewardDao.getReward(1) } returns reward
            coEvery { historyDao.insertEntry(any()) } returns 99L
            coEvery { logDao.archiveLogsForReward(any(), any()) } just Runs

            repository.claimReward(1, startOver = true)

            coVerify(exactly = 1) { historyDao.insertEntry(any()) }
        }

    @Test
    fun `claimReward startOver true archives active logs under the new history entry`() =
        runBlocking {
            val reward = RewardEntity(id = 1, name = "Trip", cost = 50)
            coEvery { rewardDao.getReward(1) } returns reward
            coEvery { historyDao.insertEntry(any()) } returns 42L
            coEvery { logDao.archiveLogsForReward(any(), any()) } just Runs

            repository.claimReward(1, startOver = true)

            coVerify { logDao.archiveLogsForReward(1L, 42L) }
        }

    @Test
    fun `claimReward snapshots reward name icon and cost into history entry`() =
        runBlocking {
            val reward = RewardEntity(id = 1, name = "Beach Trip", cost = 50, icon = "🏖️")
            val captured = mutableListOf<HistoryEntryEntity>()
            coEvery { rewardDao.getReward(1) } returns reward
            coEvery { historyDao.insertEntry(capture(captured)) } returns 99L
            coEvery { logDao.archiveLogsForReward(any(), any()) } just Runs
            coEvery { rewardDao.updateReward(any()) } just Runs

            repository.claimReward(1, startOver = false)

            assertEquals("Beach Trip", captured[0].rewardName)
            assertEquals("🏖️", captured[0].rewardIcon)
            assertEquals(50, captured[0].pointCost)
        }
}
