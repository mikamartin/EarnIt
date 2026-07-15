@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.earnit.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.earnit.app.data.AppSettings
import com.earnit.app.data.EarnItUiState
import com.earnit.app.data.MascotId
import com.earnit.app.data.Mascots
import com.earnit.app.data.RewardProgress
import com.earnit.app.ui.theme.LocalEarnItAccents
import com.earnit.app.ui.theme.cardSurface
import com.earnit.app.viewmodel.EarnItViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun QuoteOfTheDay() {
    val quote =
        remember {
            val dayIndex = (System.currentTimeMillis() / 86_400_000L).toInt()
            motivationalQuotes[dayIndex % motivationalQuotes.size]
        }
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        val surface = MaterialTheme.colorScheme.surface
        val accents = LocalEarnItAccents.current
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 9.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(surface)
                    .border(
                        width = 2.dp,
                        brush = Brush.horizontalGradient(listOf(accents.gradientStart, accents.gradientEnd)),
                        shape = RoundedCornerShape(14.dp),
                    ),
        ) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)) {
                Text(
                    quote.text,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (quote.source.isNotEmpty()) {
                    Text(
                        "— ${quote.source}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        Text(
            Strings.HOME_QUOTE_SECTION,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp)
                    .background(surface)
                    .padding(horizontal = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.ExtraBold,
            color = accents.gradientStart,
        )
    }
}

@Composable
private fun FadeVisibility(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) { content() }
}

