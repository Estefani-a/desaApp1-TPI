package com.example.uniride.domain.usecase.trip

import com.example.uniride.domain.model.TripFilters
import com.example.uniride.domain.repository.FeedFiltersRepository
import javax.inject.Inject

/** Keeps the filters of the feed for the next time the app opens (RF03). */
class SaveFeedFiltersUseCase @Inject constructor(
    private val feedFiltersRepository: FeedFiltersRepository,
) {
    suspend operator fun invoke(filters: TripFilters) = feedFiltersRepository.saveLastFilters(filters)
}
