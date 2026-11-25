package com.example.uniride.domain.usecase.trip

import com.example.uniride.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject

/** When the feed was last refreshed, for "Actualizado HH:MM" (RF03). `null` on the first start. */
class ObserveLastTripsRefreshUseCase @Inject constructor(
    private val tripRepository: TripRepository,
) {
    operator fun invoke(): Flow<Instant?> = tripRepository.observeLastRefreshedAt()
}
