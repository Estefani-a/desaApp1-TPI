package com.example.uniride.domain.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.TripDraft
import com.example.uniride.domain.model.TripFilters
import com.example.uniride.domain.model.TripId
import kotlinx.coroutines.flow.Flow

/**
 * Trips (RF02 and RF03). The `observe` functions emit the last known data, also offline, and
 * again whenever it changes. The `refresh` functions bring news from the server.
 */
interface TripRepository {

    /** Trips that still take passengers and match [filters]. */
    fun observeTrips(filters: TripFilters): Flow<List<Trip>>

    /** `null` if the trip is not known. */
    fun observeTrip(tripId: TripId): Flow<Trip?>

    /** Trips the user offers or travels on. */
    fun observeMyTrips(): Flow<List<Trip>>

    suspend fun refreshTrips(): AppResult<Unit>

    suspend fun refreshTrip(tripId: TripId): AppResult<Unit>

    suspend fun refreshMyTrips(): AppResult<Unit>

    /**
     * Offers a new trip. Works offline: the trip is visible right away as pending and is sent
     * when there is connection. Returns its id.
     */
    suspend fun publish(draft: TripDraft): AppResult<TripId>

    /** Works offline, like [publish]. */
    suspend fun finish(tripId: TripId): AppResult<Unit>

    /** Works offline, like [publish]. */
    suspend fun cancel(tripId: TripId): AppResult<Unit>
}
