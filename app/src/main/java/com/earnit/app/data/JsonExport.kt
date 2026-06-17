package com.earnit.app.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

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

    fun toJson(export: EarnItExport): String = adapter.indent("  ").toJson(export)

    fun fromJson(json: String): EarnItExport = adapter.fromJson(json) ?: EarnItExport()
}
