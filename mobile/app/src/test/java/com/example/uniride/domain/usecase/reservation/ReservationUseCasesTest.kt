package com.example.uniride.domain.usecase.reservation

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.DomainFixtures.DRIVER_ID
import com.example.uniride.domain.model.DomainFixtures.PASSENGER_ID
import com.example.uniride.domain.model.DomainFixtures.TRIP_ID
import com.example.uniride.domain.model.DomainFixtures.reservation
import com.example.uniride.domain.model.DomainFixtures.trip
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.ReservationId
import com.example.uniride.domain.model.ReservationRejection
import com.example.uniride.domain.model.ReservationStatus
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.model.TripStatus
import com.example.uniride.domain.model.User
import com.example.uniride.domain.model.UserId
import com.example.uniride.domain.validation.SeatRequestDecision
import com.example.uniride.fake.FakeProfileRepository
import com.example.uniride.fake.FakeReservationRepository
import com.example.uniride.fake.FakeTripRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Use cases of RF04. "Now" is 2026-11-20 18:00 and the trip of the fixtures leaves at 22:15. */
class ReservationUseCasesTest {

    private val clock = Clock.fixed(Instant.parse("2026-11-20T21:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"))
    private val yesterday = LocalDate.of(2026, 11, 19)

    private val trips = FakeTripRepository(listOf(trip()))
    private val reservations = FakeReservationRepository(currentUserId = PASSENGER_ID.value)
    private val profiles = FakeProfileRepository(user(PASSENGER_ID))

    private fun user(id: UserId) = User(
        id = id,
        email = "${id.value}@uade.edu.ar",
        profile = Profile("Martín Gómez", "Administración", "+5491155550001", PreferredMode.PASAJERO, vehicle = null),
        rating = null,
    )

    private fun givenTrip(trip: Trip) {
        trips.trips.value = listOf(trip)
    }

    private fun loginAsDriver() {
        profiles.user.value = user(DRIVER_ID)
        reservations.currentUserId = DRIVER_ID.value
    }

    private val requestSeat get() = RequestSeatUseCase(trips, reservations, profiles, clock)
    private val respond get() = RespondToRequestUseCase(trips, reservations, clock)
    private val cancel get() = CancelReservationUseCase(trips, reservations, clock)
    private val detail get() = ObserveTripDetailUseCase(trips, reservations, profiles, clock)
    private val myTrip get() = ObserveMyActiveTripUseCase(trips, reservations)
    private val panel get() = ObserveDriverPanelUseCase(trips, reservations, clock)

    // --- Solicitar ---

    @Test
    fun `pedir asiento en un viaje abierto lo guarda`() = runTest {
        assertEquals(RequestSeatResult.Requested(ReservationId("nueva")), requestSeat(TRIP_ID))
        assertEquals(listOf("requestSeat:${TRIP_ID.value}"), reservations.calls)
    }

    @Test
    fun `el conductor no puede pedir asiento en su viaje`() = runTest {
        loginAsDriver()

        assertEquals(RequestSeatResult.NotAllowed(SeatRequestDecision.OWN_TRIP), requestSeat(TRIP_ID))
        assertTrue(reservations.calls.isEmpty())
    }

    @Test
    fun `con una reserva pendiente no se pide otra`() = runTest {
        reservations.reservations.value = listOf(reservation(ReservationStatus.PENDIENTE))

        assertEquals(RequestSeatResult.NotAllowed(SeatRequestDecision.ALREADY_RESERVED), requestSeat(TRIP_ID))
    }

    @Test
    fun `un viaje que ya salio no acepta pedidos`() = runTest {
        givenTrip(trip(date = yesterday))

        assertEquals(RequestSeatResult.NotAllowed(SeatRequestDecision.DEPARTED), requestSeat(TRIP_ID))
    }

    @Test
    fun `sin sesion pedir asiento falla con UNAUTHORIZED`() = runTest {
        profiles.user.value = null

        assertEquals(RequestSeatResult.Failed(DataError.UNAUTHORIZED), requestSeat(TRIP_ID))
    }

    @Test
    fun `un viaje que no esta en el telefono falla con NOT_FOUND`() = runTest {
        assertEquals(RequestSeatResult.Failed(DataError.NOT_FOUND), requestSeat(TripId("otro")))
    }

