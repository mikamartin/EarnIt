package com.earnit.app.widget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.glance.appwidget.updateAll
import androidx.hilt.navigation.compose.hiltViewModel
import com.earnit.app.MainActivity
import com.earnit.app.R
import com.earnit.app.data.EarnItRepository
import com.earnit.app.data.TaskEntity
import com.earnit.app.di.ApplicationScope
import com.earnit.app.ui.NOTE_MAX_CHARS
import com.earnit.app.ui.Strings
import com.earnit.app.ui.acceptWithinLimit
import com.earnit.app.ui.theme.EarnItTheme
import com.earnit.app.viewmodel.EarnItViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val WIDGET_LOG_CHANNEL_ID = "earnit_widget_log"
private const val WIDGET_LOG_NOTIF_ID = 1001

@AndroidEntryPoint
class WidgetTaskLogActivity : ComponentActivity() {
    @Inject lateinit var repository: EarnItRepository

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // no-op: showNotification() checks grant state at call time
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        val rewardId = intent?.getLongExtra("rewardId", 0L) ?: 0L

        // Clear any stale flash from a prior process that never got to revert it.
        appScope.launch(Dispatchers.Main) {
            EarnItGlanceWidget().updateAll(applicationContext)
        }

        setContent {
            ThemedTaskPicker(
                rewardId = rewardId,
                onTaskLogged = { task, note ->
                    val pts = task.effectivePoints()
                    val appCtx = applicationContext
                    triggerHaptic()
                    showNotification(task, pts)
                    WidgetFlash.set(appCtx, rewardId)
                    // appScope outlives the activity: writes DB, then triggers widget re-render.
                    // The widget's own produceState handles the flash revert after 3 s.
                    appScope.launch(Dispatchers.IO) {
                        repository.logCompletion(task, rewardId, note)
                        withContext(Dispatchers.Main) {
                            EarnItGlanceWidget().updateAll(appCtx)
                        }
                    }
                    finish()
                },
                onClose = { finish() },
            )
        }
    }

    private fun triggerHaptic() {
        val vibrator = getSystemService(Vibrator::class.java)
        vibrator?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun showNotification(
        task: TaskEntity,
        pts: Int,
    ) {
        val canNotify =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (!canNotify) return
        val tapIntent =
            PendingIntent.getActivity(
                applicationContext,
                0,
                Intent(applicationContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_IMMUTABLE,
            )
        NotificationManagerCompat.from(applicationContext).notify(
            WIDGET_LOG_NOTIF_ID,
            NotificationCompat
                .Builder(applicationContext, WIDGET_LOG_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_add)
                .setContentTitle(task.name)
                .setContentText(Strings.widgetLoggedNotif(pts))
                .setAutoCancel(true)
                .setContentIntent(tapIntent)
                .build(),
        )
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                WIDGET_LOG_CHANNEL_ID,
                Strings.WIDGET_NOTIF_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW,
            )
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }
}

@Composable
private fun ThemedTaskPicker(
    rewardId: Long,
    viewModel: EarnItViewModel = hiltViewModel(),
    onTaskLogged: (TaskEntity, String) -> Unit,
    onClose: () -> Unit,
) {
    val settings by viewModel.settings.collectAsState()
    EarnItTheme(colorScheme = settings.colorScheme) {
        Surface(
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            color = MaterialTheme.colorScheme.background,
        ) {
            TaskPickerScreen(rewardId, viewModel, onTaskLogged, onClose)
        }
    }
}

@Composable
private fun TaskPickerScreen(
    rewardId: Long,
    viewModel: EarnItViewModel = hiltViewModel(),
    onTaskLogged: (TaskEntity, String) -> Unit,
    onClose: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val rp = uiState.rewardProgressList.find { it.reward.id == rewardId } ?: return

    val tasks = rp.loggableTasks

    var selectedTask by remember { mutableStateOf<TaskEntity?>(null) }
    var loggedPair by remember { mutableStateOf<Pair<TaskEntity, String>?>(null) }

    if (loggedPair != null) {
        val (task, note) = loggedPair!!
        LaunchedEffect(Unit) {
            delay(1500L)
            onTaskLogged(task, note)
        }
        SuccessScreen(task)
        return
    }

    if (selectedTask != null) {
        BackHandler { selectedTask = null }
        NoteScreen(
            task = selectedTask!!,
            onConfirm = { note ->
                loggedPair = selectedTask!! to note
            },
            onBack = { selectedTask = null },
        )
        return
    }

    BackHandler { onClose() }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val rewardLabel =
            rp.reward.icon
                .takeIf { it.isNotEmpty() }
                ?.let { "$it ${rp.reward.name}" } ?: rp.reward.name
        Text(
            rewardLabel,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "${rp.totalPoints} / ${rp.reward.cost} pts",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))

        if (tasks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    Strings.WIDGET_TASK_ALL_LOGGED,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Text(
                Strings.WIDGET_TASK_PICKER_HINT,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tasks) { task ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedTask = task }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            task.name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "+${task.effectivePoints()} pts",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessScreen(task: TaskEntity) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "✓",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            task.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "+${task.effectivePoints()} pts",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun NoteScreen(
    task: TaskEntity,
    onConfirm: (String) -> Unit,
    onBack: () -> Unit,
) {
    var note by rememberSaveable { mutableStateOf("") }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            task.name,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            Strings.WIDGET_NOTE_HINT,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = note,
            onValueChange = { note = acceptWithinLimit(note, it, NOTE_MAX_CHARS) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(Strings.WIDGET_NOTE_PLACEHOLDER) },
            minLines = 3,
            maxLines = 5,
            supportingText = { Text("${note.length}/$NOTE_MAX_CHARS") },
        )
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { onConfirm(note.trim()) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(Strings.WIDGET_LOG_TASK_BTN)
        }
        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(Strings.WIDGET_BACK_BTN)
        }
    }
}
