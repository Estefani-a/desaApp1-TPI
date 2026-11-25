package com.example.uniride.domain.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.TripDraft
import com.example.uniride.domain.model.TripFilters
import com.example.uniride.domain.model.TripId
import kotlinx.coroutines.flow.Flow
import java.time.Instant

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

    /**
     * Trips created or changed on this device that the server did not confirm yet. The screens
     * combine it with the trips to show "Pendiente de sincronizar".
     */
    fun observePendingTripIds(): Flow<Set<TripId>>

    /**
     * When the trips of the feed were last brought from the server, or `null` if never (first
     * start). It survives restarts, so the feed can say "Actualizado HH:MM" offline (RF03).
     */
    fun observeLastRefreshedAt(): Flow<Instant?>

    suspend fun refreshTrips(): AppResult<Unit>

    suspend fun refreshTrip(tripId: TripId): AppResult<Unit>

    suspend fun refreshMyTrips(): AppResult<Unit>

    /**
     * Trips the server rejected when they were synced, with the reason. Such a trip is marked
     * `CANCELADO` locally; the screens show the reason until it is dismissed.
     */
    fun observeRejectedTrips(): Flow<Map<TripId, DataError>>

    /** The user saw why [tripId] was rejected: forgets the reason and removes the trip. */
    suspend fun dismissRejection(tripId: TripId)

    /**
     * Forgets everything this repository keeps on the device: the trips, when they were last
     * refreshed and the reasons of rejected trips. The logout calls it, so the next user does
     * not get a partial refresh on top of an empty catalog. Pending trips are lost too.
     */
    suspend fun clearLocalData(): AppResult<Unit>

    /**
     * Offers a new trip. Works offline: the trip is visible right away, appears in
     * [observePendingTripIds] and is sent when there is connection. Returns its id.
     *
     * The driver, the vehicle and the phone come from the current user stored in `UserDao`.
     * Until the login (RF01) saves it there, this returns [DataError.UNAUTHORIZED]. A user
     * without a vehicle gets [DataError.VALIDATION], as the server would answer.
     */
    suspend fun publish(draft: TripDraft): AppResult<TripId>

    /**
     * Online only: it is not queued. Without connection it returns the [DataError]
     * (`NO_CONNECTION`, `TIMEOUT`…) and nothing changes.
     */
    suspend fun finish(tripId: TripId): AppResult<Unit>

    /** Online only, like [finish]: without connection it returns the [DataError]. */
    suspend fun cancel(tripId: TripId): AppResult<Unit>
}
