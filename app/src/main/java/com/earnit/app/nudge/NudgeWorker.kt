package com.earnit.app.nudge

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.earnit.app.MainActivity
import com.earnit.app.R
import com.earnit.app.data.EarnItDatabase
import com.earnit.app.data.SettingsRepository
import com.earnit.app.ui.Strings
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

private const val NUDGE_CHANNEL_ID = "earnit_nudge"
private const val NUDGE_NOTIF_ID = 2001

@HiltWorker
class NudgeWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val database: EarnItDatabase,
        private val settingsRepository: SettingsRepository,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val lastLogTimestamp = database.completionLogDao().getLastLogTimestamp()
            val hasActiveReward = database.rewardDao().getActiveRewardCount() > 0
            val settings = settingsRepository.settings.first()

            when (
                val decision =
                    NudgeDecider.decide(
                        now = System.currentTimeMillis(),
                        lastLogTimestamp = lastLogTimestamp,
                        hasActiveReward = hasActiveReward,
                        currentStage = settings.nudgeStage,
                        anchorTimestamp = settings.nudgeAnchorTimestamp,
                    )
            ) {
                is NudgeDecision.Send -> {
                    showNotification(decision.stage)
                    settingsRepository.updateNudgeState(decision.stage, decision.newAnchor)
                }
                is NudgeDecision.Reset -> settingsRepository.updateNudgeState(0, decision.newAnchor)
                NudgeDecision.NoOp -> {}
            }
            return Result.success()
        }

        private fun showNotification(stage: Int) {
            createNotificationChannel()
            val canNotify =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    applicationContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            if (!canNotify) return

            val (title, body) =
                if (stage == 1) {
                    Strings.NUDGE_FIRST_TITLE to Strings.NUDGE_FIRST_BODY
                } else {
                    Strings.NUDGE_SECOND_TITLE to Strings.NUDGE_SECOND_BODY
                }

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
                NUDGE_NOTIF_ID,
                NotificationCompat
                    .Builder(applicationContext, NUDGE_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_add)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setAutoCancel(true)
                    .setContentIntent(tapIntent)
                    .build(),
            )
        }

        private fun createNotificationChannel() {
            val channel =
                NotificationChannel(
                    NUDGE_CHANNEL_ID,
                    Strings.NUDGE_NOTIF_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            applicationContext.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
