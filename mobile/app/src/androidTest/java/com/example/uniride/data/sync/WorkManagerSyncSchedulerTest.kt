package com.example.uniride.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkManagerSyncSchedulerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var scheduler: WorkManagerSyncScheduler

    @Before
    fun setUp() {
        val configuration = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
        scheduler = WorkManagerSyncScheduler(context)
    }

    private fun scheduledWork(): List<WorkInfo> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWork(WorkManagerSyncScheduler.WORK_NAME).get()

    @Test
    fun `el trabajo se encola con el nombre unico sync`() {
        scheduler.schedule()

        assertEquals("sync", WorkManagerSyncScheduler.WORK_NAME)
        assertEquals(1, scheduledWork().size)
        assertEquals(WorkInfo.State.ENQUEUED, scheduledWork().single().state)
    }

    @Test
    fun `el trabajo exige conexion de red`() {
        scheduler.schedule()

        assertEquals(NetworkType.CONNECTED, scheduledWork().single().constraints.requiredNetworkType)
    }

    @Test
    fun `programar dos veces no duplica el trabajo porque se conserva el primero`() {
        scheduler.schedule()
        val firstId = scheduledWork().single().id

        scheduler.schedule()

        assertEquals(1, scheduledWork().size)
        assertEquals(firstId, scheduledWork().single().id)
    }
}
