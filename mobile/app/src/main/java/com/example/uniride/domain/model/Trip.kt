package com.example.uniride.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Trip offered by a driver (`TripDto`, docs/API.md §3.3). Same field names as the API.
 *
 * [vehiclePlate] and [driverPhone] only have a value when the user is the driver or has an
 * accepted reservation (RNF07): the backend decides it, the app just shows what arrives.
 */
data class Trip(
    val tripId: String,
    val driverId: String,
    /** "Lucía F." */
    val driverName: String,
    val driverRating: Double?,
    val direction: Direction,
    /** Code of a [Campus], for example `MONSERRAT`. */
    val campus: String,
    val meetingPoint: String,
    val destinationNeighborhood: String,
    val destinationReference: String?,
    val departureDate: LocalDate,
    val departureTime: LocalTime,
    val estimatedArrivalTime: LocalTime?,
    val totalSeats: Int,
    val availableSeats: Int,
    /** Whole pesos (docs/API.md §1). */
    val contributionPerSeat: Int,
    val vehicleModel: String,
    val vehicleColor: String,
    val vehiclePlate: String?,
    val driverPhone: String?,
    val status: TripStatus,
    /** Reservation of the current user on this trip, if any. */
    val myReservation: Reservation?,
    val updatedAt: Instant,
    /** `false` while the trip was created or changed offline and is waiting to be sent (CONVENCIONES §6.2). */
    val isSync: Boolean = true,
) {
    val shift: Shift get() = Shift.of(departureTime)

    fun isDrivenBy(userId: String): Boolean = driverId == userId

    /** A passenger can ask for a seat: open, with seats and no active reservation of theirs. */
    val canRequestSeat: Boolean
        get() = status == TripStatus.ABIERTO && availableSeats > 0 && myReservation?.isActive != true
}

/**
 * Data of the publish form (`PUT /trips/{tripId}`, docs/API.md §5.4). The server fills the
 * driver and vehicle data from the profile.
 */
data class TripDraft(
    val direction: Direction,
    val campus: String,
    val meetingPoint: String,
    val destinationNeighborhood: String,
    val destinationReference: String?,
    val departureDate: LocalDate,
    val departureTime: LocalTime,
    val estimatedArrivalTime: LocalTime?,
    val totalSeats: Int,
    val contributionPerSeat: Int,
)

/** Filters of the feed (RF03). `null` means "any". */
data class TripFilters(
    val campus: String? = null,
    val direction: Direction? = null,
    val date: LocalDate? = null,
    val shift: Shift? = null,
) {
    val isEmpty: Boolean get() = this == TripFilters()

    fun matches(trip: Trip): Boolean =
        (campus == null || trip.campus == campus) &&
            (direction == null || trip.direction == direction) &&
            (date == null || trip.departureDate == date) &&
            (shift == null || trip.shift == shift)
}

/** UADE campus with its predefined meeting points (`GET /campuses`, docs/API.md §5.2). */
data class Campus(
    val code: String,
    val name: String,
    val meetingPoints: List<String>,
)
