package com.earnit.app

import android.content.Context
import android.net.Uri
import com.earnit.app.data.AppSettings
import com.earnit.app.data.EarnItRepository
import com.earnit.app.data.EarnItUiState
import com.earnit.app.data.ImportFileTooLargeException
import com.earnit.app.data.ImportInvalidJsonException
import com.earnit.app.data.ImportUnreadableException
import com.earnit.app.data.ImportWrongFileTypeException
import com.earnit.app.data.ImportWrongSchemaException
import com.earnit.app.data.SettingsRepository
import com.earnit.app.ui.Strings
import com.earnit.app.viewmodel.EarnItViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ImportViewModelErrorTest : ViewModelTestBase() {
    private lateinit var repository: EarnItRepository
    private lateinit var viewModel: EarnItViewModel
    private val uri = mockk<Uri>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        every { repository.observeUiState() } returns flowOf(EarnItUiState())
        every { settingsRepository.settings } returns flowOf(AppSettings())
        viewModel = EarnItViewModel(repository, settingsRepository, context)
    }

    private suspend fun TestScope.captureError(replace: Boolean = true): String? {
        var result: String? = "not set"
        viewModel.importFromFile(context, uri, replace) { result = it }
        advanceUntilIdle()
        return result
    }

    @Test
    fun `importFromFile success calls onComplete with null`() =
        runTest(testDispatcher) {
            coEvery { repository.importFromFile(any(), any(), any()) } returns Unit
            assertNull(captureError())
            assertEquals(Strings.DATA_IMPORT_SUCCESS, viewModel.importResult.value?.message)
            assertFalse(viewModel.importResult.value!!.isError)
        }

    @Test
    fun `importFromFile FileTooLarge calls onComplete with too large message`() =
        runTest(testDispatcher) {
            coEvery { repository.importFromFile(any(), any(), any()) } throws ImportFileTooLargeException()
            assertEquals(Strings.IMPORT_ERROR_TOO_LARGE, captureError())
            assertEquals(Strings.IMPORT_ERROR_TOO_LARGE, viewModel.importResult.value?.message)
            assertTrue(viewModel.importResult.value!!.isError)
        }

    @Test
    fun `importFromFile WrongFileType calls onComplete with wrong type message`() =
        runTest(testDispatcher) {
            coEvery { repository.importFromFile(any(), any(), any()) } throws ImportWrongFileTypeException()
            assertEquals(Strings.IMPORT_ERROR_WRONG_TYPE, captureError())
            assertEquals(Strings.IMPORT_ERROR_WRONG_TYPE, viewModel.importResult.value?.message)
            assertTrue(viewModel.importResult.value!!.isError)
        }

    @Test
    fun `importFromFile InvalidJson calls onComplete with invalid json message`() =
        runTest(testDispatcher) {
            coEvery { repository.importFromFile(any(), any(), any()) } throws ImportInvalidJsonException()
            assertEquals(Strings.IMPORT_ERROR_INVALID_JSON, captureError())
            assertEquals(Strings.IMPORT_ERROR_INVALID_JSON, viewModel.importResult.value?.message)
            assertTrue(viewModel.importResult.value!!.isError)
        }

    @Test
    fun `importFromFile WrongSchema calls onComplete with wrong schema message`() =
        runTest(testDispatcher) {
            coEvery { repository.importFromFile(any(), any(), any()) } throws ImportWrongSchemaException()
            assertEquals(Strings.IMPORT_ERROR_WRONG_SCHEMA, captureError())
            assertEquals(Strings.IMPORT_ERROR_WRONG_SCHEMA, viewModel.importResult.value?.message)
            assertTrue(viewModel.importResult.value!!.isError)
        }

    @Test
    fun `importFromFile Unreadable calls onComplete with unreadable message`() =
        runTest(testDispatcher) {
            coEvery { repository.importFromFile(any(), any(), any()) } throws ImportUnreadableException()
            assertEquals(Strings.IMPORT_ERROR_UNREADABLE, captureError())
            assertEquals(Strings.IMPORT_ERROR_UNREADABLE, viewModel.importResult.value?.message)
            assertTrue(viewModel.importResult.value!!.isError)
        }

    @Test
    fun `importFromFile unknown exception calls onComplete with generic fail message`() =
        runTest(testDispatcher) {
            coEvery { repository.importFromFile(any(), any(), any()) } throws RuntimeException("unexpected")
            assertEquals(Strings.DATA_IMPORT_FAIL, captureError())
            assertEquals(Strings.DATA_IMPORT_FAIL, viewModel.importResult.value?.message)
            assertTrue(viewModel.importResult.value!!.isError)
        }
}
