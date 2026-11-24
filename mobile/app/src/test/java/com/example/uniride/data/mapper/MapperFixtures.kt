package com.example.uniride.data.mapper

import com.example.uniride.domain.model.Campus
import com.example.uniride.domain.model.CampusCode
import com.example.uniride.domain.model.Direction
import com.example.uniride.domain.model.Participant
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.Rating
import com.example.uniride.domain.model.Reservation
import com.example.uniride.domain.model.ReservationId
import com.example.uniride.domain.model.ReservationStatus
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.model.TripRoute
import com.example.uniride.domain.model.TripSchedule
import com.example.uniride.domain.model.TripStatus
import com.example.uniride.domain.model.TripVehicle
import com.example.uniride.domain.model.User
import com.example.uniride.domain.model.UserId
import com.example.uniride.domain.model.Vehicle
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Domain objects with every optional field filled, or all of them empty, for the mapper tests. */
object MapperFixtures {

    private val INSTANT: Instant = Instant.parse("2026-11-20T21:00:00.250Z")

    fun fullTrip() = Trip(
        id = TripId("trip-1"),
        driver = Participant(UserId("driver-1"), "Lucía F.", 4.8, "1155551234"),
        campus = CampusCode("MONSERRAT"),
        route = TripRoute(Direction.VUELTA, "Salida Lima 717", "Ramos Mejía", "Frente a la plaza"),
        schedule = TripSchedule(LocalDate.of(2026, 11, 20), LocalTime.of(22, 15), LocalTime.of(23, 0)),
        totalSeats = 3,
        availableSeats = 2,
        contributionPerSeat = 3500,
        vehicle = TripVehicle("Gol Trend", "Gris", "AB123CD"),
        status = TripStatus.ABIERTO,
        updatedAt = INSTANT,
    )

    fun minimalTrip() = fullTrip().copy(
        driver = Participant(UserId("driver-1"), "Lucía F.", rating = null, phone = null),
        route = fullTrip().route.copy(destinationReference = null),
        schedule = fullTrip().schedule.copy(estimatedArrivalTime = null),
        vehicle = TripVehicle("Gol Trend", "Gris", plate = null),
    )

    fun reservation(passenger: Participant) = Reservation(
        id = ReservationId("res-1"),
        tripId = TripId("trip-1"),
        passenger = passenger,
        status = ReservationStatus.PENDIENTE,
        reservedAt = INSTANT,
        updatedAt = INSTANT.plusSeconds(60),
    )

    fun fullPassenger() = Participant(UserId("passenger-1"), "Martín G.", 4.2, "1155559876")

    fun emptyPassenger() = Participant(UserId("passenger-1"), "Martín G.", rating = null, phone = null)

    fun driverUser() = User(
        id = UserId("driver-1"),
        email = "lucia@uade.edu.ar",
        profile = Profile(
            fullName = "Lucía Fernández",
            career = "Ingeniería en Informática",
            phone = "1155551234",
            preferredMode = PreferredMode.AMBOS,
            vehicle = Vehicle("Volkswagen", "Gol Trend", "Gris", "AB123CD"),
        ),
        rating = Rating(4.8, 12),
    )

    fun passengerUser() = driverUser().copy(
        profile = driverUser().profile.copy(preferredMode = PreferredMode.PASAJERO, vehicle = null),
        rating = null,
    )

    fun campus() = Campus(CampusCode("MONSERRAT"), "Monserrat", listOf("Salida Lima 717", "Lima 757, esquina"))
}
