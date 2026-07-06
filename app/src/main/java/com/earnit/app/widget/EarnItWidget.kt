package com.earnit.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.earnit.app.MainActivity
import com.earnit.app.R
import com.earnit.app.data.AppColorScheme
import com.earnit.app.data.EarnItRepository
import com.earnit.app.data.RewardProgress
import com.earnit.app.ui.theme.ColorSchemes
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay

val WIDGET_REWARD_ID_KEY = longPreferencesKey("widget_reward_id")
val WIDGET_REWARD_NAME_KEY = stringPreferencesKey("widget_reward_name")

private val White = ColorProvider(Color.White)

// ── Flash state (SharedPrefs so it survives across widget instances) ───────────

object WidgetFlash {
    private const val PREFS = "widget_flash"
    private const val K_UNTIL = "flash_until"
    private const val K_RWID = "flash_reward_id"

    fun set(
        context: Context,
        rewardId: Long,
        durationMs: Long = 3000L,
    ) {
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(K_UNTIL, System.currentTimeMillis() + durationMs)
            .putLong(K_RWID, rewardId)
            .apply()
    }

    fun isActive(
        context: Context,
        rewardId: Long,
    ): Boolean {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return p.getLong(K_RWID, 0L) == rewardId &&
            System.currentTimeMillis() < p.getLong(K_UNTIL, 0L)
    }

    fun remainingMs(
        context: Context,
        rewardId: Long,
    ): Long {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val until = if (p.getLong(K_RWID, 0L) == rewardId) p.getLong(K_UNTIL, 0L) else 0L
        return (until - System.currentTimeMillis()).coerceAtLeast(0L)
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun repository(): EarnItRepository

    fun settingsRepository(): com.earnit.app.data.SettingsRepository
}

// ── Widget ─────────────────────────────────────────────────────────────────────

class EarnItGlanceWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val entryPoint =
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetEntryPoint::class.java,
            )
        val repo = entryPoint.repository()
        val settingsRepo = entryPoint.settingsRepository()

        provideContent {
            val prefs = currentState<Preferences>()
            val rewardId = prefs[WIDGET_REWARD_ID_KEY] ?: 0L
            val rewardName = prefs[WIDGET_REWARD_NAME_KEY] ?: ""

            val allProgress by produceState<List<RewardProgress>>(emptyList()) {
                repo.observeUiState().collect { value = it.rewardProgressList }
            }
            val scheme by produceState(AppColorScheme.WARM_GOLD) {
                settingsRepo.settings.collect { value = it.colorScheme }
            }

            val colors = widgetColors(context, scheme)
            val progress = allProgress.find { it.reward.id == rewardId }
            WidgetContent(context, progress, rewardId, rewardName, colors)
        }
    }
}

// ── Theme-aware colors ─────────────────────────────────────────────────────────

private data class WidgetColors(
    val primary: ColorProvider,
    val surface: ColorProvider,
    val track: ColorProvider,
    val onSurface: ColorProvider,
    val onSurfaceVar: ColorProvider,
    val secondary: ColorProvider,
)

private fun widgetColors(
    context: Context,
    scheme: AppColorScheme,
): WidgetColors {
    val isDark =
        (
            context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        ) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
    val cs = if (isDark) ColorSchemes.darkColors(scheme) else ColorSchemes.lightColors(scheme)
    return WidgetColors(
        primary = ColorProvider(cs.primary),
        surface = ColorProvider(cs.surface),
        track = ColorProvider(cs.primaryContainer),
        onSurface = ColorProvider(cs.onSurface),
        onSurfaceVar = ColorProvider(cs.onSurfaceVariant),
        secondary = ColorProvider(cs.secondary),
    )
}

// ── Root ───────────────────────────────────────────────────────────────────────

@Composable
private fun WidgetContent(
    context: Context,
    progress: RewardProgress?,
    rewardId: Long,
    rewardName: String,
    colors: WidgetColors,
) {
    val showFlash by produceState(
        initialValue = WidgetFlash.isActive(context, rewardId),
        key1 = rewardId,
    ) {
        value = WidgetFlash.isActive(context, rewardId)
        if (value) {
            delay(WidgetFlash.remainingMs(context, rewardId) + 100L)
            value = false
        }
    }

    Box(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(colors.primary)
                .cornerRadius(16.dp)
                .padding(3.dp),
    ) {
        Box(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .background(colors.surface)
                    .cornerRadius(14.dp)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            when {
                progress != null && showFlash ->
                    FlashContent(context, progress.reward.id, colors)
                progress != null ->
                    StandardContent(context, progress, rewardName, colors)
                rewardId != 0L -> ClaimedState(context, rewardName, colors)
                else -> EmptyState(colors)
            }
        }
    }
}

// ── Flash state ────────────────────────────────────────────────────────────────

