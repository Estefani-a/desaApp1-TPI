package com.example.uniride.domain.usecase.reservation

import com.example.uniride.domain.model.Reservation
import com.example.uniride.domain.model.ReservationRejection
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.repository.ProfileRepository
import com.example.uniride.domain.repository.ReservationRepository
import com.example.uniride.domain.repository.TripRepository
import com.example.uniride.domain.validation.SeatRequestDecision
import com.example.uniride.domain.validation.SeatRequestRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

/** Everything the trip detail (B3) shows for the user who looks at it. */
data class TripDetail(
    val trip: Trip,
    /** The user's reservation on the trip (the active one, or the latest), or `null`. */
    val myReservation: Reservation?,
    val isDriver: Boolean,
    /** Whether "Solicitar asiento" is offered, and why not. */
    val seatRequest: SeatRequestDecision,
    val hasStarted: Boolean,
    /** The trip or the user's reservation changed on the phone and was not sent yet. */
    val isPendingSync: Boolean,
    /** Why the server did not accept the last change of the user's reservation, if it did not. */
    val rejection: ReservationRejection?,
) {
    /** "Cancelar" is offered while the reservation is active and the trip did not start. */
    val canCancel: Boolean get() = myReservation?.canBeCancelled == true && !hasStarted
}

/**
 * The trip detail (RF04, B3), from the phone: it works offline and updates itself when Room
 * changes. `null` if the trip is not on the phone. Without a session the trip is still shown, as
 * to any passenger; asking for a seat is what needs one.
 */
class ObserveTripDetailUseCase @Inject constructor(
    private val tripRepository: TripRepository,
    private val reservationRepository: ReservationRepository,
    private val profileRepository: ProfileRepository,
    private val clock: Clock,
) {
    operator fun invoke(tripId: TripId): Flow<TripDetail?> {
        val tripAndMine = combine(
            tripRepository.observeTrip(tripId),
            reservationRepository.observeMyReservation(tripId),
            profileRepository.observeCurrentUser(),
        ) { trip, mine, user -> Triple(trip, mine, user) }

        return combine(
            tripAndMine,
            tripRepository.observePendingTripIds(),
            reservationRepository.observePendingReservationIds(),
            reservationRepository.observeRejections(),
        ) { (trip, mine, user), pendingTrips, pendingReservations, rejections ->
            if (trip == null) return@combine null
            val now = LocalDateTime.now(clock)
            val userId = user?.id
            TripDetail(
                trip = trip,
                myReservation = mine,
                isDriver = userId != null && trip.isDrivenBy(userId),
                seatRequest = SeatRequestRule.evaluate(trip, userId, mine, now),
                hasStarted = trip.schedule.hasDepartedAt(now),
                isPendingSync = trip.id in pendingTrips || (mine != null && mine.id in pendingReservations),
                rejection = mine?.let { rejections[it.id] },
            )
        }.distinctUntilChanged()
    }
}
