package com.example.uniride.data.remote.dto

/**
 * Reservation payload returned by /trips/{tripId}/reservations endpoints (docs/API.md §3.4).
 * [passengerPhone] is non-null only for the driver when [status] is ACEPTADA (RNF07).
 */
data class ReservationDto(
    val reservationId: String,
    val tripId: String,
    val passengerId: String,
    val passengerName: String,
    val passengerRating: Double? = null,
    val passengerPhone: String? = null,
    val status: String,
    val reservedAt: String,
    val updatedAt: String,
)
