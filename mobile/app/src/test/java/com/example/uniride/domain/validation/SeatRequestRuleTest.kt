package com.example.uniride.domain.validation

import com.example.uniride.domain.model.DomainFixtures.DRIVER_ID
import com.example.uniride.domain.model.DomainFixtures.NOW
import com.example.uniride.domain.model.DomainFixtures.PASSENGER_ID
import com.example.uniride.domain.model.DomainFixtures.reservation
import com.example.uniride.domain.model.DomainFixtures.trip
import com.example.uniride.domain.model.Reservation
import com.example.uniride.domain.model.ReservationStatus
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.TripStatus
import com.example.uniride.domain.model.UserId
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SeatRequestRuleTest {

    private fun decide(
        trip: Trip = trip(),
        userId: UserId = PASSENGER_ID,
        myReservation: Reservation? = null,
    ) = SeatRequestRule.evaluate(trip, userId, myReservation, NOW)

    @Test
    fun `un viaje abierto con plazas permite pedir asiento`() {
        assertEquals(SeatRequestDecision.ALLOWED, decide())
    }

    @Test
    fun `despues de una reserva rechazada o cancelada se puede volver a pedir`() {
        assertEquals(SeatRequestDecision.ALLOWED, decide(myReservation = reservation(ReservationStatus.RECHAZADA)))
        assertEquals(SeatRequestDecision.ALLOWED, decide(myReservation = reservation(ReservationStatus.CANCELADA)))
    }

    @Test
    fun `el conductor no puede pedir asiento en su propio viaje`() {
        assertEquals(SeatRequestDecision.OWN_TRIP, decide(userId = DRIVER_ID))
    }

    @Test
    fun `con una reserva pendiente o aceptada no se pide otra`() {
        assertEquals(SeatRequestDecision.ALREADY_RESERVED, decide(myReservation = reservation(ReservationStatus.PENDIENTE)))
        assertEquals(SeatRequestDecision.ALREADY_RESERVED, decide(myReservation = reservation(ReservationStatus.ACEPTADA)))
    }

    @Test
    fun `un viaje que ya salio no acepta pedidos`() {
        assertEquals(SeatRequestDecision.DEPARTED, decide(trip = trip(date = LocalDate.of(2026, 11, 19))))
    }

    @Test
    fun `un viaje completo o sin plazas no acepta pedidos`() {
        assertEquals(SeatRequestDecision.NO_SEATS, decide(trip = trip(status = TripStatus.COMPLETO, availableSeats = 0)))
        assertEquals(SeatRequestDecision.NO_SEATS, decide(trip = trip(availableSeats = 0)))
    }

    @Test
    fun `un viaje finalizado o cancelado no acepta pedidos`() {
        assertEquals(SeatRequestDecision.TRIP_NOT_OPEN, decide(trip = trip(status = TripStatus.CANCELADO)))
        assertEquals(SeatRequestDecision.TRIP_NOT_OPEN, decide(trip = trip(status = TripStatus.FINALIZADO)))
    }
}
