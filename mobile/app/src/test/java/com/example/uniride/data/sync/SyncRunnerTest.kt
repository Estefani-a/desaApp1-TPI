package com.example.uniride.data.sync

import com.example.uniride.fake.FakeSyncHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class SyncRunnerTest {

    @Test
    fun `sin handlers registrados el resultado es DONE`() = runTest {
        assertEquals(SyncOutcome.DONE, SyncRunner(emptySet()).run())
    }

    @Test
    fun `si todos los handlers dan DONE el resultado es DONE y cada uno corre una sola vez`() = runTest {
        val a = FakeSyncHandler.always(SyncOutcome.DONE)
        val b = FakeSyncHandler.always(SyncOutcome.DONE)

        val result = SyncRunner(setOf(a, b)).run()

        assertEquals(SyncOutcome.DONE, result)
        assertEquals(1, a.calls)
        assertEquals(1, b.calls)
    }

    @Test
    fun `si un handler da RETRY el resultado es RETRY y los demas igual corren`() = runTest {
        val retry = FakeSyncHandler.always(SyncOutcome.RETRY)
        val done = FakeSyncHandler.always(SyncOutcome.DONE)

        val result = SyncRunner(setOf(retry, done)).run()

        assertEquals(SyncOutcome.RETRY, result)
        assertEquals(1, done.calls)
    }

    @Test
    fun `si un handler da STOP el resultado es STOP aunque otro de RETRY`() = runTest {
        val stop = FakeSyncHandler.always(SyncOutcome.STOP)
        val retry = FakeSyncHandler.always(SyncOutcome.RETRY)

        assertEquals(SyncOutcome.STOP, SyncRunner(setOf(retry, stop)).run())
    }

    @Test
    fun `un handler que lanza una excepcion cuenta como RETRY y no frena a los demas`() = runTest {
        val broken = FakeSyncHandler.throwing(IllegalStateException("unexpected"))
        val done = FakeSyncHandler.always(SyncOutcome.DONE)

        val result = SyncRunner(setOf(broken, done)).run()

        assertEquals(SyncOutcome.RETRY, result)
        assertEquals(1, done.calls)
    }

    @Test
    fun `la CancellationException de un handler se relanza`() = runTest {
        val cancelled = FakeSyncHandler.throwing(CancellationException("cancelled"))

        try {
            SyncRunner(setOf(cancelled)).run()
            fail("Expected CancellationException")
        } catch (e: CancellationException) {
            assertEquals("cancelled", e.message)
        }
    }

    @Test
    fun `mientras un handler da PROGRESS repite la pasada hasta que da DONE`() = runTest {
        val handler = FakeSyncHandler.sequence(SyncOutcome.PROGRESS, SyncOutcome.PROGRESS, SyncOutcome.DONE)

        val result = SyncRunner(setOf(handler), maxPasses = 5).run()

        assertEquals(SyncOutcome.DONE, result)
        assertEquals(3, handler.calls)
    }

    @Test
    fun `la repeticion tiene un tope de pasadas y al alcanzarlo devuelve RETRY`() = runTest {
        val handler = FakeSyncHandler.always(SyncOutcome.PROGRESS)

        val result = SyncRunner(setOf(handler), maxPasses = 3).run()

        assertEquals(SyncOutcome.RETRY, result)
        assertEquals(3, handler.calls)
    }

    @Test
    fun `el tope de pasadas por defecto es 3`() = runTest {
        val handler = FakeSyncHandler.always(SyncOutcome.PROGRESS)

        SyncRunner(setOf(handler)).run()

        assertEquals(3, handler.calls)
    }

    @Test
    fun `si en una repeticion un handler falla se corta y devuelve RETRY`() = runTest {
        val handler = FakeSyncHandler.sequence(SyncOutcome.PROGRESS, SyncOutcome.RETRY)

        val result = SyncRunner(setOf(handler), maxPasses = 5).run()

        assertEquals(SyncOutcome.RETRY, result)
        assertEquals(2, handler.calls)
    }

    @Test
    fun `si un handler da PROGRESS y otro RETRY no se repite la pasada`() = runTest {
        val progress = FakeSyncHandler.always(SyncOutcome.PROGRESS)
        val retry = FakeSyncHandler.always(SyncOutcome.RETRY)

        val result = SyncRunner(setOf(progress, retry), maxPasses = 5).run()

        assertEquals(SyncOutcome.RETRY, result)
        assertEquals(1, progress.calls)
    }

    @Test
    fun `cuando se repite la pasada corren todos los handlers de nuevo`() = runTest {
        val progress = FakeSyncHandler.sequence(SyncOutcome.PROGRESS, SyncOutcome.DONE)
        val idle = FakeSyncHandler.always(SyncOutcome.DONE)

        SyncRunner(setOf(progress, idle), maxPasses = 5).run()

        assertEquals(2, idle.calls)
    }
}
