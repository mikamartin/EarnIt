package com.earnit.app

import android.content.Context
import com.earnit.app.data.AppSettings
import com.earnit.app.data.EarnItRepository
import com.earnit.app.data.EarnItUiState
import com.earnit.app.data.SettingsRepository
import com.earnit.app.viewmodel.EarnItViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers the ViewModel-level logic behind the "Inactivity nudge" dev tool in DataScreen — the
 * status text shown after tapping 48H/96H, and the ordering guarantee that lets the dev tool
 * backdate-then-check in one tap without racing NudgeWorker against a still-in-flight DB write.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NudgeDebugToolsTest : ViewModelTestBase() {
    private fun makeViewModel(repository: EarnItRepository = mockk(relaxed = true)): EarnItViewModel {
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        every { repository.observeUiState() } returns flowOf(EarnItUiState())
        every { settingsRepository.settings } returns flowOf(AppSettings())
        return EarnItViewModel(repository, settingsRepository, context)
    }

    @Test
    fun `debugGetLastLogIdleHours reports whole hours since the last log`() =
        runTest(testDispatcher) {
            val repository = mockk<EarnItRepository>(relaxed = true)
            val threeHoursAgo = System.currentTimeMillis() - 3 * 60 * 60 * 1000L - 5_000L
            coEvery { repository.debugGetLastLogTimestamp() } returns threeHoursAgo
            val viewModel = makeViewModel(repository)
            val stateJob = launch { viewModel.uiState.collect { } }
            val settingsJob = launch { viewModel.settings.collect { } }

            var result: Long? = -1L
            viewModel.debugGetLastLogIdleHours { result = it }
            advanceUntilIdle()

            assertEquals(3L, result)
            settingsJob.cancel()
            stateJob.cancel()
        }

    @Test
    fun `debugGetLastLogIdleHours returns null when no log has ever been recorded`() =
        runTest(testDispatcher) {
            val repository = mockk<EarnItRepository>(relaxed = true)
            coEvery { repository.debugGetLastLogTimestamp() } returns null
            val viewModel = makeViewModel(repository)
            val stateJob = launch { viewModel.uiState.collect { } }
            val settingsJob = launch { viewModel.settings.collect { } }

            var result: Long? = -1L
            viewModel.debugGetLastLogIdleHours { result = it }
            advanceUntilIdle()

            assertNull(result)
            settingsJob.cancel()
            stateJob.cancel()
        }

    @Test
    fun `debugBackdateLastLog writes to the repository and invokes onComplete exactly once`() =
        runTest(testDispatcher) {
            val repository = mockk<EarnItRepository>(relaxed = true)
            val viewModel = makeViewModel(repository)
            val stateJob = launch { viewModel.uiState.collect { } }
            val settingsJob = launch { viewModel.settings.collect { } }

            var completedCount = 0
            viewModel.debugBackdateLastLog(49) { completedCount++ }
            advanceUntilIdle()

            assertEquals(1, completedCount)
            coVerify(exactly = 1) { repository.debugBackdateLastLog(49) }
            settingsJob.cancel()
            stateJob.cancel()
        }
}
