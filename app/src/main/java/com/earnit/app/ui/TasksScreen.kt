@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.earnit.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.earnit.app.data.AppSettings
import com.earnit.app.data.EarnItUiState
import com.earnit.app.data.RewardProgress
import com.earnit.app.data.TaskEntity
import com.earnit.app.ui.theme.LocalEarnItAccents
import com.earnit.app.ui.theme.cardSurface
import com.earnit.app.viewmodel.EarnItViewModel

@Composable
fun TasksScreen(
    uiState: EarnItUiState,
    settings: AppSettings,
    viewModel: EarnItViewModel,
    navController: NavHostController,
) {
    val accents = LocalEarnItAccents.current
    val taskPalette = accents.cardPalette
    val groupView = settings.tasksGroupView
    val isEmpty = uiState.tasks.isEmpty()
    val emptyPulse = rememberInfiniteTransition(label = "emptyPulse")
    val pulseScale by emptyPulse.animateFloat(
        initialValue = 1f,
        targetValue = if (isEmpty) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseScale",
    )
    val reorderedList = remember(uiState.tasks) { mutableStateListOf(*uiState.tasks.toTypedArray()) }
    val listState = rememberLazyListState()
    var isDragging by remember { mutableStateOf(false) }
    var draggingTaskId by remember { mutableStateOf<Long?>(null) }
    var draggingIndex by remember { mutableStateOf(-1) }
    var query by rememberSaveable { mutableStateOf("") }
    val showSearch = uiState.tasks.size > 15
    val displayList =
        if (showSearch && query.isNotBlank()) {
            reorderedList.filter { it.name.contains(query, ignoreCase = true) }
        } else {
            reorderedList
        }
    val collapsedGroups = remember { mutableStateMapOf<String?, Boolean>() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = { viewModel.toggleTasksGroupView() }) {
                    Icon(
                        if (groupView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.ViewAgenda,
                        contentDescription = if (groupView) Strings.TASKS_VIEW_ALL_DESC else Strings.TASKS_VIEW_GROUP_DESC,
                        tint = if (groupView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                TextButton(
                    onClick = { navController.navigate(Screen.TaskLibrary.route) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.scale(if (isEmpty) pulseScale else 1f),
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.LibraryBooks,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(Strings.TASKS_LIBRARY_BTN, style = MaterialTheme.typography.labelMedium)
                }
            }
            if (showSearch) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(Strings.TASKS_SEARCH_HINT) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
            if (uiState.tasks.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("⚔️", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        Strings.TASKS_EMPTY_TITLE,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        Strings.TASKS_EMPTY_BODY,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else if (groupView) {
                val groupedTasks = displayList.groupBy { it.group }
                val namedGroups = groupedTasks.keys.filterNotNull().sorted()
                val noGroupsSetUp = namedGroups.isEmpty()
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    namedGroups.forEachIndexed { groupIndex, groupName ->
                        val groupTasks = groupedTasks[groupName] ?: emptyList()
                        // First group expands by default; all others start collapsed
                        val isCollapsed = collapsedGroups[groupName] ?: (groupIndex != 0)
                        item(key = "header_$groupName") {
                            CollapsibleGroupHeader(
                                title = groupName,
                                isCollapsed = isCollapsed,
                                onToggle = { collapsedGroups[groupName] = !isCollapsed },
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                        if (!isCollapsed) {
                            itemsIndexed(groupTasks, key = { _, t -> t.id }) { index, task ->
                                TaskCard(
                                    task = task,
                                    accent = taskPalette[index % taskPalette.size],
                                    onClick = { navController.navigate(Screen.TaskDetail.route(task.id)) },
                                )
                            }
                        }
                    }
                    val ungroupedTasks = groupedTasks[null] ?: emptyList()
                    // "Other" expands by default only when it is the sole section
                    val isOtherCollapsed = collapsedGroups[null] ?: namedGroups.isNotEmpty()
                    if (ungroupedTasks.isNotEmpty() || noGroupsSetUp) {
                        item(key = "header_other") {
                            Column {
                                CollapsibleGroupHeader(
                                    title = Strings.TASKS_GROUP_OTHER,
                                    isCollapsed = isOtherCollapsed,
                                    onToggle = { collapsedGroups[null] = !isOtherCollapsed },
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                                if (noGroupsSetUp) {
                                    Text(
                                        Strings.TASKS_GROUP_ASSIGN_HINT,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 4.dp),
                                    )
                                }
                            }
                        }
                        if (!isOtherCollapsed) {
                            itemsIndexed(ungroupedTasks, key = { _, t -> t.id }) { index, task ->
                                TaskCard(
                                    task = task,
                                    accent = taskPalette[index % taskPalette.size],
                                    onClick = { navController.navigate(Screen.TaskDetail.route(task.id)) },
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(displayList, key = { _, task -> task.id }) { index, task ->
                        val accent = taskPalette[index % taskPalette.size]
                        var accumulatedDragY by remember { mutableStateOf(0f) }
                        val cardModifier =
                            Modifier
                                .animateItem()
                                .pointerInput(task.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            isDragging = true
                                            draggingTaskId = task.id
                                            draggingIndex = reorderedList.indexOfFirst { it.id == task.id }
                                            accumulatedDragY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            accumulatedDragY += dragAmount.y
                                            val currentIdx = draggingIndex
                                            if (currentIdx >= 0) {
                                                val visibleItems = listState.layoutInfo.visibleItemsInfo
                                                val draggedInfo = visibleItems.find { it.index == currentIdx }
                                                if (draggedInfo != null) {
                                                    val center =
                                                        draggedInfo.offset + draggedInfo.size / 2 + accumulatedDragY
                                                    val target =
                                                        DragReorder.targetIndex(
                                                            draggingIndex = currentIdx,
                                                            dragCenter = center,
                                                            visibleItems =
                                                                visibleItems.map {
                                                                    DragReorder.ItemBounds(it.index, it.offset, it.size)
                                                                },
                                                        )
                                                    if (target != null) {
                                                        val newOrder = DragReorder.reordered(reorderedList, currentIdx, target)
                                                        reorderedList.clear()
                                                        reorderedList.addAll(newOrder)
                                                        draggingIndex = target
                                                        accumulatedDragY = 0f
                                                    }
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            isDragging = false
                                            draggingTaskId = null
                                            draggingIndex = -1
                                            accumulatedDragY = 0f
                                            viewModel.updateTasksOrder(reorderedList.map { it.id })
                                        },
                                        onDragCancel = {
                                            isDragging = false
                                            draggingTaskId = null
                                            draggingIndex = -1
                                            accumulatedDragY = 0f
                                        },
                                    )
                                }

                        TaskCard(
                            task = task,
                            accent = accent,
                            onClick = { if (!isDragging) navController.navigate(Screen.TaskDetail.route(task.id)) },
                            modifier = cardModifier,
                            isDragging = draggingTaskId == task.id,
                        )
                    }
                }
            }
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .scale(if (isEmpty) pulseScale else 1f)
                    .size(56.dp)
                    .shadow(6.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { navController.navigate(Screen.TaskEdit.route(0L, 0L)) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = Strings.NEW_TASK_DESC,
                tint = Color.White,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
fun LogForRewardDialog(
    task: TaskEntity,
    rewardsForTask: List<RewardProgress>,
    viewModel: EarnItViewModel,
    onDismiss: () -> Unit,
) {
    var selectedId by remember {
        mutableStateOf(if (rewardsForTask.size == 1) rewardsForTask[0].reward.id else -1L)
    }
    var detail by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.tasksLogTitle(task.name), color = MaterialTheme.colorScheme.primary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (rewardsForTask.size > 1) {
                    Text(
                        Strings.TASKS_LOG_SELECT_REWARD,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    rewardsForTask.forEach { rp ->
                        RadioRow(
                            label = rp.reward.name,
                            selected = selectedId == rp.reward.id,
                            onClick = { selectedId = rp.reward.id },
                        )
                    }
                } else {
                    Text(
                        Strings.tasksLogSingleReward(rewardsForTask[0].reward.name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = acceptWithinLimit(detail, it, NOTE_MAX_CHARS) },
                    label = { Text(Strings.TASKS_LOG_NOTE_LABEL) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4,
                    supportingText = { Text("${detail.length}/$NOTE_MAX_CHARS") },
                )
            }
        },
        confirmButton = {
            EarnItPrimaryButton(
                text = "LOG",
                enabled = selectedId != -1L,
                onClick = {
                    if (selectedId != -1L) {
                        viewModel.logTask(task, selectedId, detail)
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
private fun TaskCard(
    task: TaskEntity,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 2.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.cardSurface,
            ),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.width(5.dp).fillMaxHeight().background(accent))
            Row(
                modifier = Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (task.icon.isNotEmpty()) {
                    Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                        Text(task.icon, style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    task.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = accent,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    task.displayPoints(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