@Composable
private fun FlashContent(
    context: Context,
    rewardId: Long,
    colors: WidgetColors,
) {
    val mainIntent =
        Intent(context, MainActivity::class.java).apply {
            putExtra("rewardId", rewardId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .clickable(actionStartActivity(mainIntent)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("✓", style = TextStyle(color = colors.primary, fontSize = 22.sp, fontWeight = FontWeight.Bold))
        Spacer(GlanceModifier.height(2.dp))
        Text("Logged!", style = TextStyle(color = colors.onSurfaceVar, fontSize = 13.sp))
    }
}

// ── Empty / unconfigured ───────────────────────────────────────────────────────

@Composable
private fun EmptyState(colors: WidgetColors) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("EarnIt", style = TextStyle(color = colors.primary, fontSize = 18.sp, fontWeight = FontWeight.Bold))
        Spacer(GlanceModifier.height(4.dp))
        Text("Long-press to configure", style = TextStyle(color = colors.onSurfaceVar, fontSize = 12.sp))
    }
}

// ── Claimed / archived ─────────────────────────────────────────────────────────

@Composable
private fun ClaimedState(
    context: Context,
    rewardName: String,
    colors: WidgetColors,
) {
    val mainIntent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    Row(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .clickable(actionStartActivity(mainIntent)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                rewardName.ifBlank { "Reward" },
                maxLines = 1,
                style = TextStyle(color = colors.primary, fontSize = 15.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(GlanceModifier.height(2.dp))
            Text(
                "Earned and Claimed",
                style = TextStyle(color = colors.onSurfaceVar, fontSize = 12.sp),
            )
        }
    }
}

// ── Standard layout ────────────────────────────────────────────────────────────

@Composable
private fun StandardContent(
    context: Context,
    progress: RewardProgress,
    rewardName: String,
    colors: WidgetColors,
) {
    val current = progress.totalPoints
    val cost = progress.reward.cost
    val fraction = (current.toFloat() / cost.toFloat()).coerceIn(0f, 1f)

    val completedIds = progress.activeLogs.map { it.taskId }.toSet()
    val taskRefsMap = progress.taskRefs.associateBy { it.taskId }
    val hasTasks =
        (progress.mandatoryTasks + progress.optionalTasks).any { task ->
            val ref = taskRefsMap[task.id]
            ref?.isRepeatable == true || task.id !in completedIds
        }
    val showMandatoryHint = !progress.canClaim && progress.totalPoints >= progress.reward.cost
    val noTasks = progress.allTasks.isEmpty()

    val logIntent =
        Intent(context, WidgetTaskLogActivity::class.java).apply {
            putExtra("rewardId", progress.reward.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    val mainIntent =
        Intent(context, MainActivity::class.java).apply {
            putExtra("rewardId", progress.reward.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

    val displayName = rewardName.ifBlank { progress.reward.name }

    Box(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .clickable(actionStartActivity(mainIntent)),
        contentAlignment = Alignment.Center,
    ) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        displayName,
                        maxLines = 1,
                        style = TextStyle(color = colors.primary, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    )
                    if (showMandatoryHint) {
                        Spacer(GlanceModifier.height(2.dp))
                        Text(
                            "Required tasks needed to claim",
                            style = TextStyle(color = colors.onSurfaceVar, fontSize = 11.sp),
                        )
                    }
                }
                Spacer(GlanceModifier.width(8.dp))
                when {
                    progress.canClaim ->
                        Box(
                            modifier =
                                GlanceModifier
                                    .background(colors.primary)
                                    .cornerRadius(16.dp)
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                    .clickable(actionStartActivity(mainIntent)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("CLAIM", style = TextStyle(color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                        }
                    hasTasks ->
                        Box(
                            modifier =
                                GlanceModifier
                                    .size(32.dp)
                                    .background(colors.primary)
                                    .cornerRadius(16.dp)
                                    .clickable(actionStartActivity(logIntent)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_add),
                                contentDescription = "Log task",
                                modifier = GlanceModifier.size(20.dp),
                            )
                        }
                    noTasks ->
                        Box(
                            modifier =
                                GlanceModifier
                                    .background(colors.track)
                                    .cornerRadius(16.dp)
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                                    .clickable(actionStartActivity(mainIntent)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("ADD TASK", style = TextStyle(color = colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                        }
                }
            }

            Spacer(GlanceModifier.height(8.dp))
            ProgressBar(fraction, current, colors)
        }
    }
}

// ── Progress bar ───────────────────────────────────────────────────────────────

@Composable
private fun ProgressBar(
    fraction: Float,
    current: Int,
    colors: WidgetColors,
) {
    val widgetWidth = LocalSize.current.width.value
    val contentWidth = (widgetWidth - 32f).coerceAtLeast(40f)
    val fillWidth = (contentWidth * fraction.coerceIn(0f, 1f)).dp

    Box(
        modifier =
            GlanceModifier
                .fillMaxWidth()
                .height(20.dp)
                .background(colors.primary)
                .cornerRadius(10.dp)
                .padding(1.dp),
    ) {
        Box(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .background(colors.track)
                    .cornerRadius(9.dp),
        ) {
            if (fraction > 0.01f) {
                Box(
                    modifier =
                        GlanceModifier
                            .width(fillWidth)
                            .fillMaxHeight()
                            .background(colors.primary)
                            .cornerRadius(9.dp),
                ) {}
            }
            if (fraction > 0.12f) {
                Box(
                    modifier =
                        GlanceModifier
                            .width(fillWidth)
                            .fillMaxHeight()
                            .padding(end = 4.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Text(
                        "$current",
                        style = TextStyle(color = White, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

// ── Receivers ──────────────────────────────────────────────────────────────────

class EarnItGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EarnItGlanceWidget()
}
