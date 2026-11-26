package com.example.uniride.data.sync

import com.example.uniride.data.remote.datasource.FakeBackendStore
import com.example.uniride.data.remote.datasource.FakeReservationRemoteDataSource
import com.example.uniride.data.remote.datasource.FakeTripRemoteDataSource
import com.example.uniride.data.remote.datasource.ReservationRemoteDataSource
import com.example.uniride.data.repository.ReservationRejectionStore
import com.example.uniride.domain.model.ReservationId
import com.example.uniride.domain.model.ReservationRejection
import com.example.uniride.domain.model.ReservationStatus
import com.example.uniride.fake.FakeReservationDao
import com.example.uniride.fake.FakeTripDao
import com.example.uniride.fake.FakeTripRepository
import com.example.uniride.fake.FakeUserDao
import com.example.uniride.fake.InMemoryPreferencesDataStore
import com.example.uniride.fake.reservationRow
import com.example.uniride.fake.userRow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ReservationSyncHandlerTest {

    private val driverId = FakeBackendStore.DEFAULT_DRIVER_ID
    private val passengerId = FakeBackendStore.DEFAULT_PASSENGER_ID
    private val tripId = FakeBackendStore.DEMO_TRIP_MONSERRAT_ID

    private val store = FakeBackendStore().apply { currentUserId = passengerId }
    private val serverReservations = FakeReservationRemoteDataSource(store)
    private val remote = ToggleableReservationRemote(serverReservations)
    private val reservationDao = FakeReservationDao()
    private val userDao = FakeUserDao(userRow(id = passengerId, withVehicle = false))
    private val rejections = ReservationRejectionStore(InMemoryPreferencesDataStore())
    private val now = Instant.parse("2026-11-21T10:00:00Z")
    private val handler = ReservationSyncHandler(
        reservationDao,
        userDao,
        remote,
        FakeTripRemoteDataSource(store),
        FakeTripDao(),
        FakeTripRepository(),
        rejections,
        Clock.fixed(now, ZoneId.of("America/Argentina/Buenos_Aires")),
    )

    /** Acts on the fake backend as [userId] (the device's own user does not change). */
    private suspend fun <T> asUser(userId: String, block: suspend () -> T): T {
        val previous = store.currentUserId
        store.currentUserId = userId
        return try {
            block()
        } finally {
            store.currentUserId = previous
        }
    }

    private fun loginAsDriver() {
        userDao.user.value = userRow(id = driverId)
        store.currentUserId = driverId
    }

    private fun departTrip() {
        val trip = store.trips.getValue(tripId)
        store.trips[tripId] = trip.copy(departureDate = LocalDate.now().minusDays(1).toString())
    }

    private suspend fun rejectionOf(id: String) = rejections.observe().first()[ReservationId(id)]

    @Test
    fun `sin reservas pendientes no llama a la red y devuelve DONE`() = runTest {
        reservationDao.upsert(reservationRow(isSync = true))

        assertEquals(SyncOutcome.DONE, handler.syncPending())
        assertEquals(0, remote.calls)
    }

    @Test
    fun `una solicitud hecha sin conexion queda creada en el servidor y sincronizada`() = runTest {
        reservationDao.upsert(reservationRow(id = "res-1"))

        assertEquals(SyncOutcome.PROGRESS, handler.syncPending())

        val row = reservationDao.getReservation("res-1")!!
        assertTrue(row.isSync)
        assertEquals(ReservationStatus.PENDIENTE, row.status)
        assertEquals("PENDIENTE", store.reservations.getValue("res-1").status)
    }

    @Test
    fun `aceptar sin conexion se envia cuando vuelve la conexion`() = runTest {
        asUser(passengerId) { serverReservations.requestSeat(tripId, "res-1") }
        loginAsDriver()
        reservationDao.upsert(reservationRow(id = "res-1", status = ReservationStatus.ACEPTADA))

        assertEquals(SyncOutcome.PROGRESS, handler.syncPending())

        assertTrue(reservationDao.getReservation("res-1")!!.isSync)
        assertEquals("ACEPTADA", store.reservations.getValue("res-1").status)
    }

    @Test
    fun `sin conexion devuelve RETRY y deja la fila intacta`() = runTest {
        val pending = reservationRow(id = "res-1")
        reservationDao.upsert(pending)
        remote.failWith = IOException()

        assertEquals(SyncOutcome.RETRY, handler.syncPending())

        assertEquals(pending, reservationDao.getReservation("res-1"))
    }

    @Test
    fun `una sesion vencida devuelve STOP`() = runTest {
        reservationDao.upsert(reservationRow(id = "res-1"))
        store.currentUserId = null

        assertEquals(SyncOutcome.STOP, handler.syncPending())
        assertFalse(reservationDao.getReservation("res-1")!!.isSync)
    }

    @Test
    fun `una solicitud a un viaje completo queda cerrada y con el motivo NO_SEATS`() = runTest {
        val trip = store.trips.getValue(tripId)
        store.trips[tripId] = trip.copy(availableSeats = 0, status = FakeBackendStore.STATUS_FULL)
        reservationDao.upsert(reservationRow(id = "res-1"))

        assertEquals(SyncOutcome.PROGRESS, handler.syncPending())

        val row = reservationDao.getReservation("res-1")!!
        assertEquals(ReservationStatus.CANCELADA, row.status)
        assertTrue(row.isSync)
        assertEquals(ReservationRejection.NO_SEATS, rejectionOf("res-1"))
    }

    @Test
    fun `cancelar un viaje que ya empezo deja la reserva aceptada y el motivo TRIP_ALREADY_DEPARTED`() = runTest {
        asUser(passengerId) { serverReservations.requestSeat(tripId, "res-1") }
        asUser(driverId) { serverReservations.acceptReservation(tripId, "res-1") }
        departTrip()
        reservationDao.upsert(reservationRow(id = "res-1", status = ReservationStatus.CANCELADA))

        assertEquals(SyncOutcome.PROGRESS, handler.syncPending())

        val row = reservationDao.getReservation("res-1")!!
        assertEquals(ReservationStatus.ACEPTADA, row.status)
        assertTrue(row.isSync)
        assertEquals(ReservationRejection.TRIP_ALREADY_DEPARTED, rejectionOf("res-1"))
        assertEquals("ACEPTADA", store.reservations.getValue("res-1").status)
    }

    @Test
    fun `una solicitud pedida y cancelada sin conexion se cierra sin motivo`() = runTest {
        reservationDao.upsert(reservationRow(id = "res-1", status = ReservationStatus.CANCELADA))

        assertEquals(SyncOutcome.PROGRESS, handler.syncPending())

        assertTrue(reservationDao.getReservation("res-1")!!.isSync)
        assertEquals(null, rejectionOf("res-1"))
    }

    @Test
    fun `aceptar sin vacantes deja la solicitud como la tiene el servidor y el motivo NO_SEATS`() = runTest {
        asUser(passengerId) { serverReservations.requestSeat(tripId, "res-1") }
        val trip = store.trips.getValue(tripId)
        store.trips[tripId] = trip.copy(availableSeats = 0, status = FakeBackendStore.STATUS_FULL)
        loginAsDriver()
        reservationDao.upsert(reservationRow(id = "res-1", status = ReservationStatus.ACEPTADA))

        assertEquals(SyncOutcome.PROGRESS, handler.syncPending())

        val row = reservationDao.getReservation("res-1")!!
        assertEquals(ReservationStatus.PENDIENTE, row.status)
        assertTrue(row.isSync)
        assertEquals(ReservationRejection.NO_SEATS, rejectionOf("res-1"))
    }
}

/** [ReservationRemoteDataSource] that can fail on purpose and counts its calls. */
class ToggleableReservationRemote(private val delegate: ReservationRemoteDataSource) : ReservationRemoteDataSource {

    var failWith: Throwable? = null
    var calls = 0
        private set

    private suspend fun <T> pass(block: suspend () -> T): T {
        calls++
        failWith?.let { throw it }
        return block()
    }

    override suspend fun requestSeat(tripId: String, reservationId: String) =
        pass { delegate.requestSeat(tripId, reservationId) }

    override suspend fun getTripReservations(tripId: String) = pass { delegate.getTripReservations(tripId) }

    override suspend fun acceptReservation(tripId: String, reservationId: String) =
        pass { delegate.acceptReservation(tripId, reservationId) }

    override suspend fun rejectReservation(tripId: String, reservationId: String) =
        pass { delegate.rejectReservation(tripId, reservationId) }

    override suspend fun cancelReservation(tripId: String, reservationId: String) =
        pass { delegate.cancelReservation(tripId, reservationId) }
}
