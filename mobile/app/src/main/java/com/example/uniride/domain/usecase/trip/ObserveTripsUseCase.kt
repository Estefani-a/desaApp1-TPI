package com.example.uniride.domain.usecase.trip

import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.TripFilters
import com.example.uniride.domain.model.TripStatus
import com.example.uniride.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Trips of the feed (RF03) that match [TripFilters], from the data saved on the phone, so it
 * also works offline. Same rule as `GET /trips` (docs/API.md §5.1): only `ABIERTO` and
 * `COMPLETO` trips that did not leave yet, sorted by departure. A trip that left while the phone
 * was offline disappears even before the next refresh.
 */
class ObserveTripsUseCase @Inject constructor(
    private val tripRepository: TripRepository,
    private val clock: Clock,
) {
    operator fun invoke(filters: TripFilters): Flow<List<Trip>> =
        tripRepository.observeTrips(filters).map { trips ->
            val now = LocalDateTime.now(clock)
            trips
                .filter { it.status in LISTED && !it.schedule.hasDepartedAt(now) && filters.matches(it) }
                .sortedWith(compareBy({ it.schedule.departureDate }, { it.schedule.departureTime }))
        }

    private companion object {
        val LISTED = setOf(TripStatus.ABIERTO, TripStatus.COMPLETO)
    }
}
