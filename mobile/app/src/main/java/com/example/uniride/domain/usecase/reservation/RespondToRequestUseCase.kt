package com.example.uniride.domain.usecase.reservation

import com.example.uniride.core.result.AppResult
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
 * The driver accepts or rejects a request (RF04, C4). Only a pending request of a trip that did
 * not start yet; accepting also needs a free seat. Works offline: the server confirms it later.
 */
class RespondToRequestUseCase @Inject constructor(
    private val tripRepository: TripRepository,
    private val reservationRepository: ReservationRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(tripId: TripId, reservationId: ReservationId, accept: Boolean): ReservationChangeResult {
        val reservation = reservationRepository.observeTripReservations(tripId).first()
            .firstOrNull { it.id == reservationId }
            ?: return ReservationChangeResult.Failed(DataError.NOT_FOUND)
        val trip = tripRepository.observeTrip(tripId).first()

        val block = when {
            trip != null && trip.schedule.hasDepartedAt(LocalDateTime.now(clock)) -> ReservationChangeBlock.TRIP_ALREADY_DEPARTED
            !reservation.canBeAnswered -> ReservationChangeBlock.NOT_PENDING
            accept && trip != null && trip.availableSeats == 0 -> ReservationChangeBlock.NO_SEATS
            else -> null
        }
        if (block != null) return ReservationChangeResult.NotAllowed(block)

        val result = if (accept) {
            reservationRepository.accept(tripId, reservationId)
        } else {
            reservationRepository.reject(tripId, reservationId)
        }
        return result.toChangeResult()
    }
}

internal fun AppResult<Unit>.toChangeResult(): ReservationChangeResult = when (this) {
    is AppResult.Success -> ReservationChangeResult.Done
    is AppResult.Failure -> ReservationChangeResult.Failed(error)
}
