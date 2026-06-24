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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.earnit.app.data.EarnItUiState
import com.earnit.app.data.Mascots
import com.earnit.app.data.RewardProgress
import com.earnit.app.ui.theme.LocalEarnItAccents
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
private fun SlideUpVisibility(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) { content() }
}

@Composable
private fun FadeVisibility(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) { content() }
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
    var showMaxBanner by remember { mutableStateOf(false) }
    var showMaxTooltip by remember { mutableStateOf(false) }
    val homeScope = rememberCoroutineScope()
    val listSize = uiState.rewardProgressList.size
    var prevListSize by remember { mutableStateOf(listSize) }

    LaunchedEffect(listSize) {
        val addedOne = listSize == prevListSize + 1
        if (addedOne && listSize >= settings.maxRewardCount) {
            showMaxBanner = true
            delay(3000)
            showMaxBanner = false
        } else if (listSize < settings.maxRewardCount) {
            showMaxBanner = false
        }
        prevListSize = listSize
    }

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

    logDialogRewardId?.let { rewardId ->
        reorderedList.find { it.reward.id == rewardId }?.let { dialogRp ->
            LogTaskDialog(rp = dialogRp, viewModel = viewModel, notesMandatory = settings.notesMandatory, onDismiss = {
                logDialogRewardId =
                    null
            })
        }
    }
    claimDialogRewardId?.let { rewardId ->
        reorderedList.find { it.reward.id == rewardId }?.let { dialogRp ->
            ClaimDialog(
                rewardName = dialogRp.reward.name,
                onStartOver = {
                    viewModel.claimReward(rewardId, startOver = true)
                    claimDialogRewardId = null
                },
                onArchive = {
                    viewModel.claimReward(rewardId, startOver = false)
                    claimDialogRewardId = null
                },
                onDismiss = { claimDialogRewardId = null },
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
                    modifier = Modifier.size(150.dp).scale(mascotScale.value),
                )
            }
            Text(
                Strings.appTitle(if (settings.useRandomNickname) viewModel.sessionNickname else settings.nickname),
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        val accentPalette = LocalEarnItAccents.current.cardPalette
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (settings.showQuote) {
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
                } else {
                    val maxCost = reorderedList.maxOfOrNull { it.reward.cost } ?: 0
                    itemsIndexed(reorderedList, key = { _, rp -> rp.reward.id }) { index, rp ->
                        var accumulatedDragY by remember { mutableStateOf(0f) }
                        val cardModifier =
                            Modifier
                                .animateItem()
                                .pointerInput(rp.reward.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            isDragging = true
                                            draggingRewardId = rp.reward.id
                                            draggingIndex = reorderedList.indexOfFirst { it.reward.id == rp.reward.id }
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
                                                        draggingIndex = target.index
                                                        accumulatedDragY = 0f
                                                    }
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            isDragging = false
                                            draggingRewardId = null
                                            draggingIndex = -1
                                            accumulatedDragY = 0f
                                            viewModel.updateRewardsOrder(reorderedList.map { it.reward.id })
                                        },
                                        onDragCancel = {
                                            isDragging = false
                                            draggingRewardId = null
                                            draggingIndex = -1
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
                            onCardClick = {
                                if (!isDragging) {
                                    navController.navigate(
                                        Screen.RewardDetail.route(rp.reward.id),
                                    )
                                }
                            },
                            onLogTask = { logDialogRewardId = rp.reward.id },
                            onClaim = { claimDialogRewardId = rp.reward.id },
                            onEditReward = { navController.navigate(Screen.RewardEdit.route(rp.reward.id)) },
                        )
                    }
                }
            }
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                SlideUpVisibility(visible = showMaxBanner) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .shadow(4.dp, RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            Strings.MAX_REWARD_BANNER,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

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
                                    .background(Color(0xFF5C3D00))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                Strings.MAX_REWARD_BANNER,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
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
                            .clickable {
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
    onEditReward: (() -> Unit)? = null,
) {
    val accents = LocalEarnItAccents.current
    val showMandatoryHint = !rp.canClaim && rp.totalPoints >= rp.reward.cost
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

    val warmBase = MaterialTheme.colorScheme.surfaceVariant
    val cardColor =
        if (isTopTier) {
            MaterialTheme.colorScheme.primaryContainer
                .copy(alpha = 0.55f)
                .compositeOver(warmBase)
        } else {
            warmBase
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
                    } else if (rp.allTasks.isEmpty() && onEditReward != null) {
                        LogPillButton(Strings.HOME_ADD_TASKS_BTN, accentColor, onClick = onEditReward)
                    } else if (onLogTask != null) {
                        LogPillButton(
                            Strings.LOG_BTN,
                            accentColor,
                            enabled = rp.allTasks.isNotEmpty(),
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
