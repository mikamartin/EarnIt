@file:OptIn(ExperimentalMaterial3Api::class)

package com.earnit.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.earnit.app.data.EarnItUiState
import com.earnit.app.data.RewardProgress
import com.earnit.app.data.TaskEntity
import com.earnit.app.ui.theme.EarnItAccents
import com.earnit.app.ui.theme.LocalEarnItAccents
import com.earnit.app.ui.theme.cardSurface
import com.earnit.app.viewmodel.EarnItViewModel
import com.earnit.app.widget.hasEarnItWidgetPinned
import com.earnit.app.widget.requestPinEarnItWidget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RewardDetailScreen(
    rp: RewardProgress,
    uiState: EarnItUiState,
    viewModel: EarnItViewModel,
    navController: NavHostController,
    autoOpenAddTask: Boolean = false,
) {
    var showLogDialog by remember { mutableStateOf(false) }
    var showClaimDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(autoOpenAddTask) }
    var isTasksExpanded by remember { mutableStateOf(true) }
    var isActivityExpanded by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val hasWidgetPinned = remember { hasEarnItWidgetPinned(context) }
    val detailSettings by viewModel.settings.collectAsState()

    RewardDetailDialogs(
        rp = rp,
        uiState = uiState,
        viewModel = viewModel,
        navController = navController,
        notesMandatory = detailSettings.notesMandatory,
        showLogDialog = showLogDialog,
        onDismissLogDialog = { showLogDialog = false },
        showClaimDialog = showClaimDialog,
        onDismissClaimDialog = { showClaimDialog = false },
        showAddTaskDialog = showAddTaskDialog,
        onDismissAddTaskDialog = { showAddTaskDialog = false },
    )

    val accents = LocalEarnItAccents.current
    val accentColor = accents.gradientStart
    val logEnabled = rp.loggableTasks.isNotEmpty() && !rp.canClaim
    val showMandatoryHint = !rp.canClaim && rp.totalPoints >= rp.reward.cost
    val showAllTasksLoggedHint = !rp.canClaim && rp.allTasks.isNotEmpty() && rp.loggableTasks.isEmpty()

    val infiniteTransition = rememberInfiniteTransition(label = "claimPulse")
    val claimBorderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "borderAlpha",
    )
    val claimScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "claimScale",
    )

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        val cardShape = RoundedCornerShape(20.dp)
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (rp.canClaim) {
                            Modifier.border(
                                width = 2.dp,
                                brush =
                                    Brush.horizontalGradient(
                                        listOf(
                                            accents.gradientStart.copy(alpha = claimBorderAlpha),
                                            accents.gradientEnd.copy(alpha = claimBorderAlpha),
                                        ),
                                    ),
                                shape = cardShape,
                            )
                        } else {
                            Modifier
                        },
                    ).clip(cardShape)
                    .background(MaterialTheme.cardSurface)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RewardHeaderCard(
                rp = rp,
                accentColor = accentColor,
                onEditClick = { navController.navigate(Screen.RewardEdit.route(rp.reward.id)) },
            )

            RewardProgressBar(rp = rp, accents = accents)

            RewardClaimOrLogButton(
                rp = rp,
                accents = accents,
                claimScale = claimScale,
                logEnabled = logEnabled,
                showMandatoryHint = showMandatoryHint,
                showAllTasksLoggedHint = showAllTasksLoggedHint,
                onClaim = { showClaimDialog = true },
                onLog = { showLogDialog = true },
            )

            if (!rp.canClaim) {
                RewardTasksSection(
                    rp = rp,
                    isExpanded = isTasksExpanded,
                    onToggleExpanded = { isTasksExpanded = !isTasksExpanded },
                    hasWidgetPinned = hasWidgetPinned,
                    widgetNudgeDismissed = detailSettings.widgetNudgeDismissed,
                    onAddTaskClick = { showAddTaskDialog = true },
                    onDismissWidgetNudge = { viewModel.dismissWidgetNudge() },
                    onPinWidget = { requestPinEarnItWidget(context) },
                )
            }

            HorizontalDivider(color = Color(0xFFD5C9B0), thickness = 1.dp)

            RewardActivitySection(
                rp = rp,
                tasks = uiState.tasks,
                isExpanded = isActivityExpanded,
                onToggleExpanded = { isActivityExpanded = !isActivityExpanded },
            )
        }
    }
}