    // --- Responder ---

    @Test
    fun `aceptar una solicitud pendiente la guarda`() = runTest {
        reservations.reservations.value = listOf(reservation(ReservationStatus.PENDIENTE))

        assertEquals(ReservationChangeResult.Done, respond(TRIP_ID, ReservationId("res-1"), accept = true))
        assertEquals(listOf("accept:res-1"), reservations.calls)
    }

    @Test
    fun `rechazar una solicitud pendiente la guarda`() = runTest {
        reservations.reservations.value = listOf(reservation(ReservationStatus.PENDIENTE))

        assertEquals(ReservationChangeResult.Done, respond(TRIP_ID, ReservationId("res-1"), accept = false))
        assertEquals(listOf("reject:res-1"), reservations.calls)
    }

    @Test
    fun `solo se responde una solicitud pendiente`() = runTest {
        reservations.reservations.value = listOf(reservation(ReservationStatus.ACEPTADA))

        assertEquals(
            ReservationChangeResult.NotAllowed(ReservationChangeBlock.NOT_PENDING),
            respond(TRIP_ID, ReservationId("res-1"), accept = false),
        )
    }

    @Test
    fun `sin vacantes no se acepta`() = runTest {
        givenTrip(trip(status = TripStatus.COMPLETO, availableSeats = 0))
        reservations.reservations.value = listOf(reservation(ReservationStatus.PENDIENTE))

        assertEquals(
            ReservationChangeResult.NotAllowed(ReservationChangeBlock.NO_SEATS),
            respond(TRIP_ID, ReservationId("res-1"), accept = true),
        )
    }

    @Test
    fun `sin vacantes igual se puede rechazar`() = runTest {
        givenTrip(trip(status = TripStatus.COMPLETO, availableSeats = 0))
        reservations.reservations.value = listOf(reservation(ReservationStatus.PENDIENTE))

        assertEquals(ReservationChangeResult.Done, respond(TRIP_ID, ReservationId("res-1"), accept = false))
    }

    @Test
    fun `despues de la salida no se responden solicitudes`() = runTest {
        givenTrip(trip(date = yesterday))
        reservations.reservations.value = listOf(reservation(ReservationStatus.PENDIENTE))

        assertEquals(
            ReservationChangeResult.NotAllowed(ReservationChangeBlock.TRIP_ALREADY_DEPARTED),
            respond(TRIP_ID, ReservationId("res-1"), accept = true),
        )
    }

    // --- Cancelar ---

    @Test
    fun `cancelar mi reserva aceptada la guarda`() = runTest {
        reservations.reservations.value = listOf(reservation(ReservationStatus.ACEPTADA))

        assertEquals(ReservationChangeResult.Done, cancel(TRIP_ID, ReservationId("res-1")))
        assertEquals(listOf("cancel:res-1"), reservations.calls)
    }

    @Test
    fun `no se puede cancelar un viaje que ya empezo`() = runTest {
        givenTrip(trip(date = yesterday))
        reservations.reservations.value = listOf(reservation(ReservationStatus.ACEPTADA))

        assertEquals(
            ReservationChangeResult.NotAllowed(ReservationChangeBlock.TRIP_ALREADY_DEPARTED),
            cancel(TRIP_ID, ReservationId("res-1")),
        )
        assertTrue(reservations.calls.isEmpty())
    }

    @Test
    fun `una reserva rechazada no se cancela`() = runTest {
        reservations.reservations.value = listOf(reservation(ReservationStatus.RECHAZADA))

        assertEquals(
            ReservationChangeResult.NotAllowed(ReservationChangeBlock.NOT_ACTIVE),
            cancel(TRIP_ID, ReservationId("res-1")),
        )
    }

    @Test
    fun `cancelar una reserva que no es mia falla con NOT_FOUND`() = runTest {
        reservations.reservations.value = listOf(reservation(ReservationStatus.ACEPTADA, passengerId = UserId("otro")))

        assertEquals(ReservationChangeResult.Failed(DataError.NOT_FOUND), cancel(TRIP_ID, ReservationId("res-1")))
    }

    // --- Detalle (B3) ---

