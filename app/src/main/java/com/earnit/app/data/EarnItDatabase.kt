package com.earnit.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TaskEntity::class,
        CompletionLogEntity::class,
        RewardEntity::class,
        RewardTaskCrossRef::class,
        HistoryEntryEntity::class,
    ],
    version = 10,
    exportSchema = false,
)
abstract class EarnItDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    abstract fun completionLogDao(): CompletionLogDao

    abstract fun rewardDao(): RewardDao

    abstract fun rewardTaskCrossRefDao(): RewardTaskCrossRefDao

    abstract fun historyDao(): HistoryDao

    companion object {
        val MIGRATION_9_10 =
            object : Migration(9, 10) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE tasks ADD COLUMN `group` TEXT DEFAULT NULL")
                }
            }
    }
}
