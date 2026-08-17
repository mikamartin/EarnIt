package com.earnit.app

import android.content.Context
import com.earnit.app.data.AppSettings
import com.earnit.app.data.EarnItRepository
import com.earnit.app.data.EarnItUiState
import com.earnit.app.data.SettingsRepository
import com.earnit.app.viewmodel.EarnItViewModel
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WipeEverythingViewModelTest : ViewModelTestBase() {
    private lateinit var repository: EarnItRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: EarnItViewModel

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        every { repository.observeUiState() } returns flowOf(EarnItUiState())
        every { settingsRepository.settings } returns flowOf(AppSettings())
        viewModel = EarnItViewModel(repository, settingsRepository, mockk<Context>(relaxed = true))
    }

    @Test
    fun `clearAll wipes the database then resets settings preserving backup choice`() =
        runTest(testDispatcher) {
            coEvery { repository.clearAll() } returns Unit
            coEvery { settingsRepository.resetForWipeEverything() } returns Unit

            var completed = false
            viewModel.clearAll { completed = true }
            advanceUntilIdle()

            coVerifyOrder {
                repository.clearAll()
                settingsRepository.resetForWipeEverything()
            }
            assert(completed)
        }
}
