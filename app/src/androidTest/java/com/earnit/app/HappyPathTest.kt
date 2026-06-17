package com.earnit.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earnit.app.data.EarnItDatabase
import com.earnit.app.data.EarnItRepository
import com.earnit.app.data.RewardEntity
import com.earnit.app.data.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E happy path: create task + reward → log completion → verify claimable → claim → verify history.
 * Uses a real in-memory Room database; no mocks.
 */
@RunWith(AndroidJUnit4::class)
class HappyPathTest {
    private lateinit var database: EarnItDatabase
    private lateinit var repository: EarnItRepository

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    EarnItDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        repository = EarnItRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createTask_logCompletion_claimReward_appearsInHistory() =
        runBlocking {
            // 1. Create a task
            val taskId =
                repository.upsertTask(
                    TaskEntity(name = "Morning Run", points = 5, icon = "🏃"),
                )
            assertNotEquals(0L, taskId)

            // 2. Create a reward and link the task as mandatory
            val rewardId =
                repository.upsertReward(
                    RewardEntity(name = "Coffee Treat", cost = 5),
                )
            repository.saveRewardTasks(rewardId, listOf(Triple(taskId, true, false)))

            // 3. Log the task completion toward the reward
            val task = repository.getTaskOrNull(taskId)!!
            repository.logCompletion(task, rewardId = rewardId, detail = "Felt great")

            // 4. Verify the reward is now claimable
            val stateAfterLog = repository.observeUiState().first()
            val progress = stateAfterLog.rewardProgressList.find { it.reward.id == rewardId }
            assertNotNull("Reward should appear in active list", progress)
            assertEquals(5, progress!!.totalPoints)
            assertTrue("Should have enough points", progress.totalPoints >= progress.reward.cost)
            assertTrue("All mandatory tasks logged", progress.canClaim)

            // 5. Claim the reward (archive, no start-over)
            repository.claimReward(rewardId, startOver = false)

            // 6. Verify a history entry was created
            val stateAfterClaim = repository.observeUiState().first()
            assertEquals(1, stateAfterClaim.historyEntries.size)

            val entry = stateAfterClaim.historyEntries[0]
            assertEquals("Coffee Treat", entry.entry.rewardName)
            assertEquals(5, entry.entry.pointCost)

            // 7. Verify the log was archived under this history entry
            assertEquals(1, entry.logs.size)
            assertEquals("Morning Run", entry.logs[0].taskName)
            assertEquals("Felt great", entry.logs[0].detail)
            assertEquals(5, entry.logs[0].points)
            assertNotNull("Log should be linked to history entry", entry.logs[0].historyEntryId)

            // 8. Verify the reward is archived and removed from active view
            assertTrue(
                "Reward should be archived",
                repository.getRewardOrNull(rewardId)?.isArchived == true,
            )
            assertTrue(
                "Archived reward should not appear in active list",
                stateAfterClaim.rewardProgressList.none { it.reward.id == rewardId },
            )
        }
}
