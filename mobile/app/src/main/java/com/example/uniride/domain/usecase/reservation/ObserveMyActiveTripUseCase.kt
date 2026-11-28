package com.example.uniride.domain.usecase.reservation

import com.example.uniride.domain.model.Reservation
import com.example.uniride.domain.model.ReservationRejection
import com.example.uniride.domain.model.ReservationStatus
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.TripStatus
import com.example.uniride.domain.repository.ReservationRepository
import com.example.uniride.domain.repository.TripRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

/** The trip the passenger is going on (B6 "Mi viaje" and the card of the start). */
data class MyTrip(
    val trip: Trip,
    val reservation: Reservation,
    /** The reservation changed on the phone and was not sent yet. */
    val isPendingSync: Boolean,
    /** Why the server did not accept the last change, if it did not (for example, a late cancel). */
    val rejection: ReservationRejection?,
) {
    /**
     * The driver cancelled the whole trip (docs/API.md §5.6). No push is sent for it, so the
     * start is where the passenger finds out.
     */
    val isCancelledByDriver: Boolean get() = trip.status == TripStatus.CANCELADO
}

/**
 * The passenger's next trip (RF04, B6): among the trips where they hold a pending or accepted
 * reservation and that are not finished or cancelled, the one that leaves first. If there is
 * none, a trip the driver cancelled that has not left yet, so the passenger sees why it is
 * gone. It reads only the phone, so a confirmed trip is readable offline (preentrega 12.1).
 * `null` if there is none.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveMyActiveTripUseCase @Inject constructor(
    private val tripRepository: TripRepository,
    private val reservationRepository: ReservationRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<MyTrip?> =
        reservationRepository.observeMyReservations().flatMapLatest { reservations ->
            // A cancelled trip cancels its reservations too, so the cancelled ones can show it
            val candidates = reservations.filter { it.isActive || it.status == ReservationStatus.CANCELADA }
            if (candidates.isEmpty()) {
                flowOf(null)
            } else {
                val withTrips = combine(candidates.map { reservation -> tripRepository.observeTrip(reservation.tripId) }) { trips ->
                    candidates.zip(trips.toList()).mapNotNull { (reservation, trip) -> trip?.let { it to reservation } }
                }
                combine(
                    withTrips,
                    reservationRepository.observePendingReservationIds(),
                    reservationRepository.observeRejections(),
                ) { pairs, pending, rejections ->
                    pickNext(pairs)?.let { (trip, reservation) ->
                        MyTrip(trip, reservation, reservation.id in pending, rejections[reservation.id])
                    }
                }
            }
        }.distinctUntilChanged()

    private fun pickNext(pairs: List<Pair<Trip, Reservation>>): Pair<Trip, Reservation>? {
        val byDeparture = compareBy<Pair<Trip, Reservation>>({ it.first.schedule.departureDate }, { it.first.schedule.departureTime })
        val upcoming = pairs.filter { (trip, reservation) -> reservation.isActive && trip.status !in CLOSED }
        if (upcoming.isNotEmpty()) return upcoming.minWith(byDeparture)
        val now = LocalDateTime.now(clock)
        return pairs
            .filter { (trip, _) -> trip.status == TripStatus.CANCELADO && !trip.schedule.hasDepartedAt(now) }
            .minWithOrNull(byDeparture)
    }

    private companion object {
        val CLOSED = setOf(TripStatus.FINALIZADO, TripStatus.CANCELADO)
    }
}
