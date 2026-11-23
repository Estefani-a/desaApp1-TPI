package com.example.uniride.domain.model

import java.time.Instant

/** A passenger's request for a seat on a trip. */
data class Reservation(
    val id: ReservationId,
    val tripId: TripId,
    val passenger: Participant,
    val status: ReservationStatus,
    val reservedAt: Instant,
    val updatedAt: Instant,
    /** Made or changed without connection and not confirmed by the server yet. */
    val isPendingSync: Boolean,
) {
    val isActive: Boolean get() = status.isActive
}
