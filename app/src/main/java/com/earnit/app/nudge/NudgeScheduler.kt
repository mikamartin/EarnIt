package com.earnit.app.nudge

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NudgeScheduler {
    private const val PERIODIC_WORK_NAME = "earnit_nudge_check"
    private const val ONE_TIME_WORK_NAME = "earnit_nudge_check_now"
    private const val CHECK_INTERVAL_HOURS = 6L

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<NudgeWorker>(CHECK_INTERVAL_HOURS, TimeUnit.HOURS).build()
        WorkManager
            .getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Dev-tool hook: runs the nudge check immediately instead of waiting for the periodic schedule. */
    fun runNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<NudgeWorker>().build()
        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(ONE_TIME_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }
}
