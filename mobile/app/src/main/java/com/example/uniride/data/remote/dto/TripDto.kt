package com.example.uniride.data.remote.dto

import com.squareup.moshi.JsonClass

/**
 * Trip payload returned by /trips endpoints (docs/API.md §3.3).
 * [vehiclePlate] and [driverPhone] are non-null only for the driver or passengers
 * with an ACCEPTED reservation (RNF07).
 */
@JsonClass(generateAdapter = true)
data class TripDto(
    val tripId: String,
    val driverId: String,
    val driverName: String,
    val driverRating: Double? = null,
    val direction: String,
    val campus: String,
    val meetingPoint: String,
    val destinationNeighborhood: String,
    val destinationReference: String? = null,
    val departureDate: String,
    val departureTime: String,
    val estimatedArrivalTime: String? = null,
    val totalSeats: Int,
    val availableSeats: Int,
    val contributionPerSeat: Int,
    val vehicleModel: String,
    val vehicleColor: String,
    val vehiclePlate: String? = null,
    val driverPhone: String? = null,
    val status: String,
    val myReservation: ReservationDto? = null,
    val updatedAt: String,
)

/**
 * Request body for idempotent PUT /trips/{tripId} (docs/API.md §5.4).
 */
@JsonClass(generateAdapter = true)
data class PublishTripRequest(
    val direction: String,
    val campus: String,
    val meetingPoint: String,
    val destinationNeighborhood: String,
    val destinationReference: String? = null,
    val departureDate: String,
    val departureTime: String,
    val estimatedArrivalTime: String? = null,
    val totalSeats: Int,
    val contributionPerSeat: Int,
)

/**
 * Campus and predefined meeting points returned by GET /campuses (docs/API.md §5.2).
 */
@JsonClass(generateAdapter = true)
data class CampusDto(
    val code: String,
    val name: String,
    val meetingPoints: List<String>,
)
