package com.earnit.app

import com.earnit.app.data.CompletionLogDao
import com.earnit.app.data.EarnItDatabase
import com.earnit.app.data.EarnItRepository
import com.earnit.app.data.HistoryDao
import com.earnit.app.data.RewardDao
import com.earnit.app.data.RewardEntity
import com.earnit.app.data.RewardTaskCrossRefDao
import com.earnit.app.data.TaskDao
import com.earnit.app.data.TaskEntity
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SortOrderTest {
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

    // ── upsertTask ────────────────────────────────────────────────────────────

    @Test
    fun `upsertTask new task gets sortOrder maxSortOrder plus 1`() =
        runBlocking {
            val inserted = mutableListOf<TaskEntity>()
            coEvery { taskDao.getMaxSortOrder() } returns 4
            coEvery { taskDao.insertTask(capture(inserted)) } returns 1L

            repository.upsertTask(TaskEntity(name = "Run"))

            assertEquals(5, inserted[0].sortOrder)
        }

    @Test
    fun `upsertTask new task when no tasks exist gets sortOrder 0`() =
        runBlocking {
            val inserted = mutableListOf<TaskEntity>()
            coEvery { taskDao.getMaxSortOrder() } returns null
            coEvery { taskDao.insertTask(capture(inserted)) } returns 1L

            repository.upsertTask(TaskEntity(name = "Run"))

            assertEquals(0, inserted[0].sortOrder)
        }

    @Test
    fun `upsertTask existing task calls updateTask not insertTask`() =
        runBlocking {
            val existing = TaskEntity(id = 7L, name = "Yoga", points = 5)
            coEvery { taskDao.updateTask(any()) } just Runs

            val returned = repository.upsertTask(existing)

            assertEquals(7L, returned)
            coVerify(exactly = 1) { taskDao.updateTask(existing) }
            coVerify(exactly = 0) { taskDao.insertTask(any()) }
        }

    // ── upsertReward ──────────────────────────────────────────────────────────

    @Test
    fun `upsertReward new reward gets sortOrder maxSortOrder plus 1`() =
        runBlocking {
            val inserted = mutableListOf<RewardEntity>()
            coEvery { rewardDao.getMaxSortOrder() } returns 2
            coEvery { rewardDao.insertReward(capture(inserted)) } returns 1L

            repository.upsertReward(RewardEntity(name = "Trip", cost = 30))

            assertEquals(3, inserted[0].sortOrder)
        }

    @Test
    fun `upsertReward new reward when no rewards exist gets sortOrder 0`() =
        runBlocking {
            val inserted = mutableListOf<RewardEntity>()
            coEvery { rewardDao.getMaxSortOrder() } returns null
            coEvery { rewardDao.insertReward(capture(inserted)) } returns 1L

            repository.upsertReward(RewardEntity(name = "Trip", cost = 30))

            assertEquals(0, inserted[0].sortOrder)
        }

    // ── updateRewardsSortOrder / updateTasksSortOrder ─────────────────────────

    @Test
    fun `updateRewardsSortOrder assigns list index as sortOrder to each reward`() =
        runBlocking {
            coEvery { rewardDao.updateSortOrder(any(), any()) } just Runs

            repository.updateRewardsSortOrder(listOf(10L, 20L, 30L))

            coVerify { rewardDao.updateSortOrder(10L, 0) }
            coVerify { rewardDao.updateSortOrder(20L, 1) }
            coVerify { rewardDao.updateSortOrder(30L, 2) }
        }

    @Test
    fun `updateTasksSortOrder assigns list index as sortOrder to each task`() =
        runBlocking {
            coEvery { taskDao.updateSortOrder(any(), any()) } just Runs

            repository.updateTasksSortOrder(listOf(5L, 6L))

            coVerify { taskDao.updateSortOrder(5L, 0) }
            coVerify { taskDao.updateSortOrder(6L, 1) }
        }
}
