package com.example.uniride.domain.usecase.trip

import com.example.uniride.domain.model.RejectedTrip
import com.example.uniride.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** Trips the server rejected on sync, with the reason and the trip as it was saved. */
class ObserveRejectedTripsUseCase @Inject constructor(
    private val tripRepository: TripRepository,
) {
    operator fun invoke(): Flow<List<RejectedTrip>> =
        combine(
            tripRepository.observeRejectedTrips(),
            tripRepository.observeMyPublishedTrips(),
        ) { rejections, trips ->
            val byId = trips.associateBy { it.id }
            rejections.map { (id, error) -> RejectedTrip(id, byId[id], error) }
        }
}
