package com.earnit.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

// Schema versions 1-10 were internal dev-only churn (renames, added columns) with no real
// install to ever migrate — collapsed here into a single v1 launch baseline. From this point
// on, every version bump MUST ship a real Migration (see docs/DEV_PLAYBOOK.md §6) — otherwise
// AppModule's fallbackToDestructiveMigration silently wipes a real user's entire database.
@Database(
    entities = [
        TaskEntity::class,
        CompletionLogEntity::class,
        RewardEntity::class,
        RewardTaskCrossRef::class,
        HistoryEntryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class EarnItDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    abstract fun completionLogDao(): CompletionLogDao

    abstract fun rewardDao(): RewardDao

    abstract fun rewardTaskCrossRefDao(): RewardTaskCrossRefDao

    abstract fun historyDao(): HistoryDao
}
