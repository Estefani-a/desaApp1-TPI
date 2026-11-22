package com.example.uniride.domain.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.TripDraft
import com.example.uniride.domain.model.TripFilters
import kotlinx.coroutines.flow.Flow

/**
 * Trips (RF02 and RF03, docs/API.md §5). The `observe*` functions read Room; the API is
 * only used by the `refresh*` functions and by the sync worker (CONVENCIONES §6.1).
 */
interface TripRepository {

    /** Open and full trips of the catalog that match [filters] (RF03). */
    fun observeTrips(filters: TripFilters): Flow<List<Trip>>

    /** `null` if the trip is not in Room. */
    fun observeTrip(tripId: String): Flow<Trip?>

    /** Trips the user published or has a reservation on (`GET /users/me/trips`). */
    fun observeMyTrips(): Flow<List<Trip>>

    /** Updates the catalog in Room (with `updatedSince` after the first time). */
    suspend fun refreshTrips(): AppResult<Unit>

    suspend fun refreshTrip(tripId: String): AppResult<Unit>

    suspend fun refreshMyTrips(): AppResult<Unit>

    /**
     * Saves the trip in Room with `isSync = false` and schedules the sync (CONVENCIONES §6.2).
     * Works offline. Returns the `tripId` generated in the app.
     */
    suspend fun publishTrip(draft: TripDraft): AppResult<String>

    /** Offline write, sent by the sync worker. */
    suspend fun finishTrip(tripId: String): AppResult<Unit>

    /** Offline write, sent by the sync worker. */
    suspend fun cancelTrip(tripId: String): AppResult<Unit>
}
