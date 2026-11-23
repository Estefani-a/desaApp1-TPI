package com.example.uniride.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Domain objects for the tests. "Now" is 2026-11-20 18:00 in Buenos Aires. */
object DomainFixtures {

    val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 20, 18, 0)
    private val UPDATED_AT: Instant = Instant.parse("2026-11-20T21:00:00Z")

    val DRIVER_ID = UserId("driver-1")
    val PASSENGER_ID = UserId("passenger-1")
    val TRIP_ID = TripId("trip-1")

    fun trip(
        campus: CampusCode = CampusCode("MONSERRAT"),
        direction: Direction = Direction.VUELTA,
        date: LocalDate = LocalDate.of(2026, 11, 20),
        time: LocalTime = LocalTime.of(22, 15),
        status: TripStatus = TripStatus.ABIERTO,
        availableSeats: Int = 2,
    ) = Trip(
        id = TRIP_ID,
        driver = Participant(DRIVER_ID, "Lucía F.", 4.8, phone = null),
        campus = campus,
        route = TripRoute(direction, "Salida Lima 717", "Ramos Mejía", destinationReference = null),
        schedule = TripSchedule(date, time, estimatedArrivalTime = null),
        totalSeats = 3,
        availableSeats = availableSeats,
        contributionPerSeat = 3500,
        vehicle = TripVehicle("Gol Trend", "Gris", plate = null),
        status = status,
        updatedAt = UPDATED_AT,
    )

    fun reservation(status: ReservationStatus, passengerId: UserId = PASSENGER_ID) = Reservation(
        id = ReservationId("res-1"),
        tripId = TRIP_ID,
        passenger = Participant(passengerId, "Martín G.", null, phone = null),
        status = status,
        reservedAt = UPDATED_AT,
        updatedAt = UPDATED_AT,
    )
}
