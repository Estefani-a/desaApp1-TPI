package com.example.uniride.domain.validation

import com.example.uniride.domain.model.Reservation
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.TripStatus
import com.example.uniride.domain.model.UserId
import java.time.LocalDateTime

/** Why a user can or cannot ask for a seat. The screen shows a message for each case. */
enum class SeatRequestDecision {
    ALLOWED,
    OWN_TRIP,
    ALREADY_RESERVED,
    TRIP_NOT_OPEN,
    NO_SEATS,
    DEPARTED,
}

/**
 * Whether [userId] can ask for a seat on a trip (RF04). Same rules as the server, so the
 * button is not offered when the request would be rejected anyway.
 */
object SeatRequestRule {

    /**
     * @param userId who asks; `null` without a session, and then only the trip is evaluated
     * @param myReservation the user's reservation on this trip, if any
     * @param now current time in Buenos Aires
     */
    fun evaluate(trip: Trip, userId: UserId?, myReservation: Reservation?, now: LocalDateTime): SeatRequestDecision =
        when {
            userId != null && trip.isDrivenBy(userId) -> SeatRequestDecision.OWN_TRIP
            myReservation?.isActive == true -> SeatRequestDecision.ALREADY_RESERVED
            trip.schedule.hasDepartedAt(now) -> SeatRequestDecision.DEPARTED
            trip.status != TripStatus.ABIERTO && trip.status != TripStatus.COMPLETO -> SeatRequestDecision.TRIP_NOT_OPEN
            trip.availableSeats == 0 || trip.status == TripStatus.COMPLETO -> SeatRequestDecision.NO_SEATS
            else -> SeatRequestDecision.ALLOWED
        }
}
