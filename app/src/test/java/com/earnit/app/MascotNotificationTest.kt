package com.earnit.app

import android.content.Context
import android.net.Uri
import com.earnit.app.data.AppSettings
import com.earnit.app.data.EarnItRepository
import com.earnit.app.data.EarnItUiState
import com.earnit.app.data.HistoryEntryEntity
import com.earnit.app.data.HistoryEntryWithLogs
import com.earnit.app.data.MascotId
import com.earnit.app.data.Mascots
import com.earnit.app.data.SettingsRepository
import com.earnit.app.viewmodel.EarnItViewModel
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MascotNotificationTest : ViewModelTestBase() {
    private fun makeViewModel(
        historyEntries: List<HistoryEntryWithLogs> = emptyList(),
        unlockedMascots: Set<MascotId> = setOf(MascotId.PUGSLY, MascotId.TABBY),
    ): EarnItViewModel {
        val repository = mockk<EarnItRepository>(relaxed = true)
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        every { repository.observeUiState() } returns
            flowOf(EarnItUiState(historyEntries = historyEntries))
        every { settingsRepository.settings } returns
            flowOf(AppSettings(unlockedMascotIds = unlockedMascots))
        return EarnItViewModel(repository, settingsRepository, context)
    }

    @Test
    fun `claimReward sets hasNewMascot when a new mascot is unlocked`() =
        runTest(testDispatcher) {
            val viewModel = makeViewModel()
            val stateJob = launch { viewModel.uiState.collect { } }
            val settingsJob = launch { viewModel.settings.collect { } }

            // 1 claim satisfies ClaimsReached(1) — MASCOT_3 (Panda) qualifies
            viewModel.claimReward(rewardId = 1L, startOver = false)
            advanceUntilIdle()

            assertTrue(viewModel.hasNewMascot.value)
            settingsJob.cancel()
            stateJob.cancel()
        }

    @Test
    fun `claimReward does not set hasNewMascot when all mascots already unlocked`() =
        runTest(testDispatcher) {
            val allUnlocked = Mascots.all.map { it.id }.toSet()
            val viewModel = makeViewModel(unlockedMascots = allUnlocked)
            val stateJob = launch { viewModel.uiState.collect { } }
            val settingsJob = launch { viewModel.settings.collect { } }

            viewModel.claimReward(rewardId = 1L, startOver = false)
            advanceUntilIdle()

            assertFalse(viewModel.hasNewMascot.value)
            settingsJob.cancel()
            stateJob.cancel()
        }

    @Test
    fun `importFromFile seeds unlocked mascots silently without triggering notification`() =
        runTest(testDispatcher) {
            val uiStateFlow = MutableStateFlow(EarnItUiState())
            val repository = mockk<EarnItRepository>(relaxed = true)
            val settingsRepository = mockk<SettingsRepository>(relaxed = true)
            val context = mockk<Context>(relaxed = true)
            val uri = mockk<Uri>(relaxed = true)
            every { repository.observeUiState() } returns uiStateFlow
            every { settingsRepository.settings } returns flowOf(AppSettings())

            val viewModel = EarnItViewModel(repository, settingsRepository, context)
            val stateJob = launch { viewModel.uiState.collect { } }
            val settingsJob = launch { viewModel.settings.collect { } }

            val unlockEvents = mutableListOf<MascotId>()
            val unlockJob = launch { viewModel.newlyUnlockedMascot.toList(unlockEvents) }

            viewModel.importFromFile(context, uri, replace = true) { }

            // Simulate Room emitting updated state after import: 1 history entry
            // satisfies ClaimsReached(1) and would normally trigger MASCOT_3.
            uiStateFlow.value =
                EarnItUiState(
                    historyEntries =
                        listOf(
                            HistoryEntryWithLogs(
                                entry =
                                    HistoryEntryEntity(
                                        rewardId = 1L,
                                        rewardName = "Reward",
                                        pointCost = 10,
                                        claimedAt = 0L,
                                    ),
                            ),
                        ),
                )
            advanceUntilIdle()

            coVerify(exactly = 1) { settingsRepository.updateUnlockedMascots(any()) }
            assertEquals(0, unlockEvents.size)
            assertFalse(viewModel.hasNewMascot.value)

            unlockJob.cancel()
            settingsJob.cancel()
            stateJob.cancel()
        }
}
