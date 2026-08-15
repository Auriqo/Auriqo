package com.auriqo.music.echomusic.updater

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

private const val UPDATE_CHECK_WORK_NAME = "auriqa_update_check"

fun scheduleUpdateChecks(context: Context) {
    val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(1, TimeUnit.DAYS)
        .setInitialDelay(1, TimeUnit.DAYS)
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build(),
        )
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        UPDATE_CHECK_WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        request,
    )
}

class UpdateCheckWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        if (!getAutoUpdateCheckSetting(applicationContext)) {
            return Result.success()
        }

        return try {
            val update = fetchLatestUpdate(applicationContext)
            saveLastCheckedTime(
                applicationContext,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy, h:mm a")),
            )
            saveUpdateCheckResult(applicationContext, update, notify = true)
            Result.success()
        } catch (error: Exception) {
            Log.w("UpdateCheck", "Background update check failed", error)
            Result.retry()
        }
    }
}
