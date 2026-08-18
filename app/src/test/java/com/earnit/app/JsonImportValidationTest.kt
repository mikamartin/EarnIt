package com.earnit.app

import com.earnit.app.data.EarnItExport
import com.earnit.app.data.ImportInvalidJsonException
import com.earnit.app.data.ImportWrongSchemaException
import com.earnit.app.data.JsonExport
import com.earnit.app.data.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class JsonImportValidationTest {
    @Test(expected = ImportInvalidJsonException::class)
    fun `fromJson with malformed JSON containing EarnIt key throws InvalidJsonException`() {
        JsonExport.fromJson("{\"tasks\": [broken json here}")
    }

    @Test(expected = ImportInvalidJsonException::class)
    fun `fromJson with truncated JSON throws InvalidJsonException`() {
        JsonExport.fromJson("{\"tasks\": [{\"id\": 1,")
    }

    @Test(expected = ImportWrongSchemaException::class)
    fun `fromJson with unrelated JSON object throws WrongSchemaException`() {
        JsonExport.fromJson("{\"name\": \"John\", \"age\": 30}")
    }

    @Test(expected = ImportWrongSchemaException::class)
    fun `fromJson with null JSON literal throws WrongSchemaException`() {
        JsonExport.fromJson("null")
    }

    @Test(expected = ImportWrongSchemaException::class)
    fun `fromJson with JSON array throws WrongSchemaException`() {
        JsonExport.fromJson("[1, 2, 3]")
    }

    @Test(expected = ImportWrongSchemaException::class)
    fun `fromJson with only tasks key present throws WrongSchemaException`() {
        JsonExport.fromJson("{\"tasks\": []}")
    }

    @Test(expected = ImportWrongSchemaException::class)
    fun `fromJson with foreign object reusing one EarnIt key throws WrongSchemaException`() {
        JsonExport.fromJson("{\"tasks\":[],\"projects\":[{\"title\":\"x\"}]}")
    }

    @Test(expected = ImportWrongSchemaException::class)
    fun `fromJson with all keys present but wrong element shape throws WrongSchemaException`() {
        JsonExport.fromJson(
            "{\"tasks\":[\"x\"],\"rewards\":[],\"rewardTaskCrossRefs\":[]," +
                "\"completionLogs\":[],\"historyEntries\":[]}",
        )
    }

    @Test
    fun `fromJson with valid EarnIt export round-trips correctly`() {
        val original = EarnItExport(tasks = listOf(TaskEntity(id = 1, name = "Walk", points = 5)))
        val result = JsonExport.fromJson(JsonExport.toJson(original))
        assertEquals(1, result.tasks.size)
        assertEquals("Walk", result.tasks[0].name)
    }

    @Test
    fun `fromJson with all empty lists succeeds`() {
        val json = JsonExport.toJson(EarnItExport())
        val result = JsonExport.fromJson(json)
        assertEquals(0, result.tasks.size)
        assertEquals(0, result.rewards.size)
        assertEquals(0, result.completionLogs.size)
    }
}
