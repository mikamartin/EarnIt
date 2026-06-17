package com.earnit.app

import android.content.Context
import com.earnit.app.data.AppSettings
import com.earnit.app.data.EarnItRepository
import com.earnit.app.data.EarnItUiState
import com.earnit.app.data.HistoryEntryEntity
import com.earnit.app.data.HistoryEntryWithLogs
import com.earnit.app.data.SettingsRepository
import com.earnit.app.viewmodel.EarnItViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InAppReviewTriggerTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun makeViewModel(historyEntries: List<HistoryEntryWithLogs> = emptyList()): EarnItViewModel {
        val repository = mockk<EarnItRepository>(relaxed = true)
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        val context = mockk<Context>(relaxed = true)

        every { repository.observeUiState() } returns
            flowOf(
                EarnItUiState(historyEntries = historyEntries),
            )
        every { settingsRepository.settings } returns flowOf(AppSettings())

        return EarnItViewModel(repository, settingsRepository, context)
    }

    @Test
    fun `claimReward emits triggerInAppReview on first ever claim`() =
        runTest(testDispatcher) {
            val viewModel = makeViewModel(historyEntries = emptyList())

            // Subscribe so WhileSubscribed activates upstream and uiState.value reflects the mock
            val stateJob = launch { viewModel.uiState.collect { } }

            val events = mutableListOf<Unit>()
            val eventJob = launch { viewModel.triggerInAppReview.toList(events) }

            viewModel.claimReward(rewardId = 1L, startOver = false)
            advanceUntilIdle()

            assertEquals(1, events.size)
            eventJob.cancel()
            stateJob.cancel()
        }

    @Test
    fun `claimReward does not emit triggerInAppReview when history already exists`() =
        runTest(testDispatcher) {
            val existingEntry =
                HistoryEntryWithLogs(
                    entry =
                        HistoryEntryEntity(
                            rewardId = 1L,
                            rewardName = "Past Reward",
                            pointCost = 10,
                            claimedAt = 0L,
                        ),
                )
            val viewModel = makeViewModel(historyEntries = listOf(existingEntry))

            // Subscribe so WhileSubscribed activates upstream and uiState.value reflects the mock
            val stateJob = launch { viewModel.uiState.collect { } }

            val events = mutableListOf<Unit>()
            val eventJob = launch { viewModel.triggerInAppReview.toList(events) }

            viewModel.claimReward(rewardId = 2L, startOver = false)
            advanceUntilIdle()

            assertEquals(0, events.size)
            eventJob.cancel()
            stateJob.cancel()
        }
}
