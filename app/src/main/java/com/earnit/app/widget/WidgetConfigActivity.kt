package com.earnit.app.widget

import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.earnit.app.data.RewardProgress
import com.earnit.app.ui.EarnItPrimaryButton
import com.earnit.app.ui.REWARD_NAME_MAX_CHARS
import com.earnit.app.ui.Strings
import com.earnit.app.ui.theme.EarnItTheme
import com.earnit.app.viewmodel.EarnItViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        val appWidgetId =
            intent.extras?.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            ThemedWidgetConfig { rewardId, label -> onConfirmed(appWidgetId, rewardId, label) }
        }
    }

    private fun onConfirmed(
        appWidgetId: Int,
        rewardId: Long,
        label: String,
    ) {
        lifecycleScope.launch {
            val glanceId =
                GlanceAppWidgetManager(this@WidgetConfigActivity)
                    .getGlanceIdBy(appWidgetId)

            updateAppWidgetState(
                this@WidgetConfigActivity,
                PreferencesGlanceStateDefinition,
                glanceId,
            ) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[WIDGET_REWARD_ID_KEY] = rewardId
                    this[WIDGET_REWARD_NAME_KEY] = label
                }
            }

            EarnItGlanceWidget().update(this@WidgetConfigActivity, glanceId)

            setResult(
                RESULT_OK,
                android.content.Intent().apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                },
            )
            finish()
        }
    }
}

@Composable
private fun ThemedWidgetConfig(
    viewModel: EarnItViewModel = hiltViewModel(),
    onConfirmed: (Long, String) -> Unit,
) {
    val settings by viewModel.settings.collectAsState()
    EarnItTheme(colorScheme = settings.colorScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            WidgetConfigFlow(viewModel, onConfirmed)
        }
    }
}

@Composable
private fun WidgetConfigFlow(
    viewModel: EarnItViewModel,
    onConfirmed: (Long, String) -> Unit,
) {
    var pendingReward by remember { mutableStateOf<RewardProgress?>(null) }

    if (pendingReward == null) {
        RewardPickerScreen(viewModel) { pendingReward = it }
    } else {
        LabelEditScreen(
            reward = pendingReward!!,
            onConfirm = { label -> onConfirmed(pendingReward!!.reward.id, label) },
            onBack = { pendingReward = null },
        )
    }
}

@Composable
private fun RewardPickerScreen(
    viewModel: EarnItViewModel,
    onRewardPicked: (RewardProgress) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val rewards = uiState.rewardProgressList

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        Text(
            Strings.WIDGET_CONFIG_TITLE,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            Strings.WIDGET_CONFIG_SUBTITLE,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        if (rewards.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    Strings.WIDGET_CONFIG_EMPTY,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(rewards) { rp ->
                    RewardRow(rp, onClick = { onRewardPicked(rp) })
                }
            }
        }
    }
}

@Composable
private fun LabelEditScreen(
    reward: RewardProgress,
    onConfirm: (String) -> Unit,
    onBack: () -> Unit,
) {
    val defaultLabel =
        reward.reward.icon
            .takeIf { it.isNotEmpty() }
            ?.let { "$it ${reward.reward.name}" } ?: reward.reward.name

    var label by rememberSaveable(reward.reward.id) { mutableStateOf(defaultLabel) }

    BackHandler(onBack = onBack)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = Strings.BACK_DESC,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                Strings.WIDGET_LABEL_TITLE,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            Strings.widgetRewardName(reward.reward.name),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            Strings.WIDGET_LABEL_HINT,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = label,
            onValueChange = { if (it.length <= REWARD_NAME_MAX_CHARS) label = it },
            label = { Text(Strings.WIDGET_LABEL_TITLE) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(Modifier.weight(1f))

        EarnItPrimaryButton(
            text = Strings.WIDGET_ADD_BTN,
            onClick = { onConfirm(label.trim().ifBlank { defaultLabel }) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RewardRow(
    rp: RewardProgress,
    onClick: () -> Unit,
) {
    val fraction = (rp.totalPoints.toFloat() / rp.reward.cost.toFloat()).coerceIn(0f, 1f)
    val pct = (fraction * 100).toInt()

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                rp.reward.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "${rp.totalPoints} / ${rp.reward.cost} pts  •  $pct%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
