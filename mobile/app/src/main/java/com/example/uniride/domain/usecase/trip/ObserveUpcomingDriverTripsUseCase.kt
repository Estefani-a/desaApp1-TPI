package com.example.uniride.domain.usecase.trip

import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.TripStatus
import com.example.uniride.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Trips the user drives that are still ahead (RF02, C1): `ABIERTO` or `COMPLETO` and not departed,
 * soonest first. Read from the phone, so it works offline and includes the ones not synced yet.
 */
class ObserveUpcomingDriverTripsUseCase @Inject constructor(
    private val tripRepository: TripRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<List<Trip>> =
        tripRepository.observeMyPublishedTrips().map { trips ->
            val now = LocalDateTime.now(clock)
            trips
                .filter { it.status in UPCOMING && !it.schedule.hasDepartedAt(now) }
                .sortedWith(compareBy({ it.schedule.departureDate }, { it.schedule.departureTime }))
        }

    private companion object {
        val UPCOMING = setOf(TripStatus.ABIERTO, TripStatus.COMPLETO)
    }
}
