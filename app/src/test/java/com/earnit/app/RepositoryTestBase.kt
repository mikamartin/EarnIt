package com.earnit.app

import androidx.room.withTransaction
import com.earnit.app.data.CompletionLogDao
import com.earnit.app.data.EarnItDatabase
import com.earnit.app.data.EarnItRepository
import com.earnit.app.data.HistoryDao
import com.earnit.app.data.RewardDao
import com.earnit.app.data.RewardTaskCrossRefDao
import com.earnit.app.data.TaskDao
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic

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

        // withTransaction dispatches onto Room's real transaction executor to run its block —
        // on a mocked database that executor never runs anything, so the suspend call would
        // hang forever. Stub it to just invoke the block directly; these tests verify DAO call
        // sequencing, not real transaction semantics.
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { database.withTransaction<Any?>(any()) } coAnswers {
            // args[0] is the RoomDatabase receiver (extension function) — the block is args[1].
            @Suppress("UNCHECKED_CAST")
            (it.invocation.args[1] as suspend () -> Any?).invoke()
        }

        // Default: no cross-ref found, so logCompletion's repeatable-guard check is skipped.
        // Tests exercising that guard specifically override this per-test.
        coEvery { rewardTaskDao.getTaskRefsForReward(any()) } returns emptyList()
    }

    protected val repository = EarnItRepository(database)
}