@Composable
private fun RewardDetailDialogs(
    rp: RewardProgress,
    uiState: EarnItUiState,
    viewModel: EarnItViewModel,
    navController: NavHostController,
    notesMandatory: Boolean,
    showLogDialog: Boolean,
    onDismissLogDialog: () -> Unit,
    showClaimDialog: Boolean,
    onDismissClaimDialog: () -> Unit,
    showAddTaskDialog: Boolean,
    onDismissAddTaskDialog: () -> Unit,
) {
    if (showLogDialog) {
        LogTaskDialog(
            rp = rp,
            viewModel = viewModel,
            notesMandatory = notesMandatory,
            onDismiss = onDismissLogDialog,
        )
    }
    if (showClaimDialog) {
        ClaimDialog(
            rewardName = rp.reward.name,
            onStartOver = {
                viewModel.claimReward(rp.reward.id, startOver = true)
                navController.popBackStack()
            },
            onArchive = {
                viewModel.claimReward(rp.reward.id, startOver = false)
                navController.popBackStack()
            },
            onDismiss = onDismissClaimDialog,
        )
    }
    if (showAddTaskDialog) {
        AddTaskToRewardDialog(
            allTasks = uiState.tasks,
            includedTaskIds = rp.allTasks.map { it.id }.toSet(),
            onDismiss = onDismissAddTaskDialog,
            onAddTasks = { tasks ->
                tasks.forEach { (task, flags) ->
                    viewModel.addTaskToReward(rp.reward.id, task.id, flags.isMandatory, flags.isRepeatable)
                }
                onDismissAddTaskDialog()
            },
            onCreateNew = {
                onDismissAddTaskDialog()
                navController.navigate(Screen.TaskEdit.route(0L, rp.reward.id))
            },
            onBrowseLibrary = {
                onDismissAddTaskDialog()
                navController.navigate(Screen.TaskLibrary.route)
            },
        )
    }
}

@Composable
private fun RewardHeaderCard(
    rp: RewardProgress,
    accentColor: Color,
    onEditClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (rp.reward.icon.isNotEmpty()) {
                Text(rp.reward.icon, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                rp.reward.name,
                style = MaterialTheme.typography.titleMedium,
                color = accentColor,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            rp.mandatoryTasks.forEach { task ->
                val done = rp.activeLogs.any { it.taskId == task.id }
                Icon(
                    if (done) Icons.Default.Star else Icons.Outlined.Star,
                    contentDescription = null,
                    tint =
                        if (done) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        },
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        if (rp.canClaim) {
            Text(
                "${rp.totalPoints} / ${rp.reward.cost}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .shadow(4.dp, RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.secondary)
                        .clickable(onClick = onEditClick)
                        .semantics { contentDescription = Strings.EDIT_REWARD_DESC }
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
    }

    if (rp.reward.description.isNotEmpty()) {
        Text(
            rp.reward.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RewardProgressBar(
    rp: RewardProgress,
    accents: EarnItAccents,
) {
    val accentColor = accents.gradientStart
    val rawProgress = (rp.totalPoints.toFloat() / rp.reward.cost.toFloat()).coerceIn(0f, 1f)
    val progress by animateFloatAsState(rawProgress, tween(500, easing = FastOutSlowInEasing), label = "barProgress")

    val floatingOffset = remember { Animatable(0f) }
    val floatingAlpha = remember { Animatable(0f) }
    var floatingPoints by remember { mutableIntStateOf(0) }
    var prevPoints by remember { mutableIntStateOf(rp.totalPoints) }
    LaunchedEffect(rp.totalPoints) {
        val gained = rp.totalPoints - prevPoints
        if (gained > 0) {
            floatingPoints = gained
            floatingOffset.snapTo(0f)
            floatingAlpha.snapTo(1f)
            launch { floatingOffset.animateTo(-60f, tween(1200, easing = FastOutSlowInEasing)) }
            launch {
                delay(500)
                floatingAlpha.animateTo(0f, tween(700, easing = FastOutSlowInEasing))
            }
        }
        prevPoints = rp.totalPoints
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(accents.gradientStart, accents.gradientEnd)),
                        RoundedCornerShape(11.dp),
                    ),
        ) {
            Canvas(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(11.dp))) {
                drawRect(
                    brush =
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFFFBF0), Color(0xFFFFF5DC)),
                            startX = 0f,
                            endX = size.width,
                        ),
                    size = size,
                )
                drawRect(color = Color.White.copy(alpha = 0.22f), size = Size(size.width, size.height * 0.45f))
                if (progress > 0f) {
                    val fillWidth = size.width * progress
                    drawRect(
                        brush =
                            Brush.horizontalGradient(
                                colors = listOf(accents.gradientStart, accents.gradientEnd),
                                startX = 0f,
                                endX = fillWidth,
                            ),
                        size = Size(fillWidth, size.height),
                    )
                    drawRect(
                        color = Color.White.copy(alpha = 0.28f),
                        size = Size(fillWidth, size.height * 0.45f),
                    )
                }
                if (progress > 0.02f && progress < 0.98f) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.7f),
                        start = Offset(size.width * progress, 0f),
                        end = Offset(size.width * progress, size.height),
                        strokeWidth = 2.5.dp.toPx(),
                    )
                }
            }
            if (rp.showsProgressNumbers) {
                if (rp.totalPoints > 0) {
                    // Anchor floored to keep the number clear of the left rounded corner at very low
                    // progress; a text shadow keeps it legible when it falls outside the colored fill.
                    val textAnchor = progress.coerceAtLeast(0.12f)
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier.weight(textAnchor).fillMaxHeight(),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Text(
                                "${rp.totalPoints}",
                                style =
                                    MaterialTheme.typography.labelLarge.copy(
                                        shadow =
                                            Shadow(
                                                color = Color.Black.copy(alpha = 0.8f),
                                                offset = Offset(0f, 1f),
                                                blurRadius = 3f,
                                            ),
                                    ),
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(end = 7.dp),
                            )
                        }
                        Spacer(modifier = Modifier.weight((1f - textAnchor).coerceAtLeast(0.001f)))
                    }
                }
                // Target cost: consistent dark-amber regardless of fill level
                Box(
                    modifier = Modifier.fillMaxSize().padding(end = 8.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Text(
                        "${rp.reward.cost}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFB06000),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        // Floating +X on task log
        if (floatingAlpha.value > 0.01f) {
            Text(
                "+$floatingPoints",
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = floatingOffset.value.dp)
                        .alpha(floatingAlpha.value),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
            )
        }
    }
}

