package com.example.uniride.domain.usecase.trip

import com.example.uniride.domain.model.TripFilters
import com.example.uniride.domain.repository.FeedFiltersRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Last filters used in the feed, restored when the app opens again (RF03). A date that already
 * passed is dropped: filtering by yesterday would always show an empty feed.
 */
class ObserveFeedFiltersUseCase @Inject constructor(
    private val feedFiltersRepository: FeedFiltersRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<TripFilters> =
        feedFiltersRepository.observeLastFilters().map { filters ->
            val date = filters.date
            if (date != null && date.isBefore(LocalDate.now(clock))) filters.copy(date = null) else filters
        }
}
