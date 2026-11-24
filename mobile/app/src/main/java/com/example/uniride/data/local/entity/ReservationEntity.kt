package com.example.uniride.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.uniride.domain.model.ReservationStatus
import java.time.Instant

/**
 * A passenger's request for a seat (preentrega 11.1).
 *
 * There is no `ForeignKey` to [TripEntity] on purpose: the trips are replaced when the catalog
 * is refreshed, and with a foreign key that replacement would delete (cascade) or reject the
 * reservations of the user, including the ones not yet synced. `tripId` has an index because
 * the reservations of a trip are queried by it.
 *
 * `passengerPhone` is `null` until the reservation is accepted (RNF07).
 */
@Entity(
    tableName = "reservations",
    indices = [Index("tripId")],
)
data class ReservationEntity(
    @PrimaryKey val reservationId: String,
    val tripId: String,
    val passengerId: String,
    val passengerName: String,
    val passengerRating: Double?,
    val passengerPhone: String?,
    val status: ReservationStatus,
    val reservedAt: Instant,
    val updatedAt: Instant,
    val isSync: Boolean,
)
