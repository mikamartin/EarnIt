@file:OptIn(ExperimentalMaterial3Api::class)

package com.earnit.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.earnit.app.data.EarnItUiState
import com.earnit.app.ui.theme.LocalEarnItAccents
import com.earnit.app.ui.theme.cardSurface
import com.earnit.app.viewmodel.EarnItViewModel

@Composable
fun TaskDetailScreen(
    taskId: Long,
    uiState: EarnItUiState,
    viewModel: EarnItViewModel,
    navController: NavHostController,
) {
    val task = uiState.tasks.find { it.id == taskId } ?: return
    val accents = LocalEarnItAccents.current
    val rewardsForTask = uiState.rewardProgressList.filter { rp -> rp.allTasks.any { it.id == taskId } }
    val logsForTask = uiState.allLogs.filter { it.taskId == taskId }
    var showLogDialog by remember { mutableStateOf(false) }
    var isRewardsExpanded by remember { mutableStateOf(true) }
    var isActivityExpanded by remember { mutableStateOf(true) }
    val logEnabled = rewardsForTask.isNotEmpty()

    if (showLogDialog) {
        LogForRewardDialog(
            task = task,
            rewardsForTask = rewardsForTask,
            viewModel = viewModel,
            onDismiss = { showLogDialog = false },
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.cardSurface)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Name + Edit ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (task.icon.isNotEmpty()) {
                        Text(task.icon, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        task.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .shadow(4.dp, RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.secondary)
                            .clickable { navController.navigate(Screen.TaskEdit.route(taskId, 0L)) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Edit,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White,
                    )
                }
            }

            // ── Group chip ──
            if (task.group != null) {
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        task.group,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            // ── Points + LOG button ──
            val taskPts = task.effectivePoints()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        Strings.TASK_DETAIL_POINTS_LABEL,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "+$taskPts",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                LogPillButton(
                    label = Strings.LOG_BTN,
                    accentColor = accents.gradientStart,
                    enabled = logEnabled,
                    onClick = { showLogDialog = true },
                )
            }

            HorizontalDivider(color = Color(0xFFD5C9B0), thickness = 1.dp)

            // ── Used in Rewards (collapsible) ──
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { isRewardsExpanded = !isRewardsExpanded }
                        .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Used in rewards${if (rewardsForTask.isNotEmpty()) " (${rewardsForTask.size})" else ""}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Icon(
                    if (isRewardsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
            if (isRewardsExpanded) {
                if (rewardsForTask.isEmpty()) {
                    Text(
                        Strings.TASK_DETAIL_NO_REWARDS,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        rewardsForTask.forEach { rp ->
                            val ref = rp.taskRefs.find { it.taskId == taskId }
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { navController.navigate(Screen.RewardDetail.route(rp.reward.id)) }
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    Icons.Default.CardGiftcard,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(rp.reward.name, style = MaterialTheme.typography.bodyMedium)
                                    if (ref?.isMandatory == true) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(13.dp),
                                        )
                                    }
                                    if (ref?.isRepeatable == true) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(13.dp),
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFD5C9B0), thickness = 1.dp)

            // ── Recent activity (collapsible) ──
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { isActivityExpanded = !isActivityExpanded }
                        .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Recent activity${if (logsForTask.isNotEmpty()) " (${logsForTask.size})" else ""}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Icon(
                    if (isActivityExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
            if (isActivityExpanded) {
                if (logsForTask.isEmpty()) {
                    Text(
                        Strings.NO_ACTIVITY,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        logsForTask.sortedByDescending { it.timestamp }.forEach { log ->
                            val rewardName =
                                uiState.rewardProgressList
                                    .find { it.reward.id == log.rewardId }
                                    ?.reward
                                    ?.name
                                    ?: uiState.historyEntries
                                        .find { it.entry.id == log.historyEntryId }
                                        ?.entry
                                        ?.rewardName
                                    ?: "Unknown reward"
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        rewardName,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF8E7CC3),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        formatTimestamp(log.timestamp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    )
                                    Spacer(Modifier.width(8.dp))
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
        }
    }
}
