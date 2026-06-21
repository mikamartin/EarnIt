package com.earnit.app

import android.content.Context
import com.earnit.app.data.AppSettings
import com.earnit.app.data.EarnItRepository
import com.earnit.app.data.EarnItUiState
import com.earnit.app.data.RewardEntity
import com.earnit.app.data.SettingsRepository
import com.earnit.app.viewmodel.EarnItViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PendingRewardIdTest {
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

    private fun makeViewModel(repository: EarnItRepository = mockk(relaxed = true)): EarnItViewModel {
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        every { repository.observeUiState() } returns flowOf(EarnItUiState())
        every { settingsRepository.settings } returns flowOf(AppSettings())
        return EarnItViewModel(repository, settingsRepository, context)
    }

    @Test
    fun `saveReward with new reward sets pendingRewardId to upserted id`() =
        runTest(testDispatcher) {
            val repository = mockk<EarnItRepository>(relaxed = true)
            coEvery { repository.upsertReward(any()) } returns 42L
            val viewModel = makeViewModel(repository)
            val stateJob = launch { viewModel.uiState.collect { } }

            viewModel.saveReward(0L, "Pizza Night", 10, "", "", emptyList())
            advanceUntilIdle()

            assertEquals(42L, viewModel.pendingRewardId.value)
            stateJob.cancel()
        }

    @Test
    fun `saveReward editing existing reward leaves pendingRewardId null`() =
        runTest(testDispatcher) {
            val repository = mockk<EarnItRepository>(relaxed = true)
            coEvery { repository.getRewardOrNull(99L) } returns
                RewardEntity(id = 99L, name = "Old Name", cost = 5, description = "", icon = "")
            val viewModel = makeViewModel(repository)
            val stateJob = launch { viewModel.uiState.collect { } }

            viewModel.saveReward(99L, "New Name", 15, "", "", emptyList())
            advanceUntilIdle()

            assertNull(viewModel.pendingRewardId.value)
            stateJob.cancel()
        }

    @Test
    fun `consumePendingRewardId clears the pending id`() =
        runTest(testDispatcher) {
            val repository = mockk<EarnItRepository>(relaxed = true)
            coEvery { repository.upsertReward(any()) } returns 7L
            val viewModel = makeViewModel(repository)
            val stateJob = launch { viewModel.uiState.collect { } }

            viewModel.saveReward(0L, "Game Night", 20, "", "", emptyList())
            advanceUntilIdle()
            viewModel.consumePendingRewardId()

            assertNull(viewModel.pendingRewardId.value)
            stateJob.cancel()
        }
}
