package com.example.uniride.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.uniride.domain.model.Direction
import com.example.uniride.domain.model.TripStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * A trip, with the driver and the vehicle flattened into columns (preentrega 11.1).
 *
 * `driverPhone` is not in 11.1: it is added because RF04 requires showing the contact of a
 * confirmed trip without connection. It is `null` until the user has an accepted reservation
 * on the trip (RNF07). `vehiclePlate` follows the same rule.
 *
 * `isSync` is `false` while the trip was created or changed on this device and the server did
 * not confirm it yet.
 */
@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val tripId: String,
    val driverId: String,
    val driverName: String,
    val driverRating: Double?,
    val driverPhone: String?,
    val direction: Direction,
    /** Code of the campus (`CampusEntity.code`). */
    val campus: String,
    val meetingPoint: String,
    val destinationNeighborhood: String,
    val destinationReference: String?,
    val departureDate: LocalDate,
    val departureTime: LocalTime,
    val estimatedArrivalTime: LocalTime?,
    val totalSeats: Int,
    val availableSeats: Int,
    val contributionPerSeat: Int,
    val vehicleModel: String,
    val vehicleColor: String,
    val vehiclePlate: String?,
    val status: TripStatus,
    val isSync: Boolean,
    val updatedAt: Instant,
)