@Composable
private fun HomeHeader(
    mascotScale: Float,
    settings: AppSettings,
    nickname: String,
    onMascotTap: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val mascotDrawable =
            Mascots.all
                .find { it.id == settings.selectedMascotId }
                ?.drawable
        if (mascotDrawable != null) {
            androidx.compose.foundation.Image(
                painter = painterResource(mascotDrawable),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(150.dp)
                        .scale(mascotScale)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onMascotTap,
                        ),
            )
        }
        Text(
            Strings.appTitle(nickname),
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun HomeDialogs(
    logDialogRewardId: Long?,
    claimDialogRewardId: Long?,
    reorderedList: List<RewardProgress>,
    viewModel: EarnItViewModel,
    notesMandatory: Boolean,
    onDismissLog: () -> Unit,
    onDismissClaim: () -> Unit,
) {
    logDialogRewardId?.let { rewardId ->
        reorderedList.find { it.reward.id == rewardId }?.let { dialogRp ->
            LogTaskDialog(
                rp = dialogRp,
                viewModel = viewModel,
                notesMandatory = notesMandatory,
                onDismiss = onDismissLog,
            )
        }
    }
    claimDialogRewardId?.let { rewardId ->
        reorderedList.find { it.reward.id == rewardId }?.let { dialogRp ->
            ClaimDialog(
                rewardName = dialogRp.reward.name,
                onStartOver = {
                    viewModel.claimReward(rewardId, startOver = true)
                    onDismissClaim()
                },
                onArchive = {
                    viewModel.claimReward(rewardId, startOver = false)
                    onDismissClaim()
                },
                onDismiss = onDismissClaim,
            )
        }
    }
}

@Composable
private fun BoxScope.HomeAddRewardFab(
    isEmpty: Boolean,
    atMax: Boolean,
    fabPulseScale: Float,
    showMaxTooltip: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            FadeVisibility(visible = showMaxTooltip) {
                Box(
                    modifier =
                        Modifier
                            .padding(bottom = 8.dp)
                            .shadow(2.dp, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        Strings.MAX_REWARD_TOOLTIP,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
        Box(
            modifier =
                Modifier
                    .scale(if (isEmpty) fabPulseScale else 1f)
                    .size(56.dp)
                    .alpha(if (atMax) 0.4f else 1f)
                    .shadow(if (atMax) 2.dp else 6.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = Strings.NEW_REWARD_DESC,
                tint = Color.White,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
fun HomeScreen(
    uiState: EarnItUiState,
    viewModel: EarnItViewModel,
    navController: NavHostController,
) {
    val settings by viewModel.settings.collectAsState()
    val reorderedList = remember { mutableStateListOf<RewardProgress>() }
    var isDragging by remember { mutableStateOf(false) }
    var draggingIndex by remember { mutableStateOf(-1) }
    var draggingRewardId by remember { mutableStateOf<Long?>(null) }
    var logDialogRewardId by remember { mutableStateOf<Long?>(null) }
    var claimDialogRewardId by remember { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()
    val atMax = uiState.rewardProgressList.size >= settings.maxRewardCount
    val isEmpty = uiState.rewardProgressList.isEmpty()
    val emptyPulse = rememberInfiniteTransition(label = "emptyPulse")
    val fabPulseScale by emptyPulse.animateFloat(
        initialValue = 1f,
        targetValue = if (isEmpty) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "fabPulse",
    )
    var showMaxTooltip by remember { mutableStateOf(false) }
    val homeScope = rememberCoroutineScope()

    LaunchedEffect(uiState.rewardProgressList) {
        if (isDragging) return@LaunchedEffect
        val currentIds = reorderedList.map { it.reward.id }.toSet()
        val newIds = uiState.rewardProgressList.map { it.reward.id }.toSet()
        if (currentIds == newIds) {
            uiState.rewardProgressList.forEach { updated ->
                val idx = reorderedList.indexOfFirst { it.reward.id == updated.reward.id }
                if (idx >= 0) reorderedList[idx] = updated
            }
        } else {
            reorderedList.clear()
            reorderedList.addAll(uiState.rewardProgressList)
        }
    }

    val pugslyTapTimestamps = remember { mutableListOf<Long>() }
    val mascotScale = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        viewModel.triggerMascotBounce.collect {
            mascotScale.animateTo(1.3f, tween(150, easing = FastOutSlowInEasing))
            mascotScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 400f))
        }
    }
    LaunchedEffect(Unit) {
        // The UI is collecting uiState here so WhileSubscribed is active — drop(1) safely
        // waits for the first real Room emission before checking mascot unlocks.
        viewModel.uiState.drop(1).first()
        viewModel.runStartupUnlockCheck()
    }

    HomeDialogs(
        logDialogRewardId = logDialogRewardId,
        claimDialogRewardId = claimDialogRewardId,
        reorderedList = reorderedList,
        viewModel = viewModel,
        notesMandatory = settings.notesMandatory,
        onDismissLog = { logDialogRewardId = null },
        onDismissClaim = { claimDialogRewardId = null },
    )

    Column(modifier = Modifier.fillMaxSize()) {
        HomeHeader(
            mascotScale = mascotScale.value,
            settings = settings,
            nickname = if (settings.useRandomNickname) viewModel.sessionNickname else settings.nickname,
            onMascotTap = {
                if (!settings.devModeEnabled && settings.selectedMascotId == MascotId.PUGSLY) {
                    val updated = PugslyGesture.nextState(pugslyTapTimestamps, System.currentTimeMillis())
                    pugslyTapTimestamps.clear()
                    pugslyTapTimestamps.addAll(updated)
                    if (PugslyGesture.isComplete(pugslyTapTimestamps)) {
                        pugslyTapTimestamps.clear()
                        viewModel.enableDevMode()
                        viewModel.bounceMascot()
                    }
                }
            },
        )
        val accentPalette = LocalEarnItAccents.current.cardPalette
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                homeRewardListItems(
                    showQuote = settings.showQuote,
                    reorderedList = reorderedList,
                    accentPalette = accentPalette,
                    listState = listState,
                    isDragging = isDragging,
                    onDraggingChange = { isDragging = it },
                    draggingIndex = draggingIndex,
                    onDraggingIndexChange = { draggingIndex = it },
                    draggingRewardId = draggingRewardId,
                    onDraggingRewardIdChange = { draggingRewardId = it },
                    onReorderCommitted = { viewModel.updateRewardsOrder(reorderedList.map { it.reward.id }) },
                    onCardClick = { rewardId ->
                        if (!isDragging) navController.navigate(Screen.RewardDetail.route(rewardId))
                    },
                    onLogTask = { logDialogRewardId = it },
                    onClaim = { claimDialogRewardId = it },
                    onAddTask = { navController.navigate(Screen.RewardDetail.route(it, autoOpenAddTask = true)) },
                )
            }
            HomeAddRewardFab(
                isEmpty = isEmpty,
                atMax = atMax,
                fabPulseScale = fabPulseScale,
                showMaxTooltip = showMaxTooltip,
                onClick = {
                    if (!atMax) {
                        navController.navigate(Screen.RewardEdit.route(0L))
                    } else {
                        showMaxTooltip = true
                        homeScope.launch {
                            delay(2000)
                            showMaxTooltip = false
                        }
                    }
                },
            )
        }
    }
}

private fun LazyListScope.homeRewardListItems(
    showQuote: Boolean,
    reorderedList: SnapshotStateList<RewardProgress>,
    accentPalette: List<Color>,
    listState: LazyListState,
    isDragging: Boolean,
    onDraggingChange: (Boolean) -> Unit,
    draggingIndex: Int,
    onDraggingIndexChange: (Int) -> Unit,
    draggingRewardId: Long?,
    onDraggingRewardIdChange: (Long?) -> Unit,
    onReorderCommitted: () -> Unit,
    onCardClick: (Long) -> Unit,
    onLogTask: (Long) -> Unit,
    onClaim: (Long) -> Unit,
    onAddTask: (Long) -> Unit,
) {
    if (showQuote) {
        item { QuoteOfTheDay() }
    }
    if (reorderedList.isEmpty()) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    Strings.HOME_EMPTY_REWARDS,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        return
    }
    val maxCost = reorderedList.maxOfOrNull { it.reward.cost } ?: 0
    itemsIndexed(reorderedList, key = { _, rp -> rp.reward.id }) { index, rp ->
        var accumulatedDragY by remember { mutableStateOf(0f) }
        val cardModifier =
            Modifier
                .animateItem()
                .pointerInput(rp.reward.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            onDraggingChange(true)
                            onDraggingRewardIdChange(rp.reward.id)
                            onDraggingIndexChange(reorderedList.indexOfFirst { it.reward.id == rp.reward.id })
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
                                        visibleItems.firstOrNull { item ->
                                            item.index != currentIdx &&
                                                center > item.offset &&
                                                center < item.offset + item.size
                                        }
                                    if (target != null) {
                                        reorderedList.add(
                                            target.index,
                                            reorderedList.removeAt(currentIdx),
                                        )
                                        onDraggingIndexChange(target.index)
                                        accumulatedDragY = 0f
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            onDraggingChange(false)
                            onDraggingRewardIdChange(null)
                            onDraggingIndexChange(-1)
                            accumulatedDragY = 0f
                            onReorderCommitted()
                        },
                        onDragCancel = {
                            onDraggingChange(false)
                            onDraggingRewardIdChange(null)
                            onDraggingIndexChange(-1)
                            accumulatedDragY = 0f
                        },
                    )
                }
        RewardProgressCard(
            rp = rp,
            accentColor = accentPalette[index % accentPalette.size],
            modifier = cardModifier,
            isBeingDragged = draggingRewardId == rp.reward.id,
            isTopTier = rp.reward.cost == maxCost && maxCost > 0,
            onCardClick = { onCardClick(rp.reward.id) },
            onLogTask = { onLogTask(rp.reward.id) },
            onClaim = { onClaim(rp.reward.id) },
            onAddTask = { onAddTask(rp.reward.id) },
        )
    }
}

@Composable
fun RewardProgressCard(
    rp: RewardProgress,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isBeingDragged: Boolean = false,
    isTopTier: Boolean = false,
    onCardClick: (() -> Unit)? = null,
    onLogTask: (() -> Unit)? = null,
    onClaim: (() -> Unit)? = null,
    onAddTask: (() -> Unit)? = null,
) {
    val accents = LocalEarnItAccents.current
    val showMandatoryHint = !rp.canClaim && rp.totalPoints >= rp.reward.cost
    val showAllTasksLoggedHint = !rp.canClaim && rp.allTasks.isNotEmpty() && rp.loggableTasks.isEmpty()
    val rawProgress = (rp.totalPoints.toFloat() / rp.reward.cost.toFloat()).coerceIn(0f, 1f)
    val progress by animateFloatAsState(rawProgress, tween(500, easing = FastOutSlowInEasing), label = "barProgress")
    val scale by animateFloatAsState(if (isBeingDragged) 1.03f else 1f, label = "scale")
    val elevation by animateDpAsState(if (isBeingDragged) 8.dp else 2.dp, label = "elevation")

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

    val cardBase = MaterialTheme.cardSurface
    val cardColor =
        if (isTopTier) {
            MaterialTheme.colorScheme.primary
                .copy(alpha = 0.18f)
                .compositeOver(cardBase)
        } else {
            cardBase
        }

    val cardShape = RoundedCornerShape(20.dp)
    val cardElevation = CardDefaults.cardElevation(defaultElevation = if (isTopTier) elevation + 1.dp else elevation)
    val cardColors = CardDefaults.cardColors(containerColor = cardColor)
    val cardModifier = modifier.fillMaxWidth().scale(scale)
    val cardContent: @Composable ColumnScope.() -> Unit = {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier =
                    Modifier
                        .width(5.dp)
                        .fillMaxHeight()
                        .background(accentColor),
            )
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                if (rp.reward.description.isNotEmpty()) {
                    Text(
                        rp.reward.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .border(
                                        1.dp,
                                        Brush.horizontalGradient(listOf(accents.gradientStart, accents.gradientEnd)),
                                        RoundedCornerShape(7.dp),
                                    ),
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(7.dp))) {
                                drawRect(
                                    brush = Brush.horizontalGradient(listOf(Color(0xFFFFFBF0), Color(0xFFFFF5DC))),
                                    size = size,
                                )
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
                            }
                        }
                        if (floatingAlpha.value > 0.01f) {
                            Text(
                                "+$floatingPoints",
                                modifier =
                                    Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(y = floatingOffset.value.dp)
                                        .alpha(floatingAlpha.value),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor,
                            )
                        }
                    }
                    if (rp.canClaim && onClaim != null) {
                        ClaimPillButton(modifier = Modifier.scale(claimScale), onClick = onClaim)
                    } else if (rp.allTasks.isEmpty() && onAddTask != null) {
                        LogPillButton(Strings.HOME_ADD_TASKS_BTN, accentColor, onClick = onAddTask)
                    } else if (onLogTask != null) {
                        LogPillButton(
                            Strings.LOG_BTN,
                            accentColor,
                            enabled = rp.loggableTasks.isNotEmpty(),
                            onClick = onLogTask,
                        )
                    }
                }
                if (showMandatoryHint) {
                    Text(
                        Strings.REWARD_MANDATORY_TASKS_HINT,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = accents.notification,
                    )
                } else if (showAllTasksLoggedHint) {
                    Text(
                        Strings.REWARD_ALL_TASKS_LOGGED_HINT,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = accents.notification,
                    )
                }
            }
        }
    }
    val claimBorder =
        if (rp.canClaim) {
            BorderStroke(
                2.dp,
                Brush.horizontalGradient(
                    listOf(
                        accents.gradientStart.copy(alpha = claimBorderAlpha),
                        accents.gradientEnd.copy(alpha = claimBorderAlpha),
                    ),
                ),
            )
        } else {
            null
        }
    if (onCardClick != null) {
        Card(
            onClick = onCardClick,
            modifier = cardModifier,
            shape = cardShape,
            elevation = cardElevation,
            colors = cardColors,
            border = claimBorder,
            content = cardContent,
        )
    } else {
        Card(
            modifier = cardModifier,
            shape = cardShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = cardColors,
            border = claimBorder,
            content = cardContent,
        )
    }
}
