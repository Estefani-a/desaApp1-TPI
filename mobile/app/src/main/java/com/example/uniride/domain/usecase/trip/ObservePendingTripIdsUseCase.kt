package com.example.uniride.domain.usecase.trip

import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Ids of the trips not confirmed by the server yet, to show "Pendiente de sincronizar". */
class ObservePendingTripIdsUseCase @Inject constructor(
    private val tripRepository: TripRepository,
) {
    operator fun invoke(): Flow<Set<TripId>> = tripRepository.observePendingTripIds()
}
