package com.example.uniride.domain.repository

import com.example.uniride.domain.model.AppMode
import com.example.uniride.domain.model.TripFilters
import kotlinx.coroutines.flow.Flow

/** Preferences of the device in DataStore (not the token: that goes in `SessionTokenStore`). */
interface UserPreferencesRepository {

    /** Last filters of the feed, restored when the app opens again (RF03). */
    fun observeLastFilters(): Flow<TripFilters>

    suspend fun saveLastFilters(filters: TripFilters)

    /** Mode chosen in the selector of the bottom bar. */
    fun observeAppMode(): Flow<AppMode>

    suspend fun setAppMode(mode: AppMode)
}
