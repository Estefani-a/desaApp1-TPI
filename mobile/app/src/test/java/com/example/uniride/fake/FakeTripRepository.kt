package com.example.uniride.fake

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.TripDraft
import com.example.uniride.domain.model.TripFilters
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * In-memory [TripRepository] for use cases and ViewModels. [trips] plays the part of Room;
 * [refreshTrips] answers [refreshResult] and, when it succeeds, puts [tripsAfterRefresh] in the
 * catalog and stamps [lastRefreshedAt].
 */
class FakeTripRepository(
    initialTrips: List<Trip> = emptyList(),
    initialLastRefreshedAt: Instant? = null,
) : TripRepository {

    val trips = MutableStateFlow(initialTrips)
    val lastRefreshedAt = MutableStateFlow(initialLastRefreshedAt)

    var refreshResult: AppResult<Unit> = AppResult.Success(Unit)
    var tripsAfterRefresh: List<Trip>? = null
    var refreshedAt: Instant = Instant.parse("2026-11-20T21:00:00Z")
    var refreshCalls = 0
        private set

    override fun observeTrips(filters: TripFilters): Flow<List<Trip>> =
        trips.map { list -> list.filter(filters::matches) }

    override fun observeTrip(tripId: TripId): Flow<Trip?> = trips.map { list -> list.firstOrNull { it.id == tripId } }

    override fun observeMyTrips(): Flow<List<Trip>> = trips

    override fun observePendingTripIds(): Flow<Set<TripId>> = MutableStateFlow(emptySet())

    override fun observeLastRefreshedAt(): Flow<Instant?> = lastRefreshedAt

    override suspend fun refreshTrips(): AppResult<Unit> {
        refreshCalls++
        if (refreshResult is AppResult.Success) {
            tripsAfterRefresh?.let { trips.value = it }
            lastRefreshedAt.value = refreshedAt
        }
        return refreshResult
    }

    override suspend fun refreshTrip(tripId: TripId): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun refreshMyTrips(): AppResult<Unit> = AppResult.Success(Unit)

    override fun observeRejectedTrips(): Flow<Map<TripId, DataError>> = MutableStateFlow(emptyMap())

    override suspend fun dismissRejection(tripId: TripId) = Unit

    override suspend fun publish(draft: TripDraft): AppResult<TripId> = error("No se usa en estos tests")

    override suspend fun finish(tripId: TripId): AppResult<Unit> = error("No se usa en estos tests")

    override suspend fun cancel(tripId: TripId): AppResult<Unit> = error("No se usa en estos tests")
}
