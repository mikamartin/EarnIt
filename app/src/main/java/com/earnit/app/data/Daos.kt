package com.earnit.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY sortOrder ASC, name ASC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY sortOrder ASC, name ASC")
    suspend fun getAllTasks(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTaskIgnore(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTask(id: Long): TaskEntity?

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("UPDATE tasks SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(
        id: Long,
        sortOrder: Int,
    )

    @Query("SELECT MAX(sortOrder) FROM tasks")
    suspend fun getMaxSortOrder(): Int?

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}

@Dao
interface CompletionLogDao {
    @Query("SELECT * FROM completion_logs ORDER BY timestamp DESC")
    fun observeLogs(): Flow<List<CompletionLogEntity>>

    @Query("SELECT * FROM completion_logs ORDER BY timestamp DESC")
    suspend fun getAllLogs(): List<CompletionLogEntity>

    @Insert
    suspend fun insertLog(log: CompletionLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLogIgnore(log: CompletionLogEntity): Long

    @Query("UPDATE completion_logs SET historyEntryId = :entryId WHERE rewardId = :rewardId AND historyEntryId IS NULL")
    suspend fun archiveLogsForReward(
        rewardId: Long,
        entryId: Long,
    )

    @Query("DELETE FROM completion_logs WHERE rewardId = :rewardId AND historyEntryId IS NULL")
    suspend fun deleteActiveLogsForReward(rewardId: Long)

    @Query("DELETE FROM completion_logs WHERE historyEntryId IS NULL")
    suspend fun deleteAllActiveLogs()

    @Query("DELETE FROM completion_logs")
    suspend fun deleteAllLogs()

    @Query("SELECT MAX(timestamp) FROM completion_logs")
    suspend fun getLastLogTimestamp(): Long?

    @Query("UPDATE completion_logs SET timestamp = :timestamp WHERE id = (SELECT id FROM completion_logs ORDER BY timestamp DESC LIMIT 1)")
    suspend fun debugSetLastLogTimestamp(timestamp: Long)
}

@Dao
interface RewardDao {
    @Query("SELECT * FROM rewards WHERE isArchived = 0 ORDER BY sortOrder ASC, name ASC")
    fun observeActiveRewards(): Flow<List<RewardEntity>>

    @Query("SELECT * FROM rewards ORDER BY name ASC")
    suspend fun getAllRewards(): List<RewardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReward(reward: RewardEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRewardIgnore(reward: RewardEntity): Long

    @Update
    suspend fun updateReward(reward: RewardEntity)

    @Query("SELECT * FROM rewards WHERE id = :id")
    suspend fun getReward(id: Long): RewardEntity?

    @Query("UPDATE rewards SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(
        id: Long,
        sortOrder: Int,
    )

    @Query("SELECT MAX(sortOrder) FROM rewards WHERE isArchived = 0")
    suspend fun getMaxSortOrder(): Int?

    @Query("DELETE FROM rewards WHERE id = :id")
    suspend fun deleteReward(id: Long)

    @Query("SELECT COUNT(*) FROM rewards WHERE isArchived = 0")
    suspend fun getActiveRewardCount(): Int

    @Query("DELETE FROM rewards")
    suspend fun deleteAllRewards()
}

@Dao
interface RewardTaskCrossRefDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(ref: RewardTaskCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRefIgnore(ref: RewardTaskCrossRef)

    @Query("DELETE FROM reward_task_cross_ref WHERE rewardId = :rewardId")
    suspend fun clearTasksForReward(rewardId: Long)

    @Query("DELETE FROM reward_task_cross_ref WHERE rewardId = :rewardId AND taskId = :taskId")
    suspend fun deleteCrossRef(
        rewardId: Long,
        taskId: Long,
    )

    @Query("SELECT * FROM reward_task_cross_ref WHERE rewardId = :rewardId")
    suspend fun getTaskRefsForReward(rewardId: Long): List<RewardTaskCrossRef>

    @Query("SELECT * FROM reward_task_cross_ref")
    fun observeAllCrossRefs(): Flow<List<RewardTaskCrossRef>>

    @Query("SELECT * FROM reward_task_cross_ref")
    suspend fun getAllCrossRefs(): List<RewardTaskCrossRef>

    @Query("DELETE FROM reward_task_cross_ref")
    suspend fun deleteAll()
}

@Dao
interface HistoryDao {
    @Transaction
    @Query("SELECT * FROM history_entries ORDER BY claimedAt DESC")
    fun observeEntriesWithLogs(): Flow<List<HistoryEntryWithLogs>>

    @Query("SELECT * FROM history_entries ORDER BY claimedAt DESC")
    suspend fun getAllEntries(): List<HistoryEntryEntity>

    @Insert
    suspend fun insertEntry(entry: HistoryEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntryIgnore(entry: HistoryEntryEntity): Long

    @Query("DELETE FROM history_entries")
    suspend fun deleteAllEntries()
}
