package com.earnit.app

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earnit.app.data.ImportInvalidJsonException
import com.earnit.app.data.ImportWrongSchemaException
import com.earnit.app.data.RewardEntity
import com.earnit.app.data.TaskEntity
import com.earnit.app.tags.ImportExport
import com.earnit.app.tags.RepositoryTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Verifies the JSON export / import round-trip.
 * Uses a real in-memory Room database; no mocks.
 */
@RepositoryTest
@ImportExport
@RunWith(AndroidJUnit4::class)
class ExportImportTest : RoomIntegrationBase() {
    @Test
    fun exportImportReplace_roundTripsTasksRewardsLinksAndLogs() =
        runBlocking {
            val taskId = repository.upsertTask(TaskEntity(name = "Run", points = 5, icon = "🏃"))
            val rewardId = repository.upsertReward(RewardEntity(name = "Trip", cost = 30, description = "Beach"))
            repository.saveRewardTasks(rewardId, listOf(Triple(taskId, true, false)))
            repository.logCompletion(repository.getTaskOrNull(taskId)!!, rewardId, detail = "good run")

            val json = repository.exportToJson()
            assertTrue("Exported JSON should be non-empty", json.isNotBlank())

            repository.clearAll()
            val emptyState = repository.observeUiState().first()
            assertEquals(0, emptyState.tasks.size)

            repository.importFromJson(json, replace = true)

            val state = repository.observeUiState().first()
            assertEquals(1, state.tasks.size)
            assertEquals("Run", state.tasks[0].name)
            assertEquals(1, state.rewardProgressList.size)
            assertEquals("Trip", state.rewardProgressList[0].reward.name)
            // Task link (cross ref) preserved
            assertEquals(1, state.rewardProgressList[0].allTasks.size)
            assertTrue("Linked task should be mandatory", state.rewardProgressList[0].mandatoryTasks.isNotEmpty())
            // Active log preserved
            assertEquals(1, state.allLogs.size)
            assertEquals("good run", state.allLogs[0].detail)
        }

    @Test
    fun exportImportReplace_preservesHistoryEntriesWithArchivedLogs() =
        runBlocking {
            val taskId = repository.upsertTask(TaskEntity(name = "Yoga", points = 10))
            val rewardId = repository.upsertReward(RewardEntity(name = "Treat", cost = 10))
            repository.logCompletion(repository.getTaskOrNull(taskId)!!, rewardId, detail = "morning")
            repository.claimReward(rewardId, startOver = false)

            val json = repository.exportToJson()
            repository.clearAll()
            repository.importFromJson(json, replace = true)

            val state = repository.observeUiState().first()
            assertEquals(1, state.historyEntries.size)
            assertEquals("Treat", state.historyEntries[0].entry.rewardName)
            assertEquals(10, state.historyEntries[0].entry.pointCost)
            assertEquals(1, state.historyEntries[0].logs.size)
            assertEquals("morning", state.historyEntries[0].logs[0].detail)
        }

    @Test
    fun exportToFile_thenImportFromFile_replace_roundTripsData() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val taskId = repository.upsertTask(TaskEntity(name = "Swim", points = 8))
            val rewardId = repository.upsertReward(RewardEntity(name = "Spa", cost = 20))
            repository.logCompletion(repository.getTaskOrNull(taskId)!!, rewardId, detail = "morning")

            val file = File(context.filesDir, "test_backup.json")
            val uri = Uri.fromFile(file)

            repository.exportToFile(context, uri)
            assertTrue("Backup file should exist after export", file.exists())
            assertTrue("Backup file should not be empty", file.length() > 0)

            repository.clearAll()
            assertEquals(
                0,
                repository
                    .observeUiState()
                    .first()
                    .tasks.size,
            )

            repository.importFromFile(context, uri, replace = true)

            val state = repository.observeUiState().first()
            assertEquals(1, state.tasks.size)
            assertEquals("Swim", state.tasks[0].name)
            assertEquals(1, state.rewardProgressList.size)
            assertEquals("Spa", state.rewardProgressList[0].reward.name)
            assertEquals(1, state.allLogs.size)
            assertEquals("morning", state.allLogs[0].detail)

            file.delete()
            Unit
        }

    @Test
    fun exportToFile_thenImportFromFile_merge_preservesExistingData() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val existingId = repository.upsertTask(TaskEntity(name = "Cycle", points = 6))
            val file = File(context.filesDir, "test_backup_merge.json")
            val uri = Uri.fromFile(file)

            repository.exportToFile(context, uri)

            repository.upsertTask(TaskEntity(name = "Hike", points = 4))
            repository.importFromFile(context, uri, replace = false)

            val tasks = repository.observeUiState().first().tasks
            assertEquals("Merge should keep both tasks", 2, tasks.size)
            assertTrue(tasks.any { it.id == existingId && it.name == "Cycle" })
            assertTrue(tasks.any { it.name == "Hike" })

            file.delete()
            Unit
        }

    @Test(expected = ImportInvalidJsonException::class)
    fun importFromJson_withMalformedJson_throwsInvalidJsonException() =
        runBlocking {
            repository.importFromJson("{\"tasks\": [broken json here}", replace = false)
        }

    @Test(expected = ImportWrongSchemaException::class)
    fun importFromJson_withWrongSchemaJson_throwsWrongSchemaException() =
        runBlocking {
            repository.importFromJson("{\"name\": \"John\", \"age\": 30}", replace = false)
        }

    @Test(expected = ImportInvalidJsonException::class)
    fun importFromFile_withInvalidJsonContent_throwsInvalidJsonException() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val file = File(context.filesDir, "invalid.json")
            file.writeText("{\"tasks\": [broken json here}")
            val uri = Uri.fromFile(file)
            try {
                repository.importFromFile(context, uri, replace = false)
            } finally {
                file.delete()
            }
        }

    @Test(expected = ImportWrongSchemaException::class)
    fun importFromFile_withWrongSchemaJsonContent_throwsWrongSchemaException() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val file = File(context.filesDir, "wrong_schema.json")
            file.writeText("{\"name\": \"John\", \"city\": \"Helsinki\"}")
            val uri = Uri.fromFile(file)
            try {
                repository.importFromFile(context, uri, replace = false)
            } finally {
                file.delete()
            }
        }

    @Test
    fun importReplace_withWrongSchema_doesNotWipeExistingData() =
        runBlocking {
            repository.upsertTask(TaskEntity(name = "Existing", points = 5))
            try {
                repository.importFromJson("{\"name\": \"not earnit\"}", replace = true)
            } catch (_: ImportWrongSchemaException) {
            }
            val tasks = repository.observeUiState().first().tasks
            assertEquals("Existing data must survive a wrong-schema replace attempt", 1, tasks.size)
            assertEquals("Existing", tasks[0].name)
        }

    @Test
    fun importMerge_preservesExistingRecords_andAddsNewOnes() =
        runBlocking {
            // Seed one task and export it
            val existingId = repository.upsertTask(TaskEntity(name = "Original", points = 3))
            val json = repository.exportToJson()

            // Add a second task that is NOT in the export
            val newId = repository.upsertTask(TaskEntity(name = "New Task", points = 5))

            // Merge import — "Original" should not be duplicated; "New Task" should survive
            repository.importFromJson(json, replace = false)

            val tasks = repository.observeUiState().first().tasks
            assertEquals("Should have exactly 2 tasks after merge", 2, tasks.size)
            assertTrue(tasks.any { it.id == existingId && it.name == "Original" })
            assertTrue(tasks.any { it.id == newId && it.name == "New Task" })
        }
}
