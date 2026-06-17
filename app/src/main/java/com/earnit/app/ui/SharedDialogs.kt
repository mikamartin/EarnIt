@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.earnit.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.earnit.app.data.RewardProgress
import com.earnit.app.data.TaskEntity
import com.earnit.app.viewmodel.EarnItViewModel

@Composable
fun LogTaskDialog(
    rp: RewardProgress,
    viewModel: EarnItViewModel,
    notesMandatory: Boolean = false,
    onDismiss: () -> Unit,
) {
    var selectedTask by remember { mutableStateOf<TaskEntity?>(null) }
    var detail by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    val logEnabled = selectedTask != null && (!notesMandatory || detail.isNotBlank())

    val filteredTasks =
        remember(search, rp.loggableTasks) {
            if (search.isBlank()) {
                rp.loggableTasks
            } else {
                rp.loggableTasks.filter { it.name.contains(search, ignoreCase = true) }
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.LOG_DIALOG_TITLE, color = MaterialTheme.colorScheme.primary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (rp.loggableTasks.size > 5) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        label = { Text(Strings.LOG_SEARCH_LABEL) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon =
                            if (search.isNotEmpty()) {
                                {
                                    IconButton(
                                        onClick = { search = "" },
                                    ) { Icon(Icons.Default.Clear, contentDescription = Strings.CLEAR_DESC) }
                                }
                            } else {
                                null
                            },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(2.dp))
                }
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(filteredTasks) { task ->
                        val isMandatory = rp.mandatoryTasks.any { it.id == task.id }
                        val isRepeatable = rp.taskRefs.find { it.taskId == task.id }?.isRepeatable ?: false
                        val pts = task.effectivePoints()
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth().clickable { selectedTask = task },
                        ) {
                            RadioButton(
                                selected = selectedTask?.id == task.id,
                                onClick = null,
                                modifier = Modifier.padding(top = 2.dp).size(24.dp),
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                            )
                            Text(
                                task.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f).padding(top = 2.dp, start = 8.dp),
                            )
                            Row(
                                modifier = Modifier.padding(top = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (isMandatory) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = Strings.REWARD_MANDATORY_DESC,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp),
                                    )
                                }
                                if (isRepeatable) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = Strings.REWARD_REPEATABLE_DESC,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(15.dp).padding(start = 3.dp),
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "+$pts",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 4.dp),
                                )
                            }
                        }
                    }
                    if (filteredTasks.isEmpty()) {
                        item {
                            Text(
                                Strings.logNoMatch(search),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                            )
                        }
                    }
                }
                if (selectedTask != null || notesMandatory) {
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = detail,
                        onValueChange = { if (it.length <= NOTE_MAX_CHARS) detail = it },
                        label = {
                            Text(
                                if (notesMandatory) Strings.LOG_NOTE_MANDATORY_LABEL else Strings.LOG_NOTE_LABEL,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 4,
                        enabled = selectedTask != null,
                        supportingText = { Text("${detail.length}/$NOTE_MAX_CHARS") },
                    )
                }
            }
        },
        confirmButton = {
            EarnItPrimaryButton(
                text = "LOG",
                enabled = logEnabled,
                onClick = {
                    selectedTask?.let { task ->
                        viewModel.logTask(task, rp.reward.id, detail)
                        onDismiss()
                    }
                },
            )
        },
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
fun ClaimDialog(
    rewardName: String,
    onStartOver: () -> Unit,
    onArchive: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("🏆", fontSize = 40.sp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "Claim \"$rewardName\"",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    Strings.CLAIM_DIALOG_SUBTITLE,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onStartOver() }
                            .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        Strings.CLAIM_START_OVER_BTN,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                            .clickable { onArchive() }
                            .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        Strings.CLAIM_ARCHIVE_BTN,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        Strings.CLAIM_CANCEL,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

data class TaskEditState(
    val included: Boolean = false,
    val isMandatory: Boolean = false,
    val isRepeatable: Boolean = false,
)

@Composable
fun AddTaskToRewardDialog(
    allTasks: List<TaskEntity>,
    includedTaskIds: Set<Long>,
    onDismiss: () -> Unit,
    onAddTasks: (List<Pair<TaskEntity, TaskEditState>>) -> Unit,
    onCreateNew: () -> Unit,
    onBrowseLibrary: () -> Unit,
) {
    val availableTasks = allTasks.filter { it.id !in includedTaskIds }
    val selected = remember { mutableStateMapOf<Long, Boolean>() }
    val taskFlags = remember { mutableStateMapOf<Long, TaskEditState>() }
    var query by remember { mutableStateOf("") }
    var groupView by remember { mutableStateOf(false) }
    val hasGroups = availableTasks.any { !it.group.isNullOrEmpty() }
    val filtered =
        if (query.isBlank()) {
            availableTasks
        } else {
            availableTasks.filter { it.name.contains(query, ignoreCase = true) }
        }

    val checkboxColors =
        CheckboxDefaults.colors(
            checkedColor = MaterialTheme.colorScheme.primary,
            uncheckedColor = MaterialTheme.colorScheme.secondary,
            checkmarkColor = MaterialTheme.colorScheme.onPrimary,
        )

    @Composable
    fun TaskRow(
        task: TaskEntity,
        showGroup: Boolean = true,
    ) {
        val isChecked = selected[task.id] == true
        val flags = taskFlags[task.id] ?: TaskEditState()
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { selected[task.id] = it },
                colors = checkboxColors,
                modifier = Modifier.padding(top = 10.dp),
            )
            Column(modifier = Modifier.weight(1f).padding(top = 14.dp)) {
                Text(task.name, style = MaterialTheme.typography.bodyMedium)
                if (showGroup && !task.group.isNullOrEmpty()) {
                    Text(
                        task.group,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                    )
                }
            }
            if (isChecked) {
                IconButton(
                    onClick = { taskFlags[task.id] = flags.copy(isMandatory = !flags.isMandatory) },
                    modifier = Modifier.size(32.dp).padding(top = 10.dp),
                ) {
                    Icon(
                        if (flags.isMandatory) Icons.Default.Star else Icons.Outlined.Star,
                        contentDescription = if (flags.isMandatory) Strings.REWARD_MANDATORY_DESC else Strings.REWARD_OPTIONAL_DESC,
                        tint =
                            if (flags.isMandatory) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            },
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(
                    onClick = { taskFlags[task.id] = flags.copy(isRepeatable = !flags.isRepeatable) },
                    modifier = Modifier.size(32.dp).padding(top = 10.dp),
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = if (flags.isRepeatable) Strings.REWARD_REPEATABLE_DESC else Strings.REWARD_NOT_REPEATABLE_DESC,
                        tint =
                            if (flags.isRepeatable) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            },
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Text(
                task.displayPoints(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Strings.ADD_TASK_DIALOG_TITLE,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                if (hasGroups) {
                    IconButton(onClick = { groupView = !groupView }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (groupView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.ViewAgenda,
                            contentDescription = if (groupView) Strings.ADD_TASK_GROUP_VIEW_DESC else Strings.ADD_TASK_ALL_VIEW_DESC,
                            tint = if (groupView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (availableTasks.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        Text(
                            Strings.ADD_TASK_EMPTY,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row {
                            TextButton(onClick = onCreateNew) {
                                Text(Strings.ADD_TASK_CREATE, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                "or",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                            TextButton(onClick = onBrowseLibrary) {
                                Text(Strings.ADD_TASK_BROWSE, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } else {
                    if (availableTasks.size > 7) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text(Strings.TASKS_SEARCH_HINT) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                ),
                            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                            trailingIcon =
                                if (query.isNotEmpty()) {
                                    {
                                        IconButton(onClick = { query = "" }) {
                                            Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                } else {
                                    null
                                },
                        )
                    }
                    if (groupView && hasGroups) {
                        val groupedTasks = filtered.groupBy { it.group }
                        val namedGroups = groupedTasks.keys.filterNotNull().sorted()
                        val ungrouped = groupedTasks[null] ?: emptyList()
                        val collapsedSections = remember { mutableStateMapOf<String?, Boolean>() }
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            namedGroups.forEach { groupName ->
                                val groupTasks = groupedTasks[groupName] ?: emptyList()
                                val isCollapsed = collapsedSections[groupName] ?: true
                                val allChecked = groupTasks.all { selected[it.id] == true }
                                item(key = "h_$groupName") {
                                    CollapsibleGroupHeader(
                                        title = groupName,
                                        isCollapsed = isCollapsed,
                                        onToggle = { collapsedSections[groupName] = !isCollapsed },
                                        leadingContent = {
                                            Checkbox(
                                                checked = allChecked,
                                                onCheckedChange = {
                                                    groupTasks.forEach { t ->
                                                        selected[t.id] =
                                                            !allChecked
                                                    }
                                                },
                                                colors = checkboxColors,
                                            )
                                        },
                                    )
                                }
                                if (!isCollapsed) {
                                    items(groupTasks, key = { it.id }) { task ->
                                        Box(
                                            modifier = Modifier.padding(start = 8.dp),
                                        ) { TaskRow(task, showGroup = false) }
                                    }
                                }
                            }
                            if (ungrouped.isNotEmpty()) {
                                val isCollapsed = collapsedSections[null] ?: true
                                val allChecked = ungrouped.all { selected[it.id] == true }
                                item(key = "h_other") {
                                    CollapsibleGroupHeader(
                                        title = Strings.TASKS_GROUP_OTHER,
                                        isCollapsed = isCollapsed,
                                        onToggle = { collapsedSections[null] = !isCollapsed },
                                        leadingContent = {
                                            Checkbox(
                                                checked = allChecked,
                                                onCheckedChange = {
                                                    ungrouped.forEach { t ->
                                                        selected[t.id] =
                                                            !allChecked
                                                    }
                                                },
                                                colors = checkboxColors,
                                            )
                                        },
                                    )
                                }
                                if (!isCollapsed) {
                                    items(ungrouped, key = { it.id }) { task ->
                                        Box(
                                            modifier = Modifier.padding(start = 8.dp),
                                        ) { TaskRow(task, showGroup = false) }
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                            items(filtered, key = { it.id }) { task -> TaskRow(task) }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                }
                OutlinedButton(
                    onClick = onCreateNew,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        Strings.ADD_TASK_CREATE_BTN,
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        },
        confirmButton = {
            EarnItPrimaryButton(
                text = Strings.ADD_TASK_CONFIRM_BTN,
                enabled = selected.values.any { it },
                onClick = {
                    val result =
                        availableTasks
                            .filter { selected[it.id] == true }
                            .map { task -> task to (taskFlags[task.id] ?: TaskEditState(included = true)) }
                    onAddTasks(result)
                },
            )
        },
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
