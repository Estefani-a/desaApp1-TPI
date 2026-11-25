package com.example.uniride

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.uniride.data.sync.SyncScheduler
import com.example.uniride.domain.repository.SessionRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class UniRideApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    // Instantiated eagerly at process start so the global 401 listener is active even when
    // Android wakes the process only to run SyncWorker in the background (CONVENCIONES §6.4).
    @Inject
    lateinit var sessionRepository: SessionRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Sends what a previous session left pending
        syncScheduler.schedule()
    }
}
