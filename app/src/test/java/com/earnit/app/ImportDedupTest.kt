package com.earnit.app

import com.earnit.app.data.EarnItDatabase
import com.earnit.app.data.EarnItRepository
import com.earnit.app.data.RewardTaskCrossRefDao
import com.earnit.app.data.TaskDao
import com.earnit.app.data.TaskEntity
import com.earnit.app.data.TaskTemplate
import com.earnit.app.data.TemplateTask
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportDedupTest {
    private val database = mockk<EarnItDatabase>(relaxed = true)
    private val taskDao = mockk<TaskDao>()
    private val rewardTaskDao = mockk<RewardTaskCrossRefDao>()

    init {
        every { database.taskDao() } returns taskDao
        every { database.rewardTaskCrossRefDao() } returns rewardTaskDao
    }

    private val repository = EarnItRepository(database)

    private fun template(vararg names: String) =
        TaskTemplate(
            name = "Set",
            icon = "🧪",
            tasks = names.map { TemplateTask(it, points = 3, icon = "") },
        )

    private fun existing(vararg names: String) = names.map { TaskEntity(name = it, points = 3) }

    @Test
    fun `importTemplate skips task with exact name match`() =
        runBlocking {
            coEvery { taskDao.getAllTasks() } returns existing("Morning Run")
            coEvery { taskDao.getMaxSortOrder() } returns 0

            val skipped = repository.importTemplate(template("Morning Run"), cleanSlate = false)

            assertEquals(listOf("Morning Run"), skipped)
            coVerify(exactly = 0) { taskDao.insertTask(any()) }
        }

    @Test
    fun `importTemplate skips task case-insensitively`() =
        runBlocking {
            coEvery { taskDao.getAllTasks() } returns existing("morning run")
            coEvery { taskDao.getMaxSortOrder() } returns 0

            val skipped = repository.importTemplate(template("Morning Run"), cleanSlate = false)

            assertEquals(listOf("Morning Run"), skipped)
            coVerify(exactly = 0) { taskDao.insertTask(any()) }
        }

    @Test
    fun `importTemplate skips task after trimming whitespace`() =
        runBlocking {
            coEvery { taskDao.getAllTasks() } returns existing("Morning Run")
            coEvery { taskDao.getMaxSortOrder() } returns 0

            val skipped = repository.importTemplate(template("  Morning Run  "), cleanSlate = false)

            assertEquals(listOf("  Morning Run  "), skipped)
            coVerify(exactly = 0) { taskDao.insertTask(any()) }
        }

    @Test
    fun `importTemplate inserts non-conflicting task`() =
        runBlocking {
            coEvery { taskDao.getAllTasks() } returns existing("Yoga")
            coEvery { taskDao.getMaxSortOrder() } returns 0
            coEvery { taskDao.insertTask(any()) } returns 1L

            val skipped = repository.importTemplate(template("Morning Run"), cleanSlate = false)

            assertTrue(skipped.isEmpty())
            coVerify(exactly = 1) { taskDao.insertTask(match { it.name == "Morning Run" }) }
        }

    @Test
    fun `importTemplate splits mixed list into inserted and skipped correctly`() =
        runBlocking {
            coEvery { taskDao.getAllTasks() } returns existing("Morning Run")
            coEvery { taskDao.getMaxSortOrder() } returns 0
            coEvery { taskDao.insertTask(any()) } returns 1L

            val skipped =
                repository.importTemplate(
                    template("Morning Run", "Evening Walk", "MORNING RUN"),
                    cleanSlate = false,
                )

            assertEquals(listOf("Morning Run", "MORNING RUN"), skipped)
            coVerify(exactly = 1) { taskDao.insertTask(match { it.name == "Evening Walk" }) }
        }

    @Test
    fun `importTemplate sort order is sequential for inserted tasks ignoring skips`() =
        runBlocking {
            val inserted = mutableListOf<TaskEntity>()
            coEvery { taskDao.getAllTasks() } returns existing("Conflict")
            coEvery { taskDao.getMaxSortOrder() } returns 4
            coEvery { taskDao.insertTask(capture(inserted)) } returnsMany listOf(1L, 2L)

            repository.importTemplate(
                template("Conflict", "Task A", "Task B"),
                cleanSlate = false,
            )

            assertEquals(2, inserted.size)
            assertEquals(5, inserted[0].sortOrder)
            assertEquals(6, inserted[1].sortOrder)
        }

    @Test
    fun `importTemplate skipped list preserves template casing not existing task casing`() =
        runBlocking {
            coEvery { taskDao.getAllTasks() } returns existing("morning run")
            coEvery { taskDao.getMaxSortOrder() } returns 0

            val skipped = repository.importTemplate(template("MORNING RUN"), cleanSlate = false)

            assertEquals("MORNING RUN", skipped[0])
        }
}
