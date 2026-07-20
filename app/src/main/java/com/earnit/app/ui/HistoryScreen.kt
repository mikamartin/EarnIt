@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.earnit.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.earnit.app.data.EarnItUiState
import com.earnit.app.ui.theme.LocalEarnItAccents
import com.earnit.app.viewmodel.EarnItViewModel

@Composable
fun HistoryScreen(
    uiState: EarnItUiState,
    viewModel: EarnItViewModel,
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 4.dp)) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(Strings.HISTORY_TAB_TASKS) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(Strings.HISTORY_TAB_REWARDS) },
                )
            }
        }
        when (selectedTab) {
            0 -> CompletedTasksTab(uiState)
            1 -> ClaimedRewardsTab(uiState, viewModel)
        }
    }
}

@Composable
private fun CompletedTasksTab(uiState: EarnItUiState) {
    val taskMap = remember(uiState.tasks) { uiState.tasks.associate { it.id to it.name } }
    val rewardNameMap =
        remember(uiState.rewardProgressList, uiState.historyEntries) {
            buildMap {
                uiState.rewardProgressList.forEach { put(it.reward.id, it.reward.name) }
                uiState.historyEntries.forEach { put(it.entry.rewardId, it.entry.rewardName) }
            }
        }
    val sortedLogs = remember(uiState.allLogs) { uiState.allLogs.sortedByDescending { it.timestamp } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (sortedLogs.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    Text(
                        Strings.HISTORY_NO_TASKS,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(sortedLogs) { log ->
            val taskName = log.taskName.ifEmpty { taskMap[log.taskId] ?: "Unknown task" }
            val rewardName = rewardNameMap[log.rewardId]
            EarnItSectionCard(shape = RoundedCornerShape(12.dp)) {
                Column(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            taskName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF8E7CC3),
                            modifier = Modifier.weight(1f).paddingFromBaseline(0.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "+${log.points}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    val meta =
                        buildString {
                            append(formatLogTime(log.timestamp))
                            if (!rewardName.isNullOrBlank()) append("  ·  $rewardName")
                        }
                    Text(
                        meta,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                    if (log.detail.isNotBlank()) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Default.Edit,
                                null,
                                modifier = Modifier.size(10.dp).padding(top = 1.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                log.detail,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClaimedRewardsTab(
    uiState: EarnItUiState,
    viewModel: EarnItViewModel,
) {
    val hofPalette = LocalEarnItAccents.current.cardPalette
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (uiState.historyEntries.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    Text(
                        Strings.HISTORY_NO_REWARDS,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        itemsIndexed(uiState.historyEntries) { index, entry ->
            val accent = hofPalette[index % hofPalette.size]
            EarnItSectionCard {
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    Box(modifier = Modifier.width(5.dp).fillMaxHeight().background(accent))
                    Column(
                        modifier = Modifier.weight(1f).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                if (entry.entry.rewardIcon.isNotEmpty()) {
                                    Text(entry.entry.rewardIcon, style = MaterialTheme.typography.titleMedium)
                                }
                                Column {
                                    Text(
                                        entry.entry.rewardName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = accent,
                                    )
                                    Text(
                                        "+${entry.entry.pointCost} · ${formatDate(entry.entry.claimedAt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Box(
                                modifier =
                                    Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.secondary)
                                        .clickable { viewModel.copyRewardFromEntry(entry.entry.id) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    Strings.HISTORY_EARN_AGAIN,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                        }
                        if (entry.logs.isNotEmpty()) {
                            HorizontalDivider()
                            var logsExpanded by remember { mutableStateOf(false) }
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { logsExpanded = !logsExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    Strings.historySection(entry.logs.size),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Icon(
                                    if (logsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (logsExpanded) {
                                entry.logs.sortedByDescending { it.timestamp }.forEach { log ->
                                    val taskName =
                                        log.taskName.ifEmpty {
                                            uiState.tasks.find { it.id == log.taskId }?.name
                                                ?: "Unknown task"
                                        }
                                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                taskName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF8E7CC3),
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                formatTimestamp(log.timestamp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            )
                                            Spacer(Modifier.weight(1f))
                                            Text(
                                                "+${log.points}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                        if (log.detail.isNotBlank()) {
                                            Row(verticalAlignment = Alignment.Top) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    null,
                                                    modifier = Modifier.size(10.dp).padding(top = 1.dp),
                                                    tint =
                                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                            alpha = 0.55f,
                                                        ),
                                                )
                                                Spacer(Modifier.width(3.dp))
                                                Text(
                                                    log.detail,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
