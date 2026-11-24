package com.example.uniride.domain.repository

import com.example.uniride.domain.model.TripFilters
import kotlinx.coroutines.flow.Flow

/** Last filters used in the feed, restored when the app opens again (RF03). */
interface FeedFiltersRepository {

    fun observeLastFilters(): Flow<TripFilters>

    suspend fun saveLastFilters(filters: TripFilters)
}
