package com.example.uniride.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.uniride.data.local.database.UniRideDatabase
import com.example.uniride.data.local.entity.CampusEntity
import com.example.uniride.data.local.entity.ReservationEntity
import com.example.uniride.data.local.entity.TripEntity
import com.example.uniride.data.local.entity.UserEntity
import com.example.uniride.domain.model.Direction
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.model.ReservationStatus
import com.example.uniride.domain.model.TripStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** In-memory database and entity builders shared by the DAO tests. */
fun inMemoryDatabase(): UniRideDatabase = Room.inMemoryDatabaseBuilder(
    ApplicationProvider.getApplicationContext<Context>(),
    UniRideDatabase::class.java,
).build()

private val UPDATED_AT: Instant = Instant.parse("2026-11-20T21:00:00Z")

fun userEntity(id: String = "user-1") = UserEntity(
    id = id,
    email = "lucia@uade.edu.ar",
    fullName = "Lucía Fernández",
    career = "Ingeniería en Informática",
    phone = "1155551234",
    preferredMode = PreferredMode.AMBOS,
    ratingAvg = 4.8,
    ratingCount = 12,
    vehicleBrand = "Volkswagen",
    vehicleModel = "Gol Trend",
    vehicleColor = "Gris",
    vehiclePlate = "AB123CD",
)

fun tripEntity(
    id: String = "trip-1",
    driverId: String = "driver-1",
    campus: String = "MONSERRAT",
    direction: Direction = Direction.VUELTA,
    date: LocalDate = LocalDate.of(2026, 11, 20),
    time: LocalTime = LocalTime.of(22, 15),
    status: TripStatus = TripStatus.ABIERTO,
    isSync: Boolean = true,
) = TripEntity(
    tripId = id,
    driverId = driverId,
    driverName = "Lucía F.",
    driverRating = 4.8,
    driverPhone = null,
    direction = direction,
    campus = campus,
    meetingPoint = "Salida Lima 717",
    destinationNeighborhood = "Ramos Mejía",
    destinationReference = null,
    departureDate = date,
    departureTime = time,
    estimatedArrivalTime = null,
    totalSeats = 3,
    availableSeats = 2,
    contributionPerSeat = 3500,
    vehicleModel = "Gol Trend",
    vehicleColor = "Gris",
    vehiclePlate = null,
    status = status,
    isSync = isSync,
    updatedAt = UPDATED_AT,
)

fun reservationEntity(
    id: String = "res-1",
    tripId: String = "trip-1",
    passengerId: String = "passenger-1",
    reservedAt: Instant = UPDATED_AT,
    isSync: Boolean = true,
) = ReservationEntity(
    reservationId = id,
    tripId = tripId,
    passengerId = passengerId,
    passengerName = "Martín G.",
    passengerRating = null,
    passengerPhone = null,
    status = ReservationStatus.PENDIENTE,
    reservedAt = reservedAt,
    updatedAt = reservedAt,
    isSync = isSync,
)

fun campusEntity(code: String, name: String, vararg meetingPoints: String) =
    CampusEntity(code, name, meetingPoints.toList())
