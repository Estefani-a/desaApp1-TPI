package com.example.uniride.domain.usecase.trip

import com.example.uniride.core.result.AppResult
import com.example.uniride.domain.repository.TripRepository
import javax.inject.Inject

/** Brings the trips of the user from the server (C1: on open and when the connection returns). */
class RefreshMyTripsUseCase @Inject constructor(
    private val tripRepository: TripRepository,
) {
    suspend operator fun invoke(): AppResult<Unit> = tripRepository.refreshMyTrips()
}
