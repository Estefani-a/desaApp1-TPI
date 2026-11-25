package com.example.uniride.data.sync

import com.example.uniride.core.result.DataError
import com.example.uniride.data.remote.datasource.FakeBackendStore
import com.example.uniride.data.remote.datasource.FakeTripRemoteDataSource
import com.example.uniride.data.repository.TripRejectionStore
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.model.TripStatus
import com.example.uniride.fake.FakeTripDao
import com.example.uniride.fake.InMemoryPreferencesDataStore
import com.example.uniride.fake.ToggleableTripRemote
import com.example.uniride.fake.tripRow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class TripSyncHandlerTest {

    private val store = FakeBackendStore().apply { currentUserId = FakeBackendStore.DEFAULT_DRIVER_ID }
    private val remote = ToggleableTripRemote(FakeTripRemoteDataSource(store))
    private val tripDao = FakeTripDao()
    private val rejections = TripRejectionStore(InMemoryPreferencesDataStore())
    private val now = Instant.parse("2026-11-21T10:00:00Z")
    private val handler = TripSyncHandler(tripDao, remote, rejections, Clock.fixed(now, ZoneId.of("UTC")))

    @Test
    fun `sin viajes pendientes no llama a la red y devuelve DONE`() = runTest {
        tripDao.upsert(tripRow("sincronizado", isSync = true))

        assertEquals(SyncOutcome.DONE, handler.syncPending())
        assertEquals(0, remote.calls)
    }

    @Test
    fun `un viaje aceptado queda con lo que devuelve el servidor y sincronizado`() = runTest {
        tripDao.upsert(tripRow("nuevo", totalSeats = 4))

        assertEquals(SyncOutcome.PROGRESS, handler.syncPending())

        val row = tripDao.getAll().single()
        assertTrue(row.isSync)
        assertEquals(TripStatus.ABIERTO, row.status)
        assertEquals(4, row.availableSeats)
        assertTrue(rejections.observe().first().isEmpty())
    }

    @Test
    fun `reenviar un viaje ya enviado es seguro`() = runTest {
        tripDao.upsert(tripRow("nuevo"))
        handler.syncPending()
        tripDao.upsert(tripDao.getAll().single().copy(isSync = false))

        assertEquals(SyncOutcome.PROGRESS, handler.syncPending())
        assertTrue(tripDao.getAll().single().isSync)
    }

    @Test
    fun `sin conexion devuelve RETRY y deja la fila intacta`() = runTest {
        val pending = tripRow("nuevo")
        tripDao.upsert(pending)
        remote.failWith = IOException()

        assertEquals(SyncOutcome.RETRY, handler.syncPending())

        assertEquals(listOf(pending), tripDao.getAll())
    }

    @Test
    fun `un timeout devuelve RETRY y deja la fila intacta`() = runTest {
        val pending = tripRow("nuevo")
        tripDao.upsert(pending)
        remote.failWith = SocketTimeoutException()

        assertEquals(SyncOutcome.RETRY, handler.syncPending())
        assertEquals(listOf(pending), tripDao.getAll())
    }

    @Test
    fun `una sesion vencida devuelve STOP y deja la fila intacta`() = runTest {
        val pending = tripRow("nuevo")
        tripDao.upsert(pending)
        store.currentUserId = null

        assertEquals(SyncOutcome.STOP, handler.syncPending())

        assertEquals(listOf(pending), tripDao.getAll())
    }

    @Test
    fun `un rechazo cancela el viaje, lo marca sincronizado y guarda el motivo`() = runTest {
        // The server only accepts 1 to 4 seats
        tripDao.upsert(tripRow("invalido", totalSeats = 9))

        assertEquals(SyncOutcome.PROGRESS, handler.syncPending())

        val row = tripDao.getAll().single()
        assertEquals(TripStatus.CANCELADO, row.status)
        assertTrue(row.isSync)
        assertEquals(now, row.updatedAt)
        assertEquals(mapOf(TripId("invalido") to DataError.VALIDATION), rejections.observe().first())
        assertEquals(SyncOutcome.DONE, handler.syncPending())
    }

    @Test
    fun `un rechazo no frena el envio de los demas viajes`() = runTest {
        tripDao.upsertAll(listOf(tripRow("invalido", totalSeats = 9), tripRow("bueno", totalSeats = 2)))

        assertEquals(SyncOutcome.PROGRESS, handler.syncPending())

        val rows = tripDao.getAll().associateBy { it.tripId }
        assertEquals(TripStatus.CANCELADO, rows.getValue("invalido").status)
        assertEquals(TripStatus.ABIERTO, rows.getValue("bueno").status)
        assertTrue(rows.values.all { it.isSync })
    }
}
