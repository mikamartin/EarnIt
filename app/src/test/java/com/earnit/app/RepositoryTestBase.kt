package com.earnit.app

import com.earnit.app.data.CompletionLogDao
import com.earnit.app.data.EarnItDatabase
import com.earnit.app.data.EarnItRepository
import com.earnit.app.data.HistoryDao
import com.earnit.app.data.RewardDao
import com.earnit.app.data.RewardTaskCrossRefDao
import com.earnit.app.data.TaskDao
import io.mockk.every
import io.mockk.mockk

abstract class RepositoryTestBase {
    protected val database = mockk<EarnItDatabase>(relaxed = true)
    protected val rewardDao = mockk<RewardDao>()
    protected val logDao = mockk<CompletionLogDao>()
    protected val historyDao = mockk<HistoryDao>()
    protected val rewardTaskDao = mockk<RewardTaskCrossRefDao>()
    protected val taskDao = mockk<TaskDao>()

    init {
        every { database.rewardDao() } returns rewardDao
        every { database.completionLogDao() } returns logDao
        every { database.historyDao() } returns historyDao
        every { database.rewardTaskCrossRefDao() } returns rewardTaskDao
        every { database.taskDao() } returns taskDao
    }

    protected val repository = EarnItRepository(database)
}
