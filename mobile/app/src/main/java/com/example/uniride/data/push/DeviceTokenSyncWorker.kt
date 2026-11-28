package com.example.uniride.data.push

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Retries registering the current FCM device token (`PUT /users/me/device-token`, API.md §4.6)
 * once connectivity returns after a transient failure.
 */
@HiltWorker
class DeviceTokenSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val pushTokenSyncManager: PushTokenSyncManager,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = when (pushTokenSyncManager.retryTokenSync()) {
        TokenSyncOutcome.DONE -> Result.success()
        TokenSyncOutcome.RETRY -> Result.retry()
    }
}
