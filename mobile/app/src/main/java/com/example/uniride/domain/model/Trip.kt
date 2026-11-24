package com.example.uniride.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** A trip a driver offers to or from a campus. */
data class Trip(
    val id: TripId,
    val driver: Participant,
    val campus: CampusCode,
    val route: TripRoute,
    val schedule: TripSchedule,
    val totalSeats: Int,
    val availableSeats: Int,
    /** Whole pesos each passenger contributes. */
    val contributionPerSeat: Int,
    val vehicle: TripVehicle,
    val status: TripStatus,
    val updatedAt: Instant,
) {
    fun isDrivenBy(userId: UserId): Boolean = driver.id == userId
}

/** Where the trip goes: direction, meeting point at the campus and destination. */
data class TripRoute(
    val direction: Direction,
    val meetingPoint: String,
    val destinationNeighborhood: String,
    val destinationReference: String?,
)

/** When the trip leaves, in the time of Buenos Aires. */
data class TripSchedule(
    val departureDate: LocalDate,
    val departureTime: LocalTime,
    val estimatedArrivalTime: LocalTime?,
) {
    val shift: Shift get() = Shift.of(departureTime)

    fun hasDepartedAt(now: LocalDateTime): Boolean = LocalDateTime.of(departureDate, departureTime).isBefore(now)
}

/** The vehicle as shown on a trip. [plate] is only known to people who travel together (RNF07). */
data class TripVehicle(
    val model: String,
    val color: String,
    val plate: String?,
)

/** What a driver fills in to offer a trip. The rest (driver, vehicle, seats left) is derived. */
data class TripDraft(
    val campus: CampusCode,
    val route: TripRoute,
    val schedule: TripSchedule,
    val totalSeats: Int,
    val contributionPerSeat: Int,
)

/** Filters of the feed (RF03). `null` means "any". */
data class TripFilters(
    val campus: CampusCode? = null,
    val direction: Direction? = null,
    val date: LocalDate? = null,
    val shift: Shift? = null,
) {
    val isEmpty: Boolean get() = this == TripFilters()

    fun matches(trip: Trip): Boolean =
        (campus == null || trip.campus == campus) &&
            (direction == null || trip.route.direction == direction) &&
            (date == null || trip.schedule.departureDate == date) &&
            (shift == null || trip.schedule.shift == shift)
}

/** UADE campus with its predefined meeting points. */
data class Campus(
    val code: CampusCode,
    val name: String,
    val meetingPoints: List<String>,
)
