package com.earnit.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class EarnItRepository
    @Inject
    constructor(
        private val database: EarnItDatabase,
    ) {
        private val taskDao = database.taskDao()
        private val logDao = database.completionLogDao()
        private val rewardDao = database.rewardDao()
        private val rewardTaskDao = database.rewardTaskCrossRefDao()
        private val historyDao = database.historyDao()

        fun observeUiState(): Flow<EarnItUiState> =
            combine(
                rewardDao.observeActiveRewards(),
                rewardTaskDao.observeAllCrossRefs(),
                taskDao.observeTasks(),
                logDao.observeLogs(),
                historyDao.observeEntriesWithLogs(),
            ) { rewards, crossRefs, tasks, logs, hallEntries ->
                val activeLogs = logs.filter { it.historyEntryId == null }
                val taskMap = tasks.associateBy { it.id }

                val rewardProgressList =
                    rewards.map { reward ->
                        val refs = crossRefs.filter { it.rewardId == reward.id }
                        val mandatory = refs.filter { it.isMandatory }.mapNotNull { taskMap[it.taskId] }
                        val optional = refs.filter { !it.isMandatory }.mapNotNull { taskMap[it.taskId] }
                        val logsForReward = activeLogs.filter { it.rewardId == reward.id }
                        RewardProgress(reward, refs, mandatory, optional, logsForReward)
                    }

                EarnItUiState(
                    rewardProgressList = rewardProgressList,
                    tasks = tasks,
                    historyEntries = hallEntries,
                    allLogs = logs,
                )
            }

        // ── Tasks ─────────────────────────────────────────────────────────────────

        suspend fun upsertTask(task: TaskEntity): Long {
            if (task.id == 0L) {
                val maxOrder = taskDao.getMaxSortOrder() ?: -1
                return taskDao.insertTask(task.copy(sortOrder = maxOrder + 1))
            }
            taskDao.updateTask(task)
            return task.id
        }

        suspend fun getTaskOrNull(id: Long) = taskDao.getTask(id)

        // Cross-ref rows for this task are removed by the FK cascade on RewardTaskCrossRef.taskId.
        suspend fun deleteTask(taskId: Long) = taskDao.deleteTask(taskId)

        // ── Rewards ───────────────────────────────────────────────────────────────

        suspend fun upsertReward(reward: RewardEntity): Long {
            if (reward.id == 0L) {
                val maxOrder = rewardDao.getMaxSortOrder() ?: -1
                return rewardDao.insertReward(
                    reward.copy(createdAt = System.currentTimeMillis(), sortOrder = maxOrder + 1),
                )
            }
            rewardDao.updateReward(reward)
            return reward.id
        }

        suspend fun getRewardOrNull(id: Long) = rewardDao.getReward(id)

        // Cross-ref rows for this reward are removed by the FK cascade on RewardTaskCrossRef.rewardId.
        suspend fun deleteReward(rewardId: Long) =
            database.withTransaction {
                logDao.deleteActiveLogsForReward(rewardId)
                rewardDao.deleteReward(rewardId)
            }

        suspend fun saveRewardTasks(
            rewardId: Long,
            tasks: List<Triple<Long, Boolean, Boolean>>,
        ) = database.withTransaction {
            rewardTaskDao.clearTasksForReward(rewardId)
            tasks.forEach { (taskId, isMandatory, isRepeatable) ->
                rewardTaskDao.insertCrossRef(RewardTaskCrossRef(rewardId, taskId, isMandatory, isRepeatable))
            }
        }

        suspend fun addTaskToReward(
            rewardId: Long,
            taskId: Long,
            isMandatory: Boolean = false,
            isRepeatable: Boolean = false,
        ) {
            rewardTaskDao.insertCrossRef(RewardTaskCrossRef(rewardId, taskId, isMandatory, isRepeatable))
        }

        suspend fun updateTaskRewards(
            taskId: Long,
            rewardLinks: Map<Long, Pair<Boolean, Boolean>>,
        ) = database.withTransaction {
            val currentRewardIds =
                rewardTaskDao
                    .getAllCrossRefs()
                    .filter { it.taskId == taskId }
                    .map { it.rewardId }
                    .toSet()
            currentRewardIds.filter { it !in rewardLinks }.forEach { rewardId ->
                rewardTaskDao.deleteCrossRef(rewardId, taskId)
            }
            rewardLinks.forEach { (rewardId, flags) ->
                rewardTaskDao.insertCrossRef(RewardTaskCrossRef(rewardId, taskId, flags.first, flags.second))
            }
        }

        // ── Logging ───────────────────────────────────────────────────────────────

        suspend fun logCompletion(
            task: TaskEntity,
            rewardId: Long,
            detail: String,
        ) {
            val points = task.effectivePoints()
            logDao.insertLog(
                CompletionLogEntity(
                    taskId = task.id,
                    taskName = task.name,
                    rewardId = rewardId,
                    timestamp = System.currentTimeMillis(),
                    detail = detail,
                    points = points,
                ),
            )
        }

        // ── Claiming ──────────────────────────────────────────────────────────────

        suspend fun claimReward(
            rewardId: Long,
            startOver: Boolean,
        ) = database.withTransaction {
            val reward = rewardDao.getReward(rewardId) ?: return@withTransaction
            val entryId =
                historyDao.insertEntry(
                    HistoryEntryEntity(
                        rewardId = reward.id,
                        rewardName = reward.name,
                        rewardIcon = reward.icon,
                        pointCost = reward.cost,
                        claimedAt = System.currentTimeMillis(),
                    ),
                )
            logDao.archiveLogsForReward(rewardId, entryId)
            if (!startOver) {
                rewardDao.updateReward(reward.copy(isArchived = true))
            }
        }

        // ── History ───────────────────────────────────────────────────────────────

        suspend fun copyRewardFromEntry(entryId: Long) =
            database.withTransaction {
                val entry = historyDao.getAllEntries().find { it.id == entryId } ?: return@withTransaction
                val taskRefs = rewardTaskDao.getTaskRefsForReward(entry.rewardId)
                val newId =
                    upsertReward(
                        RewardEntity(name = entry.rewardName, cost = entry.pointCost, icon = entry.rewardIcon),
                    )
                taskRefs.forEach { ref ->
                    rewardTaskDao.insertCrossRef(
                        RewardTaskCrossRef(newId, ref.taskId, ref.isMandatory, ref.isRepeatable),
                    )
                }
            }

        suspend fun updateRewardsSortOrder(orderedIds: List<Long>) {
            orderedIds.forEachIndexed { index, rewardId ->
                rewardDao.updateSortOrder(rewardId, index)
            }
        }

        suspend fun updateTasksSortOrder(orderedIds: List<Long>) {
            orderedIds.forEachIndexed { index, taskId ->
                taskDao.updateSortOrder(taskId, index)
            }
        }

        // ── Export / Import ───────────────────────────────────────────────────────

        suspend fun exportToJson(): String {
            val export =
                EarnItExport(
                    tasks = taskDao.getAllTasks(),
                    rewards = rewardDao.getAllRewards(),
                    rewardTaskCrossRefs = rewardTaskDao.getAllCrossRefs(),
                    completionLogs = logDao.getAllLogs(),
                    historyEntries = historyDao.getAllEntries(),
                )
            return JsonExport.toJson(export)
        }

        suspend fun importFromJson(
            json: String,
            replace: Boolean,
        ) {
            val export = JsonExport.fromJson(json)
            database.withTransaction {
                if (replace) {
                    database.clearAllTables()
                    export.tasks.forEach { taskDao.insertTask(it) }
                    export.rewards.forEach { rewardDao.insertReward(it) }
                    export.rewardTaskCrossRefs.forEach { rewardTaskDao.insertCrossRef(it) }
                    export.completionLogs.forEach { logDao.insertLog(it) }
                    export.historyEntries.forEach { historyDao.insertEntry(it) }
                } else {
                    export.tasks.forEach { taskDao.insertTaskIgnore(it) }
                    export.rewards.forEach { rewardDao.insertRewardIgnore(it) }
                    export.rewardTaskCrossRefs.forEach { rewardTaskDao.insertCrossRefIgnore(it) }
                    export.completionLogs.forEach { logDao.insertLogIgnore(it) }
                    export.historyEntries.forEach { historyDao.insertEntryIgnore(it) }
                }
            }
        }

        suspend fun exportToFile(
            context: Context,
            uri: Uri,
        ) {
            val json = exportToJson()
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            }
        }

        suspend fun importFromFile(
            context: Context,
            uri: Uri,
            replace: Boolean,
        ) {
            val mimeType = context.contentResolver.getType(uri)
            if (mimeType != null && !isMimeTypeAllowed(mimeType)) throw ImportWrongFileTypeException()

            val size =
                context.contentResolver
                    .query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
                    ?.use { cursor ->
                        val col = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (cursor.moveToFirst() && col >= 0 && !cursor.isNull(col)) cursor.getLong(col) else null
                    }
            if (size != null && size > MAX_IMPORT_BYTES) throw ImportFileTooLargeException()

            val json =
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                } ?: throw ImportUnreadableException()
            importFromJson(json, replace)
        }

        private fun isMimeTypeAllowed(mimeType: String): Boolean =
            !mimeType.startsWith("image/") &&
                !mimeType.startsWith("video/") &&
                !mimeType.startsWith("audio/")

        companion object {
            private const val MAX_IMPORT_BYTES = 10L * 1024 * 1024
        }

        fun computeAutoPoints(
            time: Int,
            difficulty: Int,
            preparation: Int,
        ): Int {
            val base = ((time + 1) * (difficulty + 1) * (preparation + 1) + 7) / 8
            val bonus = if (maxOf(time, difficulty, preparation) == 5) 3 else 0
            return base + bonus
        }

        // ── Cleanup ───────────────────────────────────────────────────────────────

        suspend fun clearAllLogs() =
            database.withTransaction {
                logDao.deleteAllLogs()
                historyDao.deleteAllEntries()
            }

        suspend fun clearAllTasks() =
            database.withTransaction {
                rewardTaskDao.deleteAll()
                taskDao.deleteAllTasks()
            }

        suspend fun clearAllRewards() =
            database.withTransaction {
                rewardTaskDao.deleteAll()
                logDao.deleteAllActiveLogs()
                rewardDao.deleteAllRewards()
            }

        suspend fun clearAll() = withContext(Dispatchers.IO) { database.clearAllTables() }

        // ── Task template import ──────────────────────────────────────────────────

        suspend fun importTemplate(
            template: TaskTemplate,
            cleanSlate: Boolean,
        ): List<String> =
            database.withTransaction {
                if (cleanSlate) {
                    rewardTaskDao.deleteAll()
                    taskDao.deleteAllTasks()
                }
                val existingNames = taskDao.getAllTasks().map { it.name.trim().lowercase() }.toHashSet()
                val maxOrder = taskDao.getMaxSortOrder() ?: -1
                val skipped = mutableListOf<String>()
                var insertedCount = 0
                template.tasks.forEach { t ->
                    if (t.name.trim().lowercase() in existingNames) {
                        skipped.add(t.name)
                    } else {
                        taskDao.insertTask(
                            TaskEntity(
                                name = t.name,
                                points = t.points,
                                icon = t.icon,
                                sortOrder = maxOrder + 1 + insertedCount,
                                group = template.name,
                            ),
                        )
                        insertedCount++
                    }
                }
                skipped
            }

        // TEST DATA — gated behind Settings.devModeEnabled (7-tap on About version); not removed, see CLEANUP_LOG Pass 21
        suspend fun seedTestData() = TestDataSeeder.seed(database)

        suspend fun seedFullTestData() = TestDataSeeder.seedFull(database)

        // Ensures no completion log is newer than `hoursAgo` so the inactivity-nudge worker can
        // be exercised without waiting 48/96 real hours — see NudgeDecider.
        suspend fun debugBackdateLastLog(hoursAgo: Int) {
            logDao.debugCapLogTimestamps(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hoursAgo.toLong()))
        }

        suspend fun debugGetLastLogTimestamp(): Long? = logDao.getLastLogTimestamp()
    }

data class EarnItUiState(
    val rewardProgressList: List<RewardProgress> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val historyEntries: List<HistoryEntryWithLogs> = emptyList(),
    val allLogs: List<CompletionLogEntity> = emptyList(),
)
