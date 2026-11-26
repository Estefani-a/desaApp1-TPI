package com.example.uniride.data.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.data.remote.datasource.FakeBackendStore
import com.example.uniride.data.remote.datasource.FakeReservationRemoteDataSource
import com.example.uniride.domain.model.ReservationId
import com.example.uniride.domain.model.ReservationRejection
import com.example.uniride.domain.model.ReservationStatus
import com.example.uniride.domain.model.TripId
import com.example.uniride.fake.FakeReservationDao
import com.example.uniride.fake.FakeSyncScheduler
import com.example.uniride.fake.FakeUserDao
import com.example.uniride.fake.InMemoryPreferencesDataStore
import com.example.uniride.fake.reservationRow
import com.example.uniride.fake.userRow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** [ReservationRepositoryImpl] over fake DAOs and the fake backend of A2. */
class ReservationRepositoryImplTest {

    private val passengerId = FakeBackendStore.DEFAULT_PASSENGER_ID
    private val tripId = TripId(FakeBackendStore.DEMO_TRIP_MONSERRAT_ID)
    private val store = FakeBackendStore().apply { currentUserId = FakeBackendStore.DEFAULT_DRIVER_ID }
    private val reservationDao = FakeReservationDao()
    private val userDao = FakeUserDao(userRow(id = passengerId, withVehicle = false))
    private val scheduler = FakeSyncScheduler()
    private val rejections = ReservationRejectionStore(InMemoryPreferencesDataStore())
    private val now = Instant.parse("2026-11-20T21:00:00Z")
    private val repository = ReservationRepositoryImpl(
        reservationDao,
        userDao,
        FakeReservationRemoteDataSource(store),
        rejections,
        scheduler,
        Clock.fixed(now, ZoneId.of("America/Argentina/Buenos_Aires")),
    )

    @Test
    fun `pedir asiento sin conexion la guarda pendiente y pide sincronizar`() = runTest {
        val result = repository.requestSeat(tripId)

        val id = (result as AppResult.Success).data
        val row = reservationDao.getReservation(id.value)!!
        assertEquals(ReservationStatus.PENDIENTE, row.status)
        assertFalse(row.isSync)
        assertEquals(passengerId, row.passengerId)
        assertEquals("Lucía F.", row.passengerName)
        assertNull(row.passengerPhone)
        assertEquals(1, scheduler.scheduleCalls)
        assertEquals(setOf(id), repository.observePendingReservationIds().first())
    }

    @Test
    fun `pedir asiento sin usuario devuelve UNAUTHORIZED y no guarda nada`() = runTest {
        userDao.user.value = null

        assertEquals(AppResult.Failure(DataError.UNAUTHORIZED), repository.requestSeat(tripId))
        assertTrue(reservationDao.all().isEmpty())
        assertEquals(0, scheduler.scheduleCalls)
    }

    @Test
    fun `cancelar cambia el estado localmente y queda pendiente de enviar`() = runTest {
        reservationDao.upsert(reservationRow(id = "res-1", status = ReservationStatus.ACEPTADA, isSync = true))

        assertEquals(AppResult.Success(Unit), repository.cancel(tripId, ReservationId("res-1")))

        val row = reservationDao.getReservation("res-1")!!
        assertEquals(ReservationStatus.CANCELADA, row.status)
        assertFalse(row.isSync)
        assertEquals(1, scheduler.scheduleCalls)
    }

    @Test
    fun `repetir un cambio que ya esta hecho no hace nada`() = runTest {
        reservationDao.upsert(reservationRow(id = "res-1", status = ReservationStatus.ACEPTADA, isSync = true))

        assertEquals(AppResult.Success(Unit), repository.accept(tripId, ReservationId("res-1")))

        assertTrue(reservationDao.getReservation("res-1")!!.isSync)
        assertEquals(0, scheduler.scheduleCalls)
    }

    @Test
    fun `una reserva de otro viaje no se encuentra`() = runTest {
        reservationDao.upsert(reservationRow(id = "res-1", tripId = "otro-viaje", isSync = true))

        assertEquals(AppResult.Failure(DataError.NOT_FOUND), repository.reject(tripId, ReservationId("res-1")))
    }

    @Test
    fun `mi reserva en un viaje es la activa aunque haya una rechazada mas nueva`() = runTest {
        reservationDao.upsert(reservationRow(id = "vieja", status = ReservationStatus.PENDIENTE, isSync = true))
        reservationDao.upsert(
            reservationRow(id = "nueva", status = ReservationStatus.RECHAZADA, reservedAt = now, isSync = true),
        )

        assertEquals(ReservationId("vieja"), repository.observeMyReservation(tripId).first()?.id)
    }

    @Test
    fun `sin reservas mi reserva es null`() = runTest {
        assertNull(repository.observeMyReservation(tripId).first())
    }

    @Test
    fun `refrescar las solicitudes no pisa un cambio que no se envio`() = runTest {
        store.currentUserId = passengerId
        FakeReservationRemoteDataSource(store).requestSeat(tripId.value, "res-1")
        store.currentUserId = FakeBackendStore.DEFAULT_DRIVER_ID
        reservationDao.upsert(reservationRow(id = "res-1", status = ReservationStatus.ACEPTADA, isSync = false))

        assertEquals(AppResult.Success(Unit), repository.refreshTripReservations(tripId))

        val row = reservationDao.getReservation("res-1")!!
        assertEquals(ReservationStatus.ACEPTADA, row.status)
        assertFalse(row.isSync)
    }

    @Test
    fun `refrescar las solicitudes guarda lo que tiene el servidor`() = runTest {
        store.currentUserId = passengerId
        FakeReservationRemoteDataSource(store).requestSeat(tripId.value, "res-1")
        store.currentUserId = FakeBackendStore.DEFAULT_DRIVER_ID

        assertEquals(AppResult.Success(Unit), repository.refreshTripReservations(tripId))

        val row = reservationDao.getReservation("res-1")!!
        assertEquals(ReservationStatus.PENDIENTE, row.status)
        assertTrue(row.isSync)
    }

    @Test
    fun `descartar un rechazo lo saca de la lista`() = runTest {
        rejections.add(ReservationId("res-1"), ReservationRejection.NO_SEATS)

        repository.dismissRejection(ReservationId("res-1"))

        assertTrue(repository.observeRejections().first().isEmpty())
    }
}
