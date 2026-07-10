package com.earnit.app.nudge

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.earnit.app.data.AppSettings
import com.earnit.app.data.CompletionLogDao
import com.earnit.app.data.EarnItDatabase
import com.earnit.app.data.RewardDao
import com.earnit.app.data.SettingsRepository
import com.earnit.app.ui.Strings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import androidx.work.ListenableWorker.Result as WorkResult

/**
 * Exercises NudgeWorker.doWork() — the actual production path that decides whether to post a
 * real notification — via the real Context/coroutine machinery androidx.work provides for
 * testing CoroutineWorker, rather than only unit-testing NudgeDecider's pure logic in isolation.
 * Notification assertions go through Robolectric's NotificationManager shadow.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NudgeWorkerTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val hour = 60 * 60 * 1000L

    private fun buildWorker(
        lastLogTimestamp: Long?,
        activeRewardCount: Int,
        settings: AppSettings,
    ): Pair<NudgeWorker, SettingsRepository> {
        val logDao = mockk<CompletionLogDao>()
        coEvery { logDao.getLastLogTimestamp() } returns lastLogTimestamp

        val rewardDao = mockk<RewardDao>()
        coEvery { rewardDao.getActiveRewardCount() } returns activeRewardCount

        val database = mockk<EarnItDatabase>()
        every { database.completionLogDao() } returns logDao
        every { database.rewardDao() } returns rewardDao

        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        every { settingsRepository.settings } returns flowOf(settings)

        val worker =
            TestListenableWorkerBuilder<NudgeWorker>(context)
                .setWorkerFactory(
                    object : WorkerFactory() {
                        override fun createWorker(
                            appContext: Context,
                            workerClassName: String,
                            workerParameters: WorkerParameters,
                        ): ListenableWorker = NudgeWorker(appContext, workerParameters, database, settingsRepository)
                    },
                ).build()

        return worker to settingsRepository
    }

    private fun postedNotifications() = shadowOf(context.getSystemService(NotificationManager::class.java)).allNotifications

    @Test
    fun `posts first nudge and records stage 1 when idle at least 48h from stage 0`() =
        runTest {
            shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
            val lastLog = System.currentTimeMillis() - 48 * hour
            val (worker, settingsRepository) =
                buildWorker(lastLogTimestamp = lastLog, activeRewardCount = 1, settings = AppSettings(nudgeStage = 0))

            val result = worker.doWork()

            assertEquals(WorkResult.success(), result)
            val notifications = postedNotifications()
            assertEquals(1, notifications.size)
            assertEquals(
                Strings.NUDGE_FIRST_TITLE,
                notifications
                    .single()
                    .extras
                    .getCharSequence(Notification.EXTRA_TITLE)
                    .toString(),
            )
            coVerify { settingsRepository.updateNudgeState(1, lastLog) }
        }

    @Test
    fun `posts second nudge and records stage 2 when idle at least 96h from stage 1`() =
        runTest {
            shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
            val lastLog = System.currentTimeMillis() - 96 * hour
            val (worker, settingsRepository) =
                buildWorker(
                    lastLogTimestamp = lastLog,
                    activeRewardCount = 1,
                    settings = AppSettings(nudgeStage = 1, nudgeAnchorTimestamp = lastLog),
                )

            worker.doWork()

            val notifications = postedNotifications()
            assertEquals(1, notifications.size)
            assertEquals(
                Strings.NUDGE_SECOND_TITLE,
                notifications
                    .single()
                    .extras
                    .getCharSequence(Notification.EXTRA_TITLE)
                    .toString(),
            )
            coVerify { settingsRepository.updateNudgeState(2, lastLog) }
        }

    @Test
    fun `does not notify when idle under 48h`() =
        runTest {
            shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
            val lastLog = System.currentTimeMillis() - 10 * hour
            val (worker, settingsRepository) =
                buildWorker(lastLogTimestamp = lastLog, activeRewardCount = 1, settings = AppSettings(nudgeStage = 0))

            worker.doWork()

            assertEquals(0, postedNotifications().size)
            coVerify(exactly = 0) { settingsRepository.updateNudgeState(any(), any()) }
        }

    @Test
    fun `does not notify when there is no active reward`() =
        runTest {
            shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
            val lastLog = System.currentTimeMillis() - 200 * hour
            val (worker, _) =
                buildWorker(lastLogTimestamp = lastLog, activeRewardCount = 0, settings = AppSettings(nudgeStage = 0))

            worker.doWork()

            assertEquals(0, postedNotifications().size)
        }

    @Test
    fun `does not notify when nothing has ever been logged`() =
        runTest {
            shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
            val (worker, _) =
                buildWorker(lastLogTimestamp = null, activeRewardCount = 1, settings = AppSettings(nudgeStage = 0))

            worker.doWork()

            assertEquals(0, postedNotifications().size)
        }

    @Test
    fun `does not notify again once stage 2 already recorded`() =
        runTest {
            shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
            val lastLog = System.currentTimeMillis() - 500 * hour
            val (worker, settingsRepository) =
                buildWorker(
                    lastLogTimestamp = lastLog,
                    activeRewardCount = 1,
                    settings = AppSettings(nudgeStage = 2, nudgeAnchorTimestamp = lastLog),
                )

            worker.doWork()

            assertEquals(0, postedNotifications().size)
            coVerify(exactly = 0) { settingsRepository.updateNudgeState(any(), any()) }
        }

    @Test
    fun `resets stage without notifying when a new log broke the idle streak`() =
        runTest {
            shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
            val newLog = System.currentTimeMillis() - 1 * hour
            val (worker, settingsRepository) =
                buildWorker(
                    lastLogTimestamp = newLog,
                    activeRewardCount = 1,
                    settings = AppSettings(nudgeStage = 1, nudgeAnchorTimestamp = System.currentTimeMillis() - 60 * hour),
                )

            worker.doWork()

            assertEquals(0, postedNotifications().size)
            coVerify { settingsRepository.updateNudgeState(0, newLog) }
        }

    @Test
    fun `does not notify when POST_NOTIFICATIONS permission is denied`() =
        runTest {
            shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
            val lastLog = System.currentTimeMillis() - 48 * hour
            val (worker, settingsRepository) =
                buildWorker(lastLogTimestamp = lastLog, activeRewardCount = 1, settings = AppSettings(nudgeStage = 0))

            worker.doWork()

            assertEquals(0, postedNotifications().size)
            // Stage is still recorded even though the notification couldn't be shown — the
            // idle streak did cross the threshold, so state shouldn't drift from decision logic.
            coVerify { settingsRepository.updateNudgeState(1, lastLog) }
        }
}
