package com.example.uniride.domain.model

import java.time.Instant

/**
 * Seat request of a passenger (`ReservationDto`, docs/API.md §3.4). Same field names as the
 * API. [passengerPhone] only has a value for the driver once the reservation is accepted
 * (RNF07).
 */
data class Reservation(
    val reservationId: String,
    val tripId: String,
    val passengerId: String,
    /** "Martín G." */
    val passengerName: String,
    val passengerRating: Double?,
    val passengerPhone: String?,
    val status: ReservationStatus,
    val reservedAt: Instant,
    val updatedAt: Instant,
    /** `false` while the request or its change was made offline and is not sent yet. */
    val isSync: Boolean = true,
) {
    /** Holds or may hold a seat: `PENDIENTE` or `ACEPTADA`. */
    val isActive: Boolean
        get() = status == ReservationStatus.PENDIENTE || status == ReservationStatus.ACEPTADA
}
