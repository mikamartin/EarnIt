@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.earnit.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.earnit.app.data.TaskEntity
import com.earnit.app.viewmodel.EarnItViewModel
import kotlinx.coroutines.launch

@Composable
fun TaskEditScreen(
    taskId: Long,
    fromRewardId: Long,
    uiState: EarnItUiState,
    viewModel: EarnItViewModel,
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
) {
    val existing = uiState.tasks.find { it.id == taskId }
    val view = LocalView.current
    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var icon by rememberSaveable { mutableStateOf(existing?.icon.orEmpty()) }
    var useAuto by rememberSaveable { mutableStateOf(existing?.useAutoPoints ?: false) }
    var points by rememberSaveable { mutableStateOf(existing?.points?.toString() ?: "4") }
    var time by rememberSaveable { mutableStateOf(existing?.time ?: 1) }
    var difficulty by rememberSaveable { mutableStateOf(existing?.difficulty ?: 1) }
    var preparation by rememberSaveable { mutableStateOf(existing?.preparation ?: 1) }
    var group by rememberSaveable { mutableStateOf(existing?.group.orEmpty()) }
    var newGroupText by rememberSaveable { mutableStateOf("") }
    var isGroupExpanded by rememberSaveable { mutableStateOf(true) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val rewardLinkState = remember(taskId) { mutableStateMapOf<Long, TaskEditState>() }
    val isNew = taskId == 0L
    val focusManager = LocalFocusManager.current
    val nameConflict =
        name.isNotBlank() &&
            uiState.tasks.any {
                it.name.trim().equals(name.trim(), ignoreCase = true) && it.id != taskId
            }

    LaunchedEffect(taskId, uiState.rewardProgressList) {
        uiState.rewardProgressList.forEach { rp ->
            if (!rewardLinkState.containsKey(rp.reward.id)) {
                val ref = rp.taskRefs.find { it.taskId == taskId }
                rewardLinkState[rp.reward.id] =
                    if (ref != null) {
                        TaskEditState(included = true, isMandatory = ref.isMandatory, isRepeatable = ref.isRepeatable)
                    } else {
                        TaskEditState()
                    }
            }
        }
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

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(Strings.TASK_DELETE_TITLE, color = MaterialTheme.colorScheme.primary) },
            text = { Text(Strings.taskDeleteBody(existing?.name ?: name)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTask(taskId) {
                            navController.navigate(Screen.Tasks.route) {
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
                if (isNew) Strings.TASK_EDIT_NEW else Strings.TASK_EDIT_EXISTING,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (!isNew) {
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = Strings.DELETE_TASK_DESC,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        // ── Scrollable form ───────────────────────────────────────────────────
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(0.dp))

            // Icon + name
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
                        if (icon.isNotEmpty()) icon else "✅",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Strings.TASK_NAME_LABEL) },
                    placeholder = { Text(Strings.TASK_NAME_PLACEHOLDER) },
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
                    Strings.taskDuplicateError(name.trim()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                )
            }

            val existingGroups =
                remember(uiState.tasks) {
                    uiState.tasks
                        .mapNotNull { it.group }
                        .distinct()
                        .sorted()
                }

            val newGroupFocusRequester = remember { FocusRequester() }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CollapsibleGroupHeader(
                    title =
                        if (newGroupText.isNotBlank()) {
                            Strings.taskGroupLabel(newGroupText)
                        } else if (group.isNotBlank()) {
                            Strings.taskGroupLabel(group)
                        } else {
                            Strings.TASK_GROUP_OPTIONAL
                        },
                    isCollapsed = !isGroupExpanded,
                    onToggle = { isGroupExpanded = !isGroupExpanded },
                )
                if (isGroupExpanded) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    RoundedCornerShape(12.dp),
                                ),
                    ) {
                        existingGroups.forEach { g ->
                            val selected = group == g && newGroupText.isBlank()
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            group = if (selected) "" else g
                                            newGroupText = ""
                                        }.padding(end = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = null,
                                    colors =
                                        RadioButtonDefaults.colors(
                                            selectedColor = MaterialTheme.colorScheme.primary,
                                        ),
                                )
                                Text(g, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        group = ""
                                        newGroupFocusRequester.requestFocus()
                                    }.padding(end = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = newGroupText.isNotBlank(),
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                            )
                            BasicTextField(
                                value = newGroupText,
                                onValueChange = {
                                    newGroupText = it
                                    if (it.isNotBlank()) group = ""
                                },
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .focusRequester(newGroupFocusRequester),
                                singleLine = true,
                                textStyle =
                                    MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                    ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (newGroupText.isEmpty()) {
                                            Text(
                                                Strings.TASK_GROUP_PLACEHOLDER,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                            )
                            if (newGroupText.isNotEmpty()) {
                                IconButton(
                                    onClick = { newGroupText = "" },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = Strings.CLEAR_DESC,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Auto points toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(Strings.TASK_AUTO_POINTS_TOGGLE, style = MaterialTheme.typography.bodyMedium)
                Switch(checked = useAuto, onCheckedChange = { useAuto = it })
            }

            // Points assignment
            if (useAuto) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SliderRow(Strings.TASK_SLIDER_TIME, time) { time = it }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(
                                    1.dp,
                                ).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        )
                        SliderRow(Strings.TASK_SLIDER_DIFFICULTY, difficulty) { difficulty = it }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(
                                    1.dp,
                                ).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        )
                        SliderRow(Strings.TASK_SLIDER_PREPARATION, preparation) { preparation = it }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        Strings.TASK_POINTS_TOTAL,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${viewModel.computeAutoPoints(time, difficulty, preparation)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                OutlinedTextField(
                    value = points,
                    onValueChange = { points = it.filter { c -> c.isDigit() } },
                    label = { Text(Strings.TASK_POINTS_LABEL) },
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

            // Reward association
            if (fromRewardId != 0L) {
                val rewardName =
                    uiState.rewardProgressList
                        .find { it.reward.id == fromRewardId }
                        ?.reward
                        ?.name
                if (rewardName != null) {
                    Text(
                        Strings.taskUsedIn(rewardName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else if (uiState.rewardProgressList.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    Strings.TASK_USE_TO_GET,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                uiState.rewardProgressList.forEach { rp ->
                    val state = rewardLinkState[rp.reward.id] ?: TaskEditState()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        rewardLinkState[rp.reward.id] =
                                            state.copy(
                                                included = !state.included,
                                                isMandatory = if (state.included) false else state.isMandatory,
                                                isRepeatable = if (state.included) false else state.isRepeatable,
                                            )
                                    }.padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = state.included,
                                onCheckedChange = {
                                    rewardLinkState[rp.reward.id] =
                                        state.copy(
                                            included = it,
                                            isMandatory = if (!it) false else state.isMandatory,
                                            isRepeatable = if (!it) false else state.isRepeatable,
                                        )
                                },
                                colors =
                                    CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        uncheckedColor = MaterialTheme.colorScheme.secondary,
                                        checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                            )
                            if (rp.reward.icon.isNotEmpty()) {
                                Text(rp.reward.icon, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                rp.reward.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            IconButton(
                                onClick = {
                                    if (state.included) {
                                        rewardLinkState[rp.reward.id] = state.copy(isMandatory = !state.isMandatory)
                                    }
                                },
                                enabled = state.included,
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(
                                    if (state.isMandatory) Icons.Default.Star else Icons.Outlined.Star,
                                    contentDescription = if (state.isMandatory) Strings.TASK_MANDATORY_DESC else Strings.TASK_OPTIONAL_DESC,
                                    tint =
                                        if (state.isMandatory) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (state.included) {
                                        rewardLinkState[rp.reward.id] = state.copy(isRepeatable = !state.isRepeatable)
                                    }
                                },
                                enabled = state.included,
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = if (state.isRepeatable) Strings.TASK_REPEATABLE_DESC else Strings.TASK_ONCE_DESC,
                                    tint =
                                        if (state.isRepeatable) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
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
            val canSave = name.isNotBlank() && !nameConflict
            EarnItPrimaryButton(
                text = "SAVE",
                enabled = canSave,
                modifier = Modifier.weight(2f),
                onClick = {
                    view.hapticTap()
                    viewModel.saveTask(
                        TaskEntity(
                            id = existing?.id ?: 0L,
                            name = name,
                            repeatable = existing?.repeatable ?: true,
                            points = points.toIntOrNull() ?: 4,
                            useAutoPoints = useAuto,
                            time = time,
                            difficulty = difficulty,
                            preparation = preparation,
                            icon = icon,
                            group = newGroupText.ifBlank { group }.takeIf { it.isNotBlank() },
                        ),
                        rewardLinks =
                            if (fromRewardId != 0L) {
                                mapOf(fromRewardId to Pair(false, false))
                            } else {
                                rewardLinkState
                                    .filter { it.value.included }
                                    .mapValues { (_, s) -> Pair(s.isMandatory, s.isRepeatable) }
                            },
                    )
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(Strings.TASK_SAVED, duration = SnackbarDuration.Short)
                    }
                },
            )
        }
    }
}

private val EmojiList =
    listOf(
        "🎯",
        "🏆",
        "🎁",
        "✈️",
        "🏖️",
        "🎮",
        "🍕",
        "🎬",
        "📚",
        "🎸",
        "💆",
        "🛒",
        "🍦",
        "🎨",
        "💪",
        "🧘",
        "🎶",
        "🌟",
        "💎",
        "🔥",
        "🏃",
        "🛏️",
        "🧹",
        "💻",
        "✍️",
        "🚴",
        "🏊",
        "☕",
        "🌱",
        "⚡",
        "📝",
        "🎭",
        "🦁",
        "🌈",
        "🎪",
        "🍎",
        "🚶",
        "💤",
        "🏋️",
        "🎵",
    )

@Composable
fun EmojiPickerDialog(
    current: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val rows = EmojiList.chunked(5)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.TASK_ICON_PICKER_TITLE) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                items(rows) { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { emoji ->
                            Box(
                                modifier =
                                    Modifier
                                        .size(52.dp)
                                        .clickable { onPick(emoji) }
                                        .background(
                                            if (emoji == current) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                Color.Transparent
                                            },
                                            RoundedCornerShape(10.dp),
                                        ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(emoji, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
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

@Composable
fun SliderRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier.fillMaxWidth().height(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .height(2.dp)
                        .background(primary.copy(alpha = 0.25f)),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                (1..5).forEach { i ->
                    Box(
                        modifier =
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (i == value) primary else surface)
                                .border(2.dp, primary, CircleShape)
                                .clickable { onValueChange(i) },
                    )
                }
            }
        }
    }
}
