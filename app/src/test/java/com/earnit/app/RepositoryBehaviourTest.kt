package com.earnit.app

import com.earnit.app.data.CompletionLogDao
import com.earnit.app.data.EarnItDatabase
import com.earnit.app.data.EarnItRepository
import com.earnit.app.data.HistoryDao
import com.earnit.app.data.HistoryEntryEntity
import com.earnit.app.data.RewardDao
import com.earnit.app.data.RewardEntity
import com.earnit.app.data.RewardTaskCrossRef
import com.earnit.app.data.RewardTaskCrossRefDao
import com.earnit.app.data.TaskDao
import com.earnit.app.data.TaskEntity
import com.earnit.app.data.TaskTemplate
import com.earnit.app.data.TemplateTask
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryBehaviourTest {
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

    // ── claimReward ───────────────────────────────────────────────────────────

    @Test
    fun `claimReward archives reward when startOver is false`() =
        runBlocking {
            val reward = RewardEntity(id = 1, name = "Trip", cost = 50)
            coEvery { rewardDao.getReward(1) } returns reward
            coEvery { historyDao.insertEntry(any()) } returns 99L
            coEvery { logDao.archiveLogsForReward(any(), any()) } just Runs
            coEvery { rewardDao.updateReward(any()) } just Runs

            repository.claimReward(1, startOver = false)

            coVerify { rewardDao.updateReward(match { it.isArchived }) }
        }

    @Test
    fun `claimReward does not archive reward when startOver is true`() =
        runBlocking {
            val reward = RewardEntity(id = 1, name = "Trip", cost = 50)
            coEvery { rewardDao.getReward(1) } returns reward
            coEvery { historyDao.insertEntry(any()) } returns 99L
            coEvery { logDao.archiveLogsForReward(any(), any()) } just Runs

            repository.claimReward(1, startOver = true)

            coVerify(exactly = 0) { rewardDao.updateReward(any()) }
        }

    @Test
    fun `claimReward does nothing when reward not found`() =
        runBlocking {
            coEvery { rewardDao.getReward(99) } returns null

            repository.claimReward(99, startOver = false)

            coVerify(exactly = 0) { historyDao.insertEntry(any()) }
        }

    // ── saveRewardTasks ───────────────────────────────────────────────────────

    @Test
    fun `saveRewardTasks inserts cross refs with correct flags`() =
        runBlocking {
            val inserted = mutableListOf<RewardTaskCrossRef>()
            coEvery { rewardTaskDao.clearTasksForReward(1) } just Runs
            coEvery { rewardTaskDao.insertCrossRef(capture(inserted)) } just Runs

            repository.saveRewardTasks(
                1,
                listOf(
                    Triple(10L, true, false),
                    Triple(20L, false, true),
                ),
            )

            assertEquals(2, inserted.size)
            assertTrue(inserted[0].isMandatory)
            assertFalse(inserted[0].isRepeatable)
            assertFalse(inserted[1].isMandatory)
            assertTrue(inserted[1].isRepeatable)
        }

    @Test
    fun `saveRewardTasks clears existing refs before inserting`() =
        runBlocking {
            coEvery { rewardTaskDao.clearTasksForReward(7) } just Runs
            coEvery { rewardTaskDao.insertCrossRef(any()) } just Runs

            repository.saveRewardTasks(7, listOf(Triple(1L, false, false)))

            coVerify(exactly = 1) { rewardTaskDao.clearTasksForReward(7) }
        }

    // ── copyRewardFromEntry ───────────────────────────────────────────────────

    @Test
    fun `copyRewardFromEntry preserves isMandatory and isRepeatable flags`() =
        runBlocking {
            val entry = HistoryEntryEntity(id = 5, rewardId = 1, rewardName = "Trip", pointCost = 50, claimedAt = 0L)
            val originalRefs =
                listOf(
                    RewardTaskCrossRef(rewardId = 1, taskId = 10, isMandatory = true, isRepeatable = false),
                    RewardTaskCrossRef(rewardId = 1, taskId = 20, isMandatory = false, isRepeatable = true),
                )
            val inserted = mutableListOf<RewardTaskCrossRef>()
            coEvery { historyDao.getAllEntries() } returns listOf(entry)
            coEvery { rewardTaskDao.getTaskRefsForReward(1) } returns originalRefs
            coEvery { rewardDao.getMaxSortOrder() } returns null
            coEvery { rewardDao.insertReward(any()) } returns 99L
            coEvery { rewardTaskDao.insertCrossRef(capture(inserted)) } just Runs

            repository.copyRewardFromEntry(5)

            assertEquals(2, inserted.size)
            assertTrue(inserted[0].isMandatory)
            assertFalse(inserted[0].isRepeatable)
            assertFalse(inserted[1].isMandatory)
            assertTrue(inserted[1].isRepeatable)
        }

    @Test
    fun `copyRewardFromEntry copies icon and appends reward to end of list`() =
        runBlocking {
            val entry =
                HistoryEntryEntity(
                    id = 3,
                    rewardId = 1,
                    rewardName = "Spa Day",
                    rewardIcon = "🛁",
                    pointCost = 30,
                    claimedAt = 0L,
                )
            val capturedReward = slot<RewardEntity>()
            coEvery { historyDao.getAllEntries() } returns listOf(entry)
            coEvery { rewardTaskDao.getTaskRefsForReward(1) } returns emptyList()
            coEvery { rewardDao.getMaxSortOrder() } returns 4
            coEvery { rewardDao.insertReward(capture(capturedReward)) } returns 99L

            repository.copyRewardFromEntry(3)

            assertEquals("🛁", capturedReward.captured.icon)
            assertEquals(5, capturedReward.captured.sortOrder) // maxOrder(4) + 1
            assertTrue(capturedReward.captured.createdAt > 0L)
        }

    // ── importTemplate ────────────────────────────────────────────────────────

    @Test
    fun `importTemplate append adds tasks without clearing existing ones`() =
        runBlocking {
            val inserted = mutableListOf<TaskEntity>()
            coEvery { taskDao.getAllTasks() } returns emptyList()
            coEvery { taskDao.getMaxSortOrder() } returns 2
            coEvery { taskDao.insertTask(capture(inserted)) } returnsMany listOf(10L, 11L)

            val template =
                TaskTemplate(
                    "Set",
                    "🧪",
                    listOf(
                        TemplateTask("Task A", 3, "🅰️"),
                        TemplateTask("Task B", 5, "🅱️"),
                    ),
                )
            repository.importTemplate(template, cleanSlate = false)

            coVerify(exactly = 0) { taskDao.deleteAllTasks() }
            assertEquals(2, inserted.size)
            assertEquals("Task A", inserted[0].name)
            assertEquals(4, inserted[1].sortOrder) // maxOrder(2) + 1 + index(1)
            assertEquals("Set", inserted[0].group)
            assertEquals("Set", inserted[1].group)
        }

    @Test
    fun `importTemplate cleanSlate deletes tasks and cross refs before inserting`() =
        runBlocking {
            coEvery { rewardTaskDao.deleteAll() } just Runs
            coEvery { taskDao.deleteAllTasks() } just Runs
            coEvery { taskDao.getAllTasks() } returns emptyList()
            coEvery { taskDao.getMaxSortOrder() } returns null
            coEvery { taskDao.insertTask(any()) } returns 1L

            val template = TaskTemplate("Set", "🧪", listOf(TemplateTask("Task A", 3, "🅰️")))
            repository.importTemplate(template, cleanSlate = true)

            coVerify(exactly = 1) { taskDao.deleteAllTasks() }
            coVerify(exactly = 1) { rewardTaskDao.deleteAll() }
        }

    @Test
    fun `importTemplate sets sortOrder sequentially from max`() =
        runBlocking {
            val inserted = mutableListOf<TaskEntity>()
            coEvery { taskDao.getAllTasks() } returns emptyList()
            coEvery { taskDao.getMaxSortOrder() } returns 4
            coEvery { taskDao.insertTask(capture(inserted)) } returnsMany listOf(1L, 2L, 3L)

            val template =
                TaskTemplate(
                    "Set",
                    "🧪",
                    listOf(
                        TemplateTask("A", 1, ""),
                        TemplateTask("B", 1, ""),
                        TemplateTask("C", 1, ""),
                    ),
                )
            repository.importTemplate(template, cleanSlate = false)

            assertEquals(5, inserted[0].sortOrder)
            assertEquals(6, inserted[1].sortOrder)
            assertEquals(7, inserted[2].sortOrder)
            inserted.forEach { assertEquals("Set", it.group) }
        }

    // ── updateTaskRewards ─────────────────────────────────────────────────────

    @Test
    fun `updateTaskRewards removes cross refs no longer linked`() =
        runBlocking {
            val existing =
                listOf(
                    RewardTaskCrossRef(rewardId = 1L, taskId = 10L, isMandatory = false, isRepeatable = false),
                    RewardTaskCrossRef(rewardId = 2L, taskId = 10L, isMandatory = false, isRepeatable = false),
                )
            coEvery { rewardTaskDao.getAllCrossRefs() } returns existing
            coEvery { rewardTaskDao.deleteCrossRef(any(), any()) } just Runs
            coEvery { rewardTaskDao.insertCrossRef(any()) } just Runs

            repository.updateTaskRewards(10L, mapOf(1L to Pair(true, false)))

            coVerify(exactly = 1) { rewardTaskDao.deleteCrossRef(2L, 10L) }
            coVerify(exactly = 0) { rewardTaskDao.deleteCrossRef(1L, 10L) }
        }

    @Test
    fun `updateTaskRewards inserts newly linked rewards with correct flags`() =
        runBlocking {
            coEvery { rewardTaskDao.getAllCrossRefs() } returns emptyList()
            val inserted = mutableListOf<RewardTaskCrossRef>()
            coEvery { rewardTaskDao.insertCrossRef(capture(inserted)) } just Runs

            repository.updateTaskRewards(
                10L,
                mapOf(
                    5L to Pair(true, false),
                    6L to Pair(false, true),
                ),
            )

            assertEquals(2, inserted.size)
            assertTrue(inserted.any { it.rewardId == 5L && it.isMandatory && !it.isRepeatable })
            assertTrue(inserted.any { it.rewardId == 6L && !it.isMandatory && it.isRepeatable })
        }
}
