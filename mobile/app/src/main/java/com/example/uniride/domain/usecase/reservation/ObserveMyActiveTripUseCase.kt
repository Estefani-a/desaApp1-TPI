package com.example.uniride.domain.usecase.reservation

import com.example.uniride.domain.model.Reservation
import com.example.uniride.domain.model.ReservationRejection
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
import javax.inject.Inject

/** The trip the passenger is going on (B6 "Mi viaje" and the card of the start). */
data class MyTrip(
    val trip: Trip,
    val reservation: Reservation,
    /** The reservation changed on the phone and was not sent yet. */
    val isPendingSync: Boolean,
    /** Why the server did not accept the last change, if it did not (for example, a late cancel). */
    val rejection: ReservationRejection?,
)

/**
 * The passenger's next trip (RF04, B6): among the trips where they hold a pending or accepted
 * reservation and that are not finished or cancelled, the one that leaves first. It reads only
 * the phone, so a confirmed trip is readable offline (preentrega 12.1). `null` if there is none.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveMyActiveTripUseCase @Inject constructor(
    private val tripRepository: TripRepository,
    private val reservationRepository: ReservationRepository,
) {
    operator fun invoke(): Flow<MyTrip?> =
        reservationRepository.observeMyReservations().flatMapLatest { reservations ->
            val active = reservations.filter { it.isActive }
            if (active.isEmpty()) {
                flowOf(null)
            } else {
                val withTrips = combine(active.map { reservation -> tripRepository.observeTrip(reservation.tripId) }) { trips ->
                    active.zip(trips.toList()).mapNotNull { (reservation, trip) -> trip?.let { it to reservation } }
                }
                combine(
                    withTrips,
                    reservationRepository.observePendingReservationIds(),
                    reservationRepository.observeRejections(),
                ) { pairs, pending, rejections ->
                    pairs
                        .filter { (trip, _) -> trip.status != TripStatus.FINALIZADO && trip.status != TripStatus.CANCELADO }
                        .minWithOrNull(compareBy({ it.first.schedule.departureDate }, { it.first.schedule.departureTime }))
                        ?.let { (trip, reservation) ->
                            MyTrip(trip, reservation, reservation.id in pending, rejections[reservation.id])
                        }
                }
            }
        }.distinctUntilChanged()
}
