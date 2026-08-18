package com.earnit.app.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.IOException

class ImportFileTooLargeException : Exception()

class ImportWrongFileTypeException : Exception()

class ImportInvalidJsonException : Exception()

class ImportWrongSchemaException : Exception()

class ImportUnreadableException : Exception()

@JsonClass(generateAdapter = true)
data class EarnItExport(
    val tasks: List<TaskEntity> = emptyList(),
    val rewards: List<RewardEntity> = emptyList(),
    val rewardTaskCrossRefs: List<RewardTaskCrossRef> = emptyList(),
    val completionLogs: List<CompletionLogEntity> = emptyList(),
    val historyEntries: List<HistoryEntryEntity> = emptyList(),
)

object JsonExport {
    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(EarnItExport::class.java)
    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val mapAdapter = moshi.adapter<Map<String, Any?>>(mapType)
    private val earnItKeys = setOf("tasks", "rewards", "rewardTaskCrossRefs", "completionLogs", "historyEntries")

    fun toJson(export: EarnItExport): String = adapter.indent("  ").toJson(export)

    fun fromJson(json: String): EarnItExport {
        val topLevel =
            try {
                mapAdapter.fromJson(json)
            } catch (e: JsonDataException) {
                throw ImportWrongSchemaException()
            } catch (e: IOException) {
                throw ImportInvalidJsonException()
            }
        if (topLevel == null || !earnItKeys.all { topLevel.containsKey(it) }) throw ImportWrongSchemaException()

        return try {
            adapter.fromJson(json) ?: throw ImportWrongSchemaException()
        } catch (e: JsonDataException) {
            throw ImportWrongSchemaException()
        } catch (e: IOException) {
            throw ImportWrongSchemaException()
        }
    }
}
