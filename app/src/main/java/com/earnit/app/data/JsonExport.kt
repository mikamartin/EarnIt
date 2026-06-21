package com.earnit.app.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(EarnItExport::class.java)
    private val earnItKeys = setOf("tasks", "rewards", "rewardTaskCrossRefs", "completionLogs", "historyEntries")

    fun toJson(export: EarnItExport): String = adapter.indent("  ").toJson(export)

    fun fromJson(json: String): EarnItExport {
        if (earnItKeys.none { key -> json.contains("\"$key\"") }) throw ImportWrongSchemaException()
        return try {
            adapter.fromJson(json) ?: throw ImportWrongSchemaException()
        } catch (e: JsonDataException) {
            throw ImportInvalidJsonException()
        } catch (e: IOException) {
            throw ImportInvalidJsonException()
        }
    }
}
