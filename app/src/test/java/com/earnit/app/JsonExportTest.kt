package com.earnit.app

import com.earnit.app.data.CompletionLogEntity
import com.earnit.app.data.EarnItExport
import com.earnit.app.data.HistoryEntryEntity
import com.earnit.app.data.ImportWrongSchemaException
import com.earnit.app.data.JsonExport
import com.earnit.app.data.RewardEntity
import com.earnit.app.data.RewardTaskCrossRef
import com.earnit.app.data.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonExportTest {
    @Test
    fun `toJson produces non-empty string containing task and reward names`() {
        val export =
            EarnItExport(
                tasks = listOf(TaskEntity(id = 1, name = "Run", points = 5)),
                rewards = listOf(RewardEntity(id = 1, name = "Coffee", cost = 10)),
            )
        val json = JsonExport.toJson(export)

        assertTrue(json.isNotBlank())
        assertTrue(json.contains("Run"))
        assertTrue(json.contains("Coffee"))
    }

    @Test
    fun `fromJson round-trips task fields correctly`() {
        val original =
            EarnItExport(
                tasks =
                    listOf(
                        TaskEntity(
                            id = 7,
                            name = "Yoga",
                            points = 3,
                            useAutoPoints = true,
                            time = 2,
                            difficulty = 3,
                            preparation = 1,
                            icon = "🧘",
                            repeatable = false,
                        ),
                    ),
            )
        val result = JsonExport.fromJson(JsonExport.toJson(original))

        assertEquals(1, result.tasks.size)
        with(result.tasks[0]) {
            assertEquals(7L, id)
            assertEquals("Yoga", name)
            assertEquals(3, points)
            assertTrue(useAutoPoints)
            assertEquals(2, time)
            assertEquals(3, difficulty)
            assertEquals(1, preparation)
            assertEquals("🧘", icon)
            assertFalse(repeatable)
        }
    }

    @Test
    fun `fromJson round-trips reward fields correctly`() {
        val original =
            EarnItExport(
                rewards =
                    listOf(
                        RewardEntity(
                            id = 3,
                            name = "Beach Trip",
                            cost = 50,
                            isArchived = false,
                            description = "Go to the beach",
                            icon = "🏖️",
                        ),
                    ),
            )
        val result = JsonExport.fromJson(JsonExport.toJson(original))

        assertEquals(1, result.rewards.size)
        with(result.rewards[0]) {
            assertEquals(3L, id)
            assertEquals("Beach Trip", name)
            assertEquals(50, cost)
            assertEquals("Go to the beach", description)
            assertEquals("🏖️", icon)
        }
    }

    @Test
    fun `fromJson round-trips cross refs and completion logs`() {
        val original =
            EarnItExport(
                rewardTaskCrossRefs =
                    listOf(
                        RewardTaskCrossRef(rewardId = 1, taskId = 2, isMandatory = true, isRepeatable = false),
                    ),
                completionLogs =
                    listOf(
                        CompletionLogEntity(
                            id = 5,
                            taskId = 2,
                            taskName = "Run",
                            rewardId = 1,
                            timestamp = 1000L,
                            detail = "felt great",
                            points = 7,
                            historyEntryId = null,
                        ),
                    ),
            )
        val result = JsonExport.fromJson(JsonExport.toJson(original))

        assertEquals(1, result.rewardTaskCrossRefs.size)
        assertTrue(result.rewardTaskCrossRefs[0].isMandatory)
        assertFalse(result.rewardTaskCrossRefs[0].isRepeatable)

        assertEquals(1, result.completionLogs.size)
        assertEquals("felt great", result.completionLogs[0].detail)
        assertEquals(7, result.completionLogs[0].points)
        assertNull(result.completionLogs[0].historyEntryId)
    }

    @Test(expected = ImportWrongSchemaException::class)
    fun `fromJson with empty JSON object throws WrongSchemaException`() {
        JsonExport.fromJson("{}")
    }

    @Test
    fun `toJson emits the exact top-level and per-entity key names`() {
        val export =
            EarnItExport(
                tasks = listOf(TaskEntity(id = 1, name = "Run")),
                rewards = listOf(RewardEntity(id = 1, name = "Coffee", cost = 10)),
                rewardTaskCrossRefs = listOf(RewardTaskCrossRef(rewardId = 1, taskId = 1)),
                completionLogs =
                    listOf(
                        CompletionLogEntity(id = 1, taskId = 1, rewardId = 1, timestamp = 0, points = 1, historyEntryId = 1),
                    ),
                historyEntries = listOf(HistoryEntryEntity(id = 1, rewardId = 1, rewardName = "Coffee", pointCost = 10, claimedAt = 0)),
            )
        val json = JsonExport.toJson(export)

        // Top-level keys.
        listOf("tasks", "rewards", "rewardTaskCrossRefs", "completionLogs", "historyEntries")
            .forEach { key -> assertTrue("missing top-level key \"$key\"", json.contains("\"$key\":")) }

        // Per-entity keys. RewardTaskCrossRef is the one that falls outside proguard's
        // *Entity keep-rule glob and had no @JsonClass adapter — pin its keys explicitly
        // to catch a regression that R8 could silently mangle in the release build.
        listOf("id", "name", "repeatable", "points", "useAutoPoints", "time", "difficulty", "preparation", "icon", "sortOrder")
            .forEach { key -> assertTrue("missing TaskEntity key \"$key\"", json.contains("\"$key\":")) }
        listOf("cost", "isArchived", "createdAt", "description")
            .forEach { key -> assertTrue("missing RewardEntity key \"$key\"", json.contains("\"$key\":")) }
        listOf("rewardId", "taskId", "isMandatory", "isRepeatable")
            .forEach { key -> assertTrue("missing RewardTaskCrossRef key \"$key\"", json.contains("\"$key\":")) }
        listOf("taskName", "detail", "historyEntryId")
            .forEach { key -> assertTrue("missing CompletionLogEntity key \"$key\"", json.contains("\"$key\":")) }
        listOf("rewardName", "rewardIcon", "pointCost", "claimedAt")
            .forEach { key -> assertTrue("missing HistoryEntryEntity key \"$key\"", json.contains("\"$key\":")) }
    }
}
