package com.example.uniride.domain.usecase.reservation

import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.ReservationId
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.repository.ReservationRepository
import com.example.uniride.domain.repository.TripRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * The passenger cancels their reservation (RF04, B6). Not once the trip started: the passenger
 * keeps the seat (docs/API.md §6.3). If it was done offline and the trip started before it was
 * sent, the server rejects it with `TRIP_ALREADY_DEPARTED` and the reservation stays accepted.
 */
class CancelReservationUseCase @Inject constructor(
    private val tripRepository: TripRepository,
    private val reservationRepository: ReservationRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(tripId: TripId, reservationId: ReservationId): ReservationChangeResult {
        val reservation = reservationRepository.observeMyReservation(tripId).first()
            ?.takeIf { it.id == reservationId }
            ?: return ReservationChangeResult.Failed(DataError.NOT_FOUND)
        val trip = tripRepository.observeTrip(tripId).first()

        val block = when {
            trip != null && trip.schedule.hasDepartedAt(LocalDateTime.now(clock)) -> ReservationChangeBlock.TRIP_ALREADY_DEPARTED
            !reservation.canBeCancelled -> ReservationChangeBlock.NOT_ACTIVE
            else -> null
        }
        if (block != null) return ReservationChangeResult.NotAllowed(block)

        return reservationRepository.cancel(tripId, reservationId).toChangeResult()
    }
}
