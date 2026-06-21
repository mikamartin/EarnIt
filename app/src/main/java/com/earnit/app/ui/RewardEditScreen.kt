@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.earnit.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.earnit.app.data.EarnItUiState
import com.earnit.app.viewmodel.EarnItViewModel
import kotlinx.coroutines.launch

@Composable
fun RewardEditScreen(
    rewardId: Long,
    uiState: EarnItUiState,
    viewModel: EarnItViewModel,
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
) {
    val view = LocalView.current
    var name by rememberSaveable { mutableStateOf("") }
    var cost by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var icon by rememberSaveable { mutableStateOf("") }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val taskState = remember { mutableStateMapOf<Long, TaskEditState>() }
    var taskStateReady by remember { mutableStateOf(false) }
    var awaitingNewTask by rememberSaveable { mutableStateOf(false) }
    val pendingTaskId by viewModel.pendingTaskId.collectAsState()
    val pendingRewardId by viewModel.pendingRewardId.collectAsState()
    var pendingRewardSaveNav by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val isNew = rewardId == 0L

    LaunchedEffect(uiState.tasks, uiState.rewardProgressList) {
        val cur = uiState.rewardProgressList.find { it.reward.id == rewardId }
        if (!taskStateReady) {
            if (rewardId != 0L && cur == null) return@LaunchedEffect
            if (name.isEmpty()) name = cur?.reward?.name.orEmpty()
            if (cost.isEmpty()) cost = cur?.reward?.cost?.toString() ?: "10"
            if (description.isEmpty()) description = cur?.reward?.description.orEmpty()
            if (icon.isEmpty()) icon = cur?.reward?.icon.orEmpty()
            uiState.tasks.forEach { task ->
                val m = cur?.mandatoryTasks?.any { it.id == task.id } == true
                val o = cur?.optionalTasks?.any { it.id == task.id } == true
                val r = cur?.taskRefs?.find { it.taskId == task.id }?.isRepeatable ?: false
                taskState[task.id] = TaskEditState(m || o, m, r)
            }
            taskStateReady = true
        } else {
            uiState.tasks.forEach { task ->
                if (!taskState.containsKey(task.id)) {
                    val m = cur?.mandatoryTasks?.any { it.id == task.id } == true
                    val o = cur?.optionalTasks?.any { it.id == task.id } == true
                    val r = cur?.taskRefs?.find { it.taskId == task.id }?.isRepeatable ?: false
                    taskState[task.id] = TaskEditState(m || o, m, r)
                }
            }
        }
    }

    // Auto-include newly created task — only when THIS form initiated the creation.
    LaunchedEffect(pendingTaskId, taskStateReady) {
        val id = pendingTaskId
        if (awaitingNewTask && id != null && taskStateReady) {
            awaitingNewTask = false
            taskState[id] = TaskEditState(included = true)
            viewModel.consumePendingTaskId()
        }
    }

    LaunchedEffect(pendingRewardId) {
        if (pendingRewardSaveNav && pendingRewardId != null) {
            pendingRewardSaveNav = false
            val newId = pendingRewardId!!
            viewModel.consumePendingRewardId()
            navController.navigate(Screen.RewardDetail.route(newId)) {
                popUpTo(Screen.RewardEdit.route) { inclusive = true }
            }
        }
    }

    if (showAddTaskDialog) {
        AddTaskToRewardDialog(
            allTasks = uiState.tasks,
            includedTaskIds =
                taskState.entries
                    .filter { it.value.included }
                    .map { it.key }
                    .toSet(),
            onDismiss = { showAddTaskDialog = false },
            onAddTasks = { tasks ->
                tasks.forEach { (task, flags) ->
                    taskState[task.id] = flags.copy(included = true)
                }
                showAddTaskDialog = false
            },
            onCreateNew = {
                awaitingNewTask = true
                showAddTaskDialog = false
                navController.navigate(Screen.TaskEdit.route(0L, rewardId, name))
            },
            onBrowseLibrary = {
                showAddTaskDialog = false
                navController.navigate(Screen.TaskLibrary.route)
            },
        )
    }
    if (showIconPicker) {
        EmojiPickerDialog(
            current = icon,
            onPick = {
                icon = it
                showIconPicker = false
            },
            onDismiss = { showIconPicker = false },
        )
    }

    val includedTasks = uiState.tasks.filter { taskState[it.id]?.included == true }
    val nameConflict =
        !pendingRewardSaveNav &&
            name.isNotBlank() &&
            uiState.rewardProgressList.any {
                it.reward.name
                    .trim()
                    .equals(name.trim(), ignoreCase = true) &&
                    it.reward.id != rewardId
            }
    val canSave = name.isNotBlank() && !nameConflict

    if (showDeleteDialog) {
        val rewardName =
            uiState.rewardProgressList
                .find { it.reward.id == rewardId }
                ?.reward
                ?.name ?: name
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(Strings.REWARD_DELETE_TITLE, color = MaterialTheme.colorScheme.primary) },
            text = { Text(Strings.rewardDeleteBody(rewardName)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteReward(rewardId) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White,
                        ),
                ) {
                    Text(
                        "DELETE",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                }) {
                    Text(
                        "CANCEL",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            },
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
    ) {
        // ── Title bar ─────────────────────────────────────────────────────────
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.BACK_DESC)
            }
            Text(
                if (isNew) Strings.REWARD_EDIT_NEW else Strings.REWARD_EDIT_EXISTING,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (!isNew) {
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = Strings.DELETE_REWARD_DESC,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        // ── Scrollable content ────────────────────────────────────────────────
        LazyColumn(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { showIconPicker = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(56.dp),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(
                            if (icon.isNotEmpty()) icon else "🎯",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(Strings.REWARD_NAME_LABEL) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        isError = nameConflict,
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            ),
                    )
                }
                if (nameConflict) {
                    Text(
                        Strings.rewardDuplicateError(name.trim()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it.filter { c -> c.isDigit() } },
                    label = { Text(Strings.REWARD_COST_LABEL) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        ),
                )
            }
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(Strings.REWARD_DESC_LABEL) },
                    placeholder = { Text(Strings.REWARD_DESC_PLACEHOLDER) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        ),
                )
            }
            if (includedTasks.isNotEmpty()) {
                item {
                    Text(
                        Strings.REWARD_TASKS_SECTION,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                items(includedTasks, key = { it.id }) { task ->
                    val state = taskState[task.id] ?: TaskEditState()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = true,
                                onCheckedChange = {
                                    taskState[task.id] =
                                        state.copy(included = false, isMandatory = false, isRepeatable = false)
                                },
                                colors =
                                    CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        uncheckedColor = MaterialTheme.colorScheme.secondary,
                                        checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                            )
                            Text(task.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            IconButton(
                                onClick = { taskState[task.id] = state.copy(isMandatory = !state.isMandatory) },
                            ) {
                                Icon(
                                    if (state.isMandatory) Icons.Default.Star else Icons.Outlined.Star,
                                    contentDescription = if (state.isMandatory) Strings.REWARD_MANDATORY_DESC else Strings.REWARD_OPTIONAL_DESC,
                                    tint =
                                        if (state.isMandatory) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        },
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            IconButton(
                                onClick = { taskState[task.id] = state.copy(isRepeatable = !state.isRepeatable) },
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = if (state.isRepeatable) Strings.REWARD_REPEATABLE_DESC else Strings.REWARD_NOT_REPEATABLE_DESC,
                                    tint =
                                        if (state.isRepeatable) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        },
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { showAddTaskDialog = true },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(Strings.REWARD_ADD_TASK_BTN)
                }
            }
        }

        // ── Fixed bottom buttons ──────────────────────────────────────────────
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EarnItOutlinedButton(
                text = "CANCEL",
                onClick = { navController.popBackStack() },
                modifier = Modifier.weight(1f),
            )
            EarnItPrimaryButton(
                text = "SAVE",
                enabled = canSave,
                modifier = Modifier.weight(2f),
                onClick = {
                    view.hapticTap()
                    val taskTriples =
                        taskState.entries
                            .filter { it.value.included }
                            .map { (id, s) -> Triple(id, s.isMandatory, s.isRepeatable) }
                    viewModel.saveReward(rewardId, name, cost.toIntOrNull() ?: 10, description, icon, taskTriples)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(Strings.REWARD_SAVED, duration = SnackbarDuration.Short)
                    }
                    if (!isNew) {
                        navController.popBackStack()
                    } else {
                        pendingRewardSaveNav = true
                    }
                },
            )
        }
    }
}