@Composable
private fun RewardClaimOrLogButton(
    rp: RewardProgress,
    accents: EarnItAccents,
    claimScale: Float,
    logEnabled: Boolean,
    showMandatoryHint: Boolean,
    showAllTasksLoggedHint: Boolean,
    onClaim: () -> Unit,
    onLog: () -> Unit,
) {
    val accentColor = accents.gradientStart
    val view = LocalView.current
    if (rp.canClaim) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .scale(claimScale)
                    .shadow(4.dp, RoundedCornerShape(50))
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        view.hapticTap()
                        onClaim()
                    }.padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White,
                )
                Text(
                    Strings.REWARD_DETAIL_CLAIM_BTN,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
            }
        }
    } else {
        LogPillButton(
            label = Strings.LOG_BTN,
            accentColor = accentColor,
            enabled = logEnabled,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                view.hapticTap()
                onLog()
            },
        )
        if (showMandatoryHint) {
            Spacer(Modifier.height(6.dp))
            Text(
                Strings.REWARD_MANDATORY_TASKS_HINT,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = accents.notification,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        } else if (showAllTasksLoggedHint) {
            Spacer(Modifier.height(6.dp))
            Text(
                Strings.REWARD_ALL_TASKS_LOGGED_HINT,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = accents.notification,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun RewardTasksSection(
    rp: RewardProgress,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    hasWidgetPinned: Boolean,
    widgetNudgeDismissed: Boolean,
    onAddTaskClick: () -> Unit,
    onDismissWidgetNudge: () -> Unit,
    onPinWidget: () -> Unit,
) {
    HorizontalDivider(color = Color(0xFFD5C9B0), thickness = 1.dp)

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded)
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            Strings.rewardEarnTasksTitle(rp.allTasks.size),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Icon(
            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
    if (isExpanded) {
        if (rp.allTasks.isEmpty()) {
            Text(
                Strings.REWARD_DETAIL_NO_TASKS,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (rp.mandatoryTasks.sortedBy { it.name } + rp.optionalTasks.sortedBy { it.name }).forEach { task ->
                    val isMandatory = rp.mandatoryTasks.any { it.id == task.id }
                    val isRepeatable = rp.taskRefs.find { it.taskId == task.id }?.isRepeatable ?: false
                    val isDone = rp.activeLogs.any { it.taskId == task.id }
                    val pts = task.effectivePoints()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            task.name,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        if (isMandatory) {
                            Icon(
                                if (isDone) Icons.Default.Star else Icons.Outlined.Star,
                                contentDescription = null,
                                tint =
                                    if (isDone) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                    },
                                modifier = Modifier.size(14.dp).padding(start = 4.dp),
                            )
                        }
                        if (isRepeatable) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp).padding(start = 3.dp),
                            )
                        }
                        Text(
                            "+$pts",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
        }
        OutlinedButton(
            onClick = onAddTaskClick,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary,
                ),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(Strings.REWARD_ADD_TASK_BTN)
        }
        if (rp.allTasks.isNotEmpty() && !hasWidgetPinned && !widgetNudgeDismissed) {
            Spacer(Modifier.height(8.dp))
            DismissibleTipBanner(
                text = Strings.WIDGET_NUDGE_BODY,
                onDismiss = onDismissWidgetNudge,
                dismissContentDescription = Strings.WIDGET_NUDGE_DISMISS_DESC,
            ) {
                OutlinedButton(
                    onClick = onPinWidget,
                    shape = RoundedCornerShape(10.dp),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                ) {
                    Text(Strings.WIDGET_NUDGE_ACTION, style = buttonLabelStyle)
                }
            }
        }
    }
}

@Composable
private fun RewardActivitySection(
    rp: RewardProgress,
    tasks: List<TaskEntity>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded)
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val actBase = if (rp.canClaim) Strings.REWARD_ROAD_TO_GLORY else Strings.REWARD_RECENT_ACTIVITY
        Text(
            "$actBase${if (rp.activeLogs.isNotEmpty()) " (${rp.activeLogs.size})" else ""}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Icon(
            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
    if (isExpanded) {
        if (rp.activeLogs.isEmpty()) {
            Text(
                Strings.NO_ACTIVITY,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rp.activeLogs.sortedByDescending { it.timestamp }.forEach { log ->
                    val task = tasks.find { it.id == log.taskId }
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                task?.name ?: log.taskName,
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
