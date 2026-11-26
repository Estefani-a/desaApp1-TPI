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
) {
    val isActive: Boolean get() = status.isActive

    /** Only a pending request can be accepted or rejected by the driver. */
    val canBeAnswered: Boolean get() = status == ReservationStatus.PENDIENTE

    /** The passenger can cancel while it holds or may hold a seat. */
    val canBeCancelled: Boolean get() = status.isActive

    companion object {
        /**
         * The reservation of a passenger on a trip, out of all they made there: the active one,
         * otherwise the newest (for example, a rejected request). Same rule as the server.
         */
        fun currentOf(reservations: List<Reservation>): Reservation? =
            reservations.firstOrNull { it.isActive } ?: reservations.maxByOrNull { it.reservedAt }
    }
}

/**
 * Why the server did not accept a change made on this device. The screens show a message for
 * each one; it stays until the user dismisses it.
 */
enum class ReservationRejection {
    /** The trip was full when the request or the acceptance arrived. */
    NO_SEATS,

    /** The trip was finished or cancelled. */
    TRIP_NOT_OPEN,

    /** The passenger already had another request on the trip. */
    ALREADY_RESERVED,

    /** The trip had already started: the reservation can no longer change (the seat is kept). */
    TRIP_ALREADY_DEPARTED,

    /** Anything else; the reservation shows the status the server has. */
    OTHER,
}
