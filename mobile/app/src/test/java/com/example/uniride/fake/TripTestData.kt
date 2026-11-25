package com.example.uniride.fake

import com.example.uniride.data.local.entity.TripEntity
import com.example.uniride.data.local.entity.UserEntity
import com.example.uniride.data.remote.datasource.FakeBackendStore
import com.example.uniride.domain.model.Direction
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.model.TripStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Rows and users for the repository and sync tests. */
fun tripRow(
    id: String = "trip-1",
    driverId: String = FakeBackendStore.DEFAULT_DRIVER_ID,
    time: LocalTime = LocalTime.of(22, 15),
    date: LocalDate = LocalDate.now().plusDays(3),
    totalSeats: Int = 3,
    status: TripStatus = TripStatus.ABIERTO,
    isSync: Boolean = false,
) = TripEntity(
    tripId = id,
    driverId = driverId,
    driverName = "Lucía F.",
    driverRating = 4.8,
    driverPhone = null,
    direction = Direction.VUELTA,
    campus = "MONSERRAT",
    meetingPoint = "Salida Lima 717",
    destinationNeighborhood = "Ramos Mejía",
    destinationReference = null,
    departureDate = date,
    departureTime = time,
    estimatedArrivalTime = null,
    totalSeats = totalSeats,
    availableSeats = totalSeats,
    contributionPerSeat = 3500,
    vehicleModel = "Gol Trend",
    vehicleColor = "Gris",
    vehiclePlate = "AB123CD",
    status = status,
    isSync = isSync,
    updatedAt = Instant.parse("2026-11-20T21:00:00Z"),
)

fun userRow(id: String = FakeBackendStore.DEFAULT_DRIVER_ID, withVehicle: Boolean = true) = UserEntity(
    id = id,
    email = "lucia@uade.edu.ar",
    fullName = "Lucía Fernández",
    career = "Ingeniería en Informática",
    phone = "1155551234",
    preferredMode = PreferredMode.AMBOS,
    ratingAvg = 4.8,
    ratingCount = 12,
    vehicleBrand = if (withVehicle) "Volkswagen" else null,
    vehicleModel = if (withVehicle) "Gol Trend" else null,
    vehicleColor = if (withVehicle) "Gris" else null,
    vehiclePlate = if (withVehicle) "AB123CD" else null,
)
