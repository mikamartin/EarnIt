package com.earnit.app

import com.earnit.app.data.CompletionLogEntity
import com.earnit.app.data.EarnItExport
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

    @Test
    fun `fromJson with empty JSON object returns empty export`() {
        val result = JsonExport.fromJson("{}")

        assertEquals(0, result.tasks.size)
        assertEquals(0, result.rewards.size)
        assertEquals(0, result.completionLogs.size)
        assertEquals(0, result.rewardTaskCrossRefs.size)
        assertEquals(0, result.historyEntries.size)
    }
}
