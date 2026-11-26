package com.example.uniride.domain.usecase.reservation

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.ReservationId
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.repository.ProfileRepository
import com.example.uniride.domain.repository.ReservationRepository
import com.example.uniride.domain.repository.TripRepository
import com.example.uniride.domain.validation.SeatRequestDecision
import com.example.uniride.domain.validation.SeatRequestRule
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * What happened when the passenger asked for a seat. Not an `AppResult`: a request the rules
 * do not allow is not a [DataError], and the screen shows a different message for each reason.
 */
sealed interface RequestSeatResult {
    /** Saved on the phone as `PENDIENTE`; it is sent to the server when there is connection (B5). */
    data class Requested(val reservationId: ReservationId) : RequestSeatResult

    /** The rules of [SeatRequestRule] do not allow it; nothing was saved. */
    data class NotAllowed(val reason: SeatRequestDecision) : RequestSeatResult

    data class Failed(val error: DataError) : RequestSeatResult
}

/**
 * Asks for a seat on a trip (RF04, B3). It checks [SeatRequestRule] with the data on the phone
 * (the same rules as the server) and only then asks the repository. Works offline.
 */
class RequestSeatUseCase @Inject constructor(
    private val tripRepository: TripRepository,
    private val reservationRepository: ReservationRepository,
    private val profileRepository: ProfileRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(tripId: TripId): RequestSeatResult {
        val trip = tripRepository.observeTrip(tripId).first()
            ?: return RequestSeatResult.Failed(DataError.NOT_FOUND)
        val user = profileRepository.observeCurrentUser().first()
            ?: return RequestSeatResult.Failed(DataError.UNAUTHORIZED)
        val mine = reservationRepository.observeMyReservation(tripId).first()

        val decision = SeatRequestRule.evaluate(trip, user.id, mine, LocalDateTime.now(clock))
        if (decision != SeatRequestDecision.ALLOWED) return RequestSeatResult.NotAllowed(decision)

        return when (val result = reservationRepository.requestSeat(tripId)) {
            is AppResult.Success -> RequestSeatResult.Requested(result.data)
            is AppResult.Failure -> RequestSeatResult.Failed(result.error)
        }
    }
}
