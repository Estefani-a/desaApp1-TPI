package com.example.uniride.domain.usecase.trip

import com.example.uniride.core.result.AppResult
import com.example.uniride.domain.repository.TripRepository
import javax.inject.Inject

/**
 * Brings the news of the feed from the server (RF03: on open, on pull to refresh and when the
 * connection comes back). The screen keeps reading from [ObserveTripsUseCase].
 */
class RefreshTripsUseCase @Inject constructor(
    private val tripRepository: TripRepository,
) {
    suspend operator fun invoke(): AppResult<Unit> = tripRepository.refreshTrips()
}
