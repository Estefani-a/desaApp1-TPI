package com.example.uniride.data.push

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Enqueues a network-constrained retry when `PUT /users/me/device-token` fails transiently. */
fun interface DeviceTokenSyncScheduler {
    fun scheduleRetry()
}

/**
 * Schedules a single unique WorkManager job ([WORK_NAME]) requiring network connectivity with
 * exponential backoff so a failed token upload after login or token rotation recovers once online.
 */
class WorkManagerDeviceTokenSyncScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : DeviceTokenSyncScheduler {

    override fun scheduleRetry() {
        val request = OneTimeWorkRequestBuilder<DeviceTokenSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        const val WORK_NAME = "device_token_sync"
        private const val BACKOFF_SECONDS = 30L
    }
}
