package com.earnit.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earnit.app.data.AppColorScheme
import com.earnit.app.data.AppSettings
import com.earnit.app.data.EarnItRepository
import com.earnit.app.data.EarnItUiState
import com.earnit.app.data.ImportFileTooLargeException
import com.earnit.app.data.ImportInvalidJsonException
import com.earnit.app.data.ImportUnreadableException
import com.earnit.app.data.ImportWrongFileTypeException
import com.earnit.app.data.ImportWrongSchemaException
import com.earnit.app.data.MascotId
import com.earnit.app.data.Mascots
import com.earnit.app.data.RewardEntity
import com.earnit.app.data.SettingsRepository
import com.earnit.app.data.TaskEntity
import com.earnit.app.data.TaskTemplate
import com.earnit.app.ui.Strings
import com.earnit.app.ui.randomNicknames
import com.earnit.app.widget.EarnItGlanceWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EarnItViewModel
    @Inject
    constructor(
        private val repository: EarnItRepository,
        private val settingsRepository: SettingsRepository,
        @param:ApplicationContext private val context: Context,
    ) : ViewModel() {
        var sessionNickname by mutableStateOf(randomNicknames.random())
            private set

        val settings: StateFlow<AppSettings> =
            settingsRepository.settings
                .catch { emit(AppSettings()) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

        val uiState: StateFlow<EarnItUiState> =
            repository
                .observeUiState()
                .catch { emit(EarnItUiState()) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EarnItUiState())

        private val _pendingTaskId = MutableStateFlow<Long?>(null)
        val pendingTaskId: StateFlow<Long?> = _pendingTaskId.asStateFlow()

        private val _pendingRewardId = MutableStateFlow<Long?>(null)
        val pendingRewardId: StateFlow<Long?> = _pendingRewardId.asStateFlow()

        private val _newlyUnlockedMascot = MutableSharedFlow<MascotId>(extraBufferCapacity = 1)
        val newlyUnlockedMascot: SharedFlow<MascotId> = _newlyUnlockedMascot.asSharedFlow()

        private val _triggerMascotBounce = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val triggerMascotBounce: SharedFlow<Unit> = _triggerMascotBounce.asSharedFlow()

        private val _openMascotPicker = MutableStateFlow<MascotId?>(null)
        val openMascotPicker: StateFlow<MascotId?> = _openMascotPicker.asStateFlow()

        private val _hasNewMascot = MutableStateFlow(false)
        val hasNewMascot: StateFlow<Boolean> = _hasNewMascot.asStateFlow()

        private val _triggerInAppReview = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val triggerInAppReview: SharedFlow<Unit> = _triggerInAppReview.asSharedFlow()

        private val _importResult = MutableStateFlow<ImportResult?>(null)
        val importResult: StateFlow<ImportResult?> = _importResult.asStateFlow()

        data class ImportResult(
            val message: String,
            val isError: Boolean,
        )

        fun triggerMascotPickerFor(id: MascotId) {
            _openMascotPicker.value = id
        }

        fun consumeMascotPickerId() {
            _openMascotPicker.value = null
        }

        fun clearNewMascotBadge() {
            _hasNewMascot.value = false
        }

        fun clearImportResult() {
            _importResult.value = null
        }

        fun saveTask(
            task: TaskEntity,
            rewardLinks: Map<Long, Pair<Boolean, Boolean>> = emptyMap(),
        ) {
            viewModelScope.launch {
                val taskId = repository.upsertTask(task)
                if (task.id == 0L) {
                    _pendingTaskId.value = taskId
                    rewardLinks.forEach { (rewardId, flags) ->
                        if (rewardId != 0L) repository.addTaskToReward(rewardId, taskId, flags.first, flags.second)
                    }
                } else {
                    repository.updateTaskRewards(task.id, rewardLinks)
                }
            }
        }

        fun consumePendingTaskId() {
            _pendingTaskId.value = null
        }

        fun consumePendingRewardId() {
            _pendingRewardId.value = null
        }

        fun saveReward(
            rewardId: Long,
            name: String,
            cost: Int,
            description: String,
            icon: String,
            tasks: List<Triple<Long, Boolean, Boolean>>,
        ) {
            viewModelScope.launch {
                val reward =
                    if (rewardId ==
                        0L
                    ) {
                        RewardEntity(name = name, cost = cost, description = description, icon = icon)
                    } else {
                        repository
                            .getRewardOrNull(
                                rewardId,
                            )?.copy(name = name, cost = cost, description = description, icon = icon)
                    }
                if (reward != null) {
                    val id = repository.upsertReward(reward)
                    repository.saveRewardTasks(id, tasks)
                    if (rewardId == 0L) _pendingRewardId.value = id
                }
            }
        }

        fun logTask(
            task: TaskEntity,
            rewardId: Long,
            detail: String,
        ) {
            viewModelScope.launch {
                repository.logCompletion(task, rewardId, detail)
                refreshWidgets()
            }
        }

        fun claimReward(
            rewardId: Long,
            startOver: Boolean,
        ) {
            viewModelScope.launch {
                val isFirstClaim = uiState.value.historyEntries.isEmpty()
                repository.claimReward(rewardId, startOver)
                if (isFirstClaim) _triggerInAppReview.tryEmit(Unit)
                refreshWidgets()
                checkAndUnlockMascots(pendingClaim = true)
                _triggerMascotBounce.tryEmit(Unit)
            }
        }

        private suspend fun checkAndUnlockMascots(
            pendingClaim: Boolean = false,
            silent: Boolean = false,
        ) {
            val state = uiState.value
            val current = settings.value.unlockedMascotIds
            val newlyUnlocked =
                Mascots.computeNewlyUnlocked(
                    totalClaims = state.historyEntries.size + if (pendingClaim) 1 else 0,
                    totalPoints = state.allLogs.sumOf { it.points },
                    totalTasks = state.allLogs.size,
                    alreadyUnlocked = current,
                )
            if (newlyUnlocked.isNotEmpty()) {
                settingsRepository.updateUnlockedMascots(current + newlyUnlocked)
                if (!silent) {
                    _newlyUnlockedMascot.emit(newlyUnlocked.first())
                    _hasNewMascot.value = true
                }
            }
        }

        private suspend fun refreshWidgets() {
            try {
                EarnItGlanceWidget().updateAll(context)
            } catch (_: Exception) {
            }
        }

        fun addTaskToReward(
            rewardId: Long,
            taskId: Long,
            isMandatory: Boolean,
            isRepeatable: Boolean,
        ) {
            viewModelScope.launch { repository.addTaskToReward(rewardId, taskId, isMandatory, isRepeatable) }
        }

        fun deleteTask(
            taskId: Long,
            onComplete: () -> Unit,
        ) {
            viewModelScope.launch {
                repository.deleteTask(taskId)
                onComplete()
            }
        }

        fun deleteReward(
            rewardId: Long,
            onComplete: () -> Unit,
        ) {
            viewModelScope.launch {
                repository.deleteReward(rewardId)
                onComplete()
            }
        }

        fun updateRewardsOrder(orderedIds: List<Long>) {
            viewModelScope.launch { repository.updateRewardsSortOrder(orderedIds) }
        }

        fun updateTasksOrder(orderedIds: List<Long>) {
            viewModelScope.launch { repository.updateTasksSortOrder(orderedIds) }
        }

        fun copyRewardFromEntry(entryId: Long) {
            viewModelScope.launch { repository.copyRewardFromEntry(entryId) }
        }

        fun exportToFile(
            context: Context,
            uri: Uri,
            onComplete: (Boolean) -> Unit,
        ) {
            viewModelScope.launch {
                runCatching { repository.exportToFile(context, uri) }
                    .onSuccess { onComplete(true) }
                    .onFailure { onComplete(false) }
            }
        }

        fun importFromFile(
            context: Context,
            uri: Uri,
            replace: Boolean,
            onComplete: (String?) -> Unit = {},
        ) {
            viewModelScope.launch {
                runCatching { repository.importFromFile(context, uri, replace) }
                    .onSuccess {
                        val successMsg = if (replace) Strings.DATA_IMPORT_SUCCESS else Strings.DATA_IMPORT_MERGE_SUCCESS
                        _importResult.value = ImportResult(successMsg, false)
                        onComplete(null)
                        // Seed unlocked mascots silently so the startup check finds nothing new.
                        uiState.drop(1).first()
                        checkAndUnlockMascots(silent = true)
                    }.onFailure { e ->
                        val errorMsg =
                            when (e) {
                                is ImportFileTooLargeException -> Strings.IMPORT_ERROR_TOO_LARGE
                                is ImportWrongFileTypeException -> Strings.IMPORT_ERROR_WRONG_TYPE
                                is ImportInvalidJsonException -> Strings.IMPORT_ERROR_INVALID_JSON
                                is ImportWrongSchemaException -> Strings.IMPORT_ERROR_WRONG_SCHEMA
                                is ImportUnreadableException -> Strings.IMPORT_ERROR_UNREADABLE
                                else -> Strings.DATA_IMPORT_FAIL
                            }
                        _importResult.value = ImportResult(errorMsg, true)
                        onComplete(errorMsg)
                    }
            }
        }

        fun computeAutoPoints(
            time: Int,
            difficulty: Int,
            preparation: Int,
        ): Int = repository.computeAutoPoints(time, difficulty, preparation)

        // ── Settings ──────────────────────────────────────────────────────────────

        fun updateColorScheme(scheme: AppColorScheme) {
            viewModelScope.launch { settingsRepository.updateColorScheme(scheme) }
        }

        fun updateNotesMandatory(mandatory: Boolean) {
            viewModelScope.launch { settingsRepository.updateNotesMandatory(mandatory) }
        }

        fun updateOptimalRewardCount(count: Int) {
            viewModelScope.launch { settingsRepository.updateOptimalRewardCount(count) }
        }

        fun updateMaxRewardCount(count: Int) {
            viewModelScope.launch { settingsRepository.updateMaxRewardCount(count) }
        }

        fun updateNickname(name: String) {
            viewModelScope.launch { settingsRepository.updateNickname(name) }
        }

        fun updateSelectedMascot(id: MascotId?) {
            viewModelScope.launch { settingsRepository.updateSelectedMascot(id) }
        }

        fun updateShowQuote(show: Boolean) {
            viewModelScope.launch { settingsRepository.updateShowQuote(show) }
        }

        fun updateUseRandomNickname(use: Boolean) {
            viewModelScope.launch { settingsRepository.updateUseRandomNickname(use) }
        }

        fun toggleTasksGroupView() {
            viewModelScope.launch {
                settingsRepository.updateTasksGroupView(!settings.value.tasksGroupView)
            }
        }

        fun refreshNickname() {
            sessionNickname = randomNicknames.random()
            viewModelScope.launch { settingsRepository.updateUseRandomNickname(true) }
        }

        // ── Cleanup ───────────────────────────────────────────────────────────────

        fun clearAllLogs(onComplete: () -> Unit = {}) {
            viewModelScope.launch {
                repository.clearAllLogs()
                onComplete()
            }
        }

        fun clearAllTasks(onComplete: () -> Unit = {}) {
            viewModelScope.launch {
                repository.clearAllTasks()
                onComplete()
            }
        }

        fun clearAllRewards(onComplete: () -> Unit = {}) {
            viewModelScope.launch {
                repository.clearAllRewards()
                onComplete()
            }
        }

        fun clearAll(onComplete: () -> Unit = {}) {
            viewModelScope.launch {
                repository.clearAll()
                onComplete()
            }
        }

        fun importTemplate(
            template: TaskTemplate,
            cleanSlate: Boolean,
            onDone: (skipped: List<String>) -> Unit = {},
        ) {
            viewModelScope.launch { onDone(repository.importTemplate(template, cleanSlate)) }
        }

        fun enableDevMode() {
            viewModelScope.launch { settingsRepository.enableDevMode() }
        }

        fun disableDevMode() {
            viewModelScope.launch { settingsRepository.disableDevMode() }
        }

        // TEST DATA — only visible when dev mode enabled (7-tap on version in About)
        fun seedTestData() {
            viewModelScope.launch { repository.seedTestData() }
        }

        fun seedFullTestData() {
            viewModelScope.launch { repository.seedFullTestData() }
        }

        fun runStartupUnlockCheck() {
            viewModelScope.launch { checkAndUnlockMascots(pendingClaim = false) }
        }
    }
