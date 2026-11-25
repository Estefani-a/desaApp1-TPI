package com.example.uniride.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Builds a [SyncWorker] with the given handlers, like HiltWorkerFactory does with the real ones. */
    private fun worker(vararg outcomes: SyncOutcome): SyncWorker {
        val handlers: Set<SyncHandler> = outcomes.map { outcome ->
            object : SyncHandler {
                override suspend fun syncPending() = outcome
            }
        }.toSet()
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker? =
                if (workerClassName == SyncWorker::class.java.name) {
                    SyncWorker(appContext, workerParameters, SyncRunner(handlers))
                } else {
                    null
                }
        }
        return TestListenableWorkerBuilder<SyncWorker>(context).setWorkerFactory(factory).build()
    }

    @Test
    fun `si no hay nada pendiente el worker termina con success`() = runTest {
        assertEquals(Result.success(), worker(SyncOutcome.DONE).doWork())
    }

    @Test
    fun `sin handlers registrados el worker termina con success`() = runTest {
        assertEquals(Result.success(), worker().doWork())
    }

    @Test
    fun `si un handler pide reintentar el worker devuelve retry`() = runTest {
        assertEquals(Result.retry(), worker(SyncOutcome.DONE, SyncOutcome.RETRY).doWork())
    }

    @Test
    fun `si la sesion no es valida el worker devuelve failure`() = runTest {
        assertEquals(Result.failure(), worker(SyncOutcome.RETRY, SyncOutcome.STOP).doWork())
    }
}
