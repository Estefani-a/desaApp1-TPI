package com.example.uniride.domain.usecase.reservation

import com.example.uniride.core.result.AppResult
import com.example.uniride.domain.model.ReservationId
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.repository.ReservationRepository
import com.example.uniride.domain.repository.TripRepository
import javax.inject.Inject

/** Brings the latest version of a trip and of the user's reservation on it (B3, B6). */
class RefreshTripDetailUseCase @Inject constructor(
    private val tripRepository: TripRepository,
) {
    suspend operator fun invoke(tripId: TripId): AppResult<Unit> = tripRepository.refreshTrip(tripId)
}

/** Brings the trip and its requests for the driver panel (C4). The first error wins. */
class RefreshDriverPanelUseCase @Inject constructor(
    private val tripRepository: TripRepository,
    private val reservationRepository: ReservationRepository,
) {
    suspend operator fun invoke(tripId: TripId): AppResult<Unit> {
        val trip = tripRepository.refreshTrip(tripId)
        val requests = reservationRepository.refreshTripReservations(tripId)
        return if (trip is AppResult.Failure) trip else requests
    }
}

/** The user saw why the server did not accept a change: the message is not shown again. */
class DismissReservationRejectionUseCase @Inject constructor(
    private val reservationRepository: ReservationRepository,
) {
    suspend operator fun invoke(reservationId: ReservationId) = reservationRepository.dismissRejection(reservationId)
}