    @Test
    fun `el detalle ofrece pedir asiento a un pasajero sin reserva`() = runTest {
        val result = detail(TRIP_ID).first()!!

        assertFalse(result.isDriver)
        assertEquals(SeatRequestDecision.ALLOWED, result.seatRequest)
        assertNull(result.myReservation)
        assertFalse(result.canCancel)
    }

    @Test
    fun `el detalle muestra mi reserva pendiente de sincronizar y el motivo de un rechazo`() = runTest {
        val mine = reservation(ReservationStatus.ACEPTADA)
        reservations.reservations.value = listOf(mine)
        reservations.pendingIds.value = setOf(mine.id)
        reservations.rejections.value = mapOf(mine.id to ReservationRejection.TRIP_ALREADY_DEPARTED)

        val result = detail(TRIP_ID).first()!!

        assertEquals(mine, result.myReservation)
        assertTrue(result.isPendingSync)
        assertEquals(ReservationRejection.TRIP_ALREADY_DEPARTED, result.rejection)
        assertTrue(result.canCancel)
    }

    @Test
    fun `cuando el viaje empezo el detalle no ofrece cancelar`() = runTest {
        givenTrip(trip(date = yesterday))
        reservations.reservations.value = listOf(reservation(ReservationStatus.ACEPTADA))

        val result = detail(TRIP_ID).first()!!

        assertTrue(result.hasStarted)
        assertFalse(result.canCancel)
    }

    @Test
    fun `el conductor ve su viaje como propio`() = runTest {
        loginAsDriver()

        val result = detail(TRIP_ID).first()!!

        assertTrue(result.isDriver)
        assertEquals(SeatRequestDecision.OWN_TRIP, result.seatRequest)
    }

    // --- Mi viaje (B6) ---

    @Test
    fun `mi viaje es el de mi reserva activa`() = runTest {
        reservations.reservations.value = listOf(reservation(ReservationStatus.ACEPTADA))

        assertEquals(TRIP_ID, myTrip().first()?.trip?.id)
    }

    @Test
    fun `sin reserva activa no hay viaje`() = runTest {
        reservations.reservations.value = listOf(reservation(ReservationStatus.RECHAZADA))

        assertNull(myTrip().first())
    }

    @Test
    fun `un viaje cancelado por el conductor no es mi viaje`() = runTest {
        givenTrip(trip(status = TripStatus.CANCELADO))
        reservations.reservations.value = listOf(reservation(ReservationStatus.ACEPTADA))

        assertNull(myTrip().first())
    }

    @Test
    fun `con dos reservas activas mi viaje es el que sale primero`() = runTest {
        val tomorrow = trip(date = LocalDate.of(2026, 11, 21)).copy(id = TripId("manana"))
        trips.trips.value = listOf(trip(), tomorrow)
        reservations.reservations.value = listOf(
            reservation(ReservationStatus.PENDIENTE).copy(id = ReservationId("res-manana"), tripId = TripId("manana")),
            reservation(ReservationStatus.ACEPTADA),
        )

        assertEquals(TRIP_ID, myTrip().first()?.trip?.id)
    }

    // --- Panel del conductor (C4) ---

    @Test
    fun `el panel muestra primero las solicitudes pendientes`() = runTest {
        loginAsDriver()
        reservations.reservations.value = listOf(
            reservation(ReservationStatus.ACEPTADA).copy(id = ReservationId("aceptada")),
            reservation(ReservationStatus.PENDIENTE).copy(id = ReservationId("pendiente")),
            reservation(ReservationStatus.RECHAZADA).copy(id = ReservationId("rechazada")),
        )
        reservations.pendingIds.value = setOf(ReservationId("aceptada"))

        val result = panel(TRIP_ID).first()!!

        assertEquals(listOf("pendiente", "aceptada", "rechazada"), result.requests.map { it.reservation.id.value })
        assertTrue(result.requests[1].isPendingSync)
        assertTrue(result.requests[0].canBeAnswered)
    }

    @Test
    fun `refrescar el panel devuelve el error de las solicitudes`() = runTest {
        reservations.refreshResult = AppResult.Failure(DataError.NO_CONNECTION)

        assertEquals(AppResult.Failure(DataError.NO_CONNECTION), RefreshDriverPanelUseCase(trips, reservations)(TRIP_ID))
    }
}
