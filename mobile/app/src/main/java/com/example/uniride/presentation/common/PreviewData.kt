package com.example.uniride.presentation.common

import com.example.uniride.domain.model.CampusCode
import com.example.uniride.domain.model.Direction
import com.example.uniride.domain.model.Participant
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.model.TripRoute
import com.example.uniride.domain.model.TripSchedule
import com.example.uniride.domain.model.TripStatus
import com.example.uniride.domain.model.TripVehicle
import com.example.uniride.domain.model.UserId
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Sample data for the `@Preview`s, so the screens can be reviewed without backend. */
object PreviewData {

    private val today: LocalDate = LocalDate.now(BusinessZone)

    val trip = Trip(
        id = TripId("0b7c4f0e-5d1a-4f5a-8a52-7c9e5e0d2b31"),
        driver = Participant(UserId("driver-1"), displayName = "Lucía F.", rating = 4.8, phone = null),
        campus = CampusCode("MONSERRAT"),
        route = TripRoute(
            direction = Direction.VUELTA,
            meetingPoint = "Salida Lima 717",
            destinationNeighborhood = "Ramos Mejía",
            destinationReference = "Estación de tren",
        ),
        schedule = TripSchedule(today, LocalTime.of(22, 15), LocalTime.of(23, 0)),
        totalSeats = 3,
        availableSeats = 2,
        contributionPerSeat = 3500,
        vehicle = TripVehicle(model = "Gol Trend", color = "Gris", plate = null),
        status = TripStatus.ABIERTO,
        updatedAt = Instant.parse("2026-11-20T21:40:12-03:00"),
    )

    val fullTrip = trip.copy(
        id = TripId("1a9d3e2f-4c8b-4d6e-9f01-2b3c4d5e6f70"),
        driver = Participant(UserId("driver-2"), displayName = "Sofía P.", rating = null, phone = null),
        campus = CampusCode("BELGRANO"),
        route = TripRoute(Direction.IDA, "Salida 11 de Septiembre 1990", "Palermo", "Plaza Italia"),
        schedule = TripSchedule(today.plusDays(1), LocalTime.of(8, 0), LocalTime.of(8, 30)),
        totalSeats = 2,
        availableSeats = 0,
        contributionPerSeat = 0,
        status = TripStatus.COMPLETO,
    )

    val trips = listOf(trip, fullTrip)
}
