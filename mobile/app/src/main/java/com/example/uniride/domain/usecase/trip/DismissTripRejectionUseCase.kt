package com.example.uniride.domain.usecase.trip

import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.repository.TripRepository
import javax.inject.Inject

/** The driver read why a trip was rejected: the notice and the trip are removed. */
class DismissTripRejectionUseCase @Inject constructor(
    private val tripRepository: TripRepository,
) {
    suspend operator fun invoke(tripId: TripId) = tripRepository.dismissRejection(tripId)
}
