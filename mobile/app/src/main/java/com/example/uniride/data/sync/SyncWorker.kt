package com.example.uniride.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background job that sends everything the user saved offline. It only translates the result
 * of [SyncRunner] for WorkManager: `DONE` is success, `RETRY` runs again with backoff and
 * `STOP` ends without retrying.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val runner: SyncRunner,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = when (runner.run()) {
        SyncOutcome.DONE, SyncOutcome.PROGRESS -> Result.success()
        SyncOutcome.RETRY -> Result.retry()
        SyncOutcome.STOP -> Result.failure()
    }
}
