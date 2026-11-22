package com.example.uniride.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class DomainModelTest {

    private val now = Instant.parse("2026-11-20T21:40:12Z")

    private fun trip(
        campus: String = "MONSERRAT",
        direction: Direction = Direction.VUELTA,
        date: LocalDate = LocalDate.of(2026, 11, 20),
        time: LocalTime = LocalTime.of(22, 15),
        status: TripStatus = TripStatus.ABIERTO,
        availableSeats: Int = 2,
        myReservation: Reservation? = null,
    ) = Trip(
        tripId = "trip-1",
        driverId = "driver-1",
        driverName = "Lucía F.",
        driverRating = 4.8,
        direction = direction,
        campus = campus,
        meetingPoint = "Salida Lima 717",
        destinationNeighborhood = "Ramos Mejía",
        destinationReference = null,
        departureDate = date,
        departureTime = time,
        estimatedArrivalTime = null,
        totalSeats = 3,
        availableSeats = availableSeats,
        contributionPerSeat = 3500,
        vehicleModel = "Gol Trend",
        vehicleColor = "Gris",
        vehiclePlate = null,
        driverPhone = null,
        status = status,
        myReservation = myReservation,
        updatedAt = now,
    )

    private fun reservation(status: ReservationStatus) = Reservation(
        reservationId = "res-1",
        tripId = "trip-1",
        passengerId = "passenger-1",
        passengerName = "Martín G.",
        passengerRating = null,
        passengerPhone = null,
        status = status,
        reservedAt = now,
        updatedAt = now,
    )

    @Test
    fun `el turno usa las franjas acordadas en A0`() {
        assertEquals(Shift.MANANA, Shift.of(LocalTime.of(0, 0)))
        assertEquals(Shift.MANANA, Shift.of(LocalTime.of(12, 59)))
        assertEquals(Shift.TARDE, Shift.of(LocalTime.of(13, 0)))
        assertEquals(Shift.TARDE, Shift.of(LocalTime.of(17, 59)))
        assertEquals(Shift.NOCHE, Shift.of(LocalTime.of(18, 0)))
        assertEquals(Shift.NOCHE, Shift.of(LocalTime.of(23, 59)))
    }

    @Test
    fun `filtros vacios aceptan cualquier viaje`() {
        assertTrue(TripFilters().isEmpty)
        assertTrue(TripFilters().matches(trip()))
    }

    @Test
    fun `los filtros se combinan con Y`() {
        val filters = TripFilters(
            campus = "MONSERRAT",
            direction = Direction.VUELTA,
            date = LocalDate.of(2026, 11, 20),
            shift = Shift.NOCHE,
        )

        assertTrue(filters.matches(trip()))
        assertFalse(filters.matches(trip(campus = "BELGRANO")))
        assertFalse(filters.matches(trip(direction = Direction.IDA)))
        assertFalse(filters.matches(trip(date = LocalDate.of(2026, 11, 21))))
        assertFalse(filters.matches(trip(time = LocalTime.of(9, 0))))
    }

    @Test
    fun `se puede pedir asiento en un viaje abierto con plazas y sin reserva activa`() {
        assertTrue(trip().canRequestSeat)
        assertTrue(trip(myReservation = reservation(ReservationStatus.RECHAZADA)).canRequestSeat)
        assertTrue(trip(myReservation = reservation(ReservationStatus.CANCELADA)).canRequestSeat)
    }

    @Test
    fun `no se puede pedir asiento sin plazas, con el viaje cerrado o con una reserva activa`() {
        assertFalse(trip(availableSeats = 0).canRequestSeat)
        assertFalse(trip(status = TripStatus.COMPLETO).canRequestSeat)
        assertFalse(trip(status = TripStatus.CANCELADO).canRequestSeat)
        assertFalse(trip(myReservation = reservation(ReservationStatus.PENDIENTE)).canRequestSeat)
        assertFalse(trip(myReservation = reservation(ReservationStatus.ACEPTADA)).canRequestSeat)
    }

    @Test
    fun `una reserva activa es la pendiente o la aceptada`() {
        assertTrue(reservation(ReservationStatus.PENDIENTE).isActive)
        assertTrue(reservation(ReservationStatus.ACEPTADA).isActive)
        assertFalse(reservation(ReservationStatus.RECHAZADA).isActive)
        assertFalse(reservation(ReservationStatus.CANCELADA).isActive)
    }

    @Test
    fun `solo puede manejar quien cargo un vehiculo`() {
        val user = User(
            id = "user-1",
            fullName = "Lucía Fernández",
            email = "conductora.demo@uade.edu.ar",
            career = "Ingeniería en Informática",
            phone = "+5491155550000",
            preferredMode = PreferredMode.AMBOS,
            ratingAvg = null,
            ratingCount = 0,
            vehicle = null,
        )

        assertFalse(user.canDrive)
        assertTrue(user.copy(vehicle = Vehicle("Volkswagen", "Gol Trend", "Gris", "AB123CD")).canDrive)
    }

    @Test
    fun `los enums tienen los valores del contrato`() {
        assertEquals(listOf("IDA", "VUELTA"), Direction.entries.map { it.name })
        assertEquals(
            listOf("ABIERTO", "COMPLETO", "FINALIZADO", "CANCELADO"),
            TripStatus.entries.map { it.name },
        )
        assertEquals(
            listOf("PENDIENTE", "ACEPTADA", "RECHAZADA", "CANCELADA"),
            ReservationStatus.entries.map { it.name },
        )
        assertEquals(listOf("PASAJERO", "CONDUCTOR", "AMBOS"), PreferredMode.entries.map { it.name })
        assertEquals(listOf("MANANA", "TARDE", "NOCHE"), Shift.entries.map { it.name })
    }
}
