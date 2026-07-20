package com.earnit.app.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val repeatable: Boolean = true,
    val points: Int = 4,
    val useAutoPoints: Boolean = false,
    val time: Int = 1,
    val difficulty: Int = 1,
    val preparation: Int = 1,
    val icon: String = "",
    val sortOrder: Int = 0,
    val group: String? = null,
) {
    fun computeAutoPoints(): Int = computeAutoPoints(time, difficulty, preparation)

    fun effectivePoints(): Int = if (useAutoPoints) computeAutoPoints() else points

    companion object {
        fun computeAutoPoints(
            time: Int,
            difficulty: Int,
            preparation: Int,
        ): Int {
            val base = ((time + 1) * (difficulty + 1) * (preparation + 1) + 7) / 8
            val bonus = if (maxOf(time, difficulty, preparation) == 5) 3 else 0
            return base + bonus
        }
    }
}

@Entity(tableName = "completion_logs")
data class CompletionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val taskName: String = "",
    val rewardId: Long,
    val timestamp: Long,
    val detail: String = "",
    val points: Int,
    val historyEntryId: Long? = null, // set when claimed, null = active
)

@Entity(tableName = "rewards")
data class RewardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val cost: Int,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = 0L,
    val description: String = "",
    val icon: String = "",
)

@Entity(
    primaryKeys = ["rewardId", "taskId"],
    tableName = "reward_task_cross_ref",
    indices = [Index(value = ["taskId"]), Index(value = ["rewardId"])],
    // A cross-ref with no reward or no task is always a bug, never a feature (unlike
    // CompletionLogEntity/HistoryEntryEntity, which deliberately snapshot names so they
    // survive their source task/reward being deleted) — safe to enforce with cascade.
    foreignKeys = [
        ForeignKey(
            entity = RewardEntity::class,
            parentColumns = ["id"],
            childColumns = ["rewardId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class RewardTaskCrossRef(
    val rewardId: Long,
    val taskId: Long,
    val isMandatory: Boolean = false,
    val isRepeatable: Boolean = false,
)

@Entity(tableName = "history_entries")
data class HistoryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rewardId: Long,
    val rewardName: String,
    val rewardIcon: String = "",
    val pointCost: Int,
    val claimedAt: Long,
)

// ── POJOs ─────────────────────────────────────────────────────────────────────

data class HistoryEntryWithLogs(
    @Embedded val entry: HistoryEntryEntity,
    @Relation(parentColumn = "id", entityColumn = "historyEntryId")
    var logs: List<CompletionLogEntity> = emptyList(),
)

// Computed in repository — not a Room entity
data class RewardProgress(
    val reward: RewardEntity,
    val taskRefs: List<RewardTaskCrossRef>,
    val mandatoryTasks: List<TaskEntity>,
    val optionalTasks: List<TaskEntity>,
    val activeLogs: List<CompletionLogEntity>,
) {
    val totalPoints: Int get() = activeLogs.sumOf { it.points }
    val canClaim: Boolean get() =
        totalPoints >= reward.cost &&
            mandatoryTasks.all { mt -> activeLogs.any { it.taskId == mt.id } }
    val showsProgressNumbers: Boolean get() = !canClaim && totalPoints < reward.cost
    val allTasks: List<TaskEntity> get() = mandatoryTasks + optionalTasks
    val loggableTasks: List<TaskEntity> get() {
        val completedIds = activeLogs.map { it.taskId }.toSet()
        val refsById = taskRefs.associateBy { it.taskId }
        return allTasks.filter { task ->
            refsById[task.id]?.isRepeatable == true || task.id !in completedIds
        }
    }
}
