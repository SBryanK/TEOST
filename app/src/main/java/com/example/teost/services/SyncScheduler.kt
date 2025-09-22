package com.example.teost.services

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncScheduler {
    private const val UNIQUE_ONCE = "edgeone_cloud_sync_once"
    private const val UNIQUE_PERIODIC = "edgeone_cloud_sync_periodic"

    fun runOneTimeNow(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val req = OneTimeWorkRequestBuilder<CloudSyncWorker>()
            .setConstraints(constraints)
            .addTag(UNIQUE_ONCE)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_ONCE, ExistingWorkPolicy.REPLACE, req)
    }

    fun observeOnce(context: Context) =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(UNIQUE_ONCE)

    fun lastState(context: Context): List<WorkInfo> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWork(UNIQUE_ONCE).get()

    fun schedulePeriodic(context: Context, repeatHours: Long = 24) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        val req = PeriodicWorkRequestBuilder<CloudSyncWorker>(repeatHours, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag(UNIQUE_PERIODIC)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun cancelPeriodic(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIODIC)
    }
}


