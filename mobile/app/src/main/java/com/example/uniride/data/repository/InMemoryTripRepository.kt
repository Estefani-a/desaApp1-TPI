package com.example.uniride.data.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.data.local.CatalogRetention
import com.example.uniride.data.mapper.toDomain
import com.example.uniride.data.remote.datasource.TripRemoteDataSource
import com.example.uniride.data.remote.dto.TripDto
import com.example.uniride.data.remote.safeApiCall
import com.example.uniride.domain.model.ReservationStatus
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.TripDraft
import com.example.uniride.domain.model.TripFilters
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.model.TripStatus
import com.example.uniride.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * **Provisional** [TripRepository] for RF03 while Room (E1) is not merged: the catalog lives in
 * memory, so it is lost when the process dies. It already follows the rules of the real one —
 * the screens only observe flows, refresh is full the first time and partial (`updatedSince`)
 * after that, and the retention rule of 11.2 is [CatalogRetention] — so replacing it with the
 * Room implementation only changes the binding in `TripModule`.
 *
 * Publishing, finishing and cancelling belong to RF02 (Room + SyncWorker): here they fail.
 */
@Singleton
class InMemoryTripRepository @Inject constructor(
    private val remote: TripRemoteDataSource,
    private val clock: Clock,
) : TripRepository {

    /** A trip of the catalog and whether the user holds an active reservation on it. */
    private data class Entry(val trip: Trip, val hasActiveReservation: Boolean) {
        val id: TripId get() = trip.id

        val isListed: Boolean get() = trip.status == TripStatus.ABIERTO || trip.status == TripStatus.COMPLETO

        /** 11.2: a trip with an active reservation stays even if the server stops listing it. */
        val mustKeep: Boolean get() = hasActiveReservation
    }

    private val catalog = MutableStateFlow<Map<TripId, Entry>>(emptyMap())
    private val lastRefreshedAt = MutableStateFlow<Instant?>(null)

    override fun observeTrips(filters: TripFilters): Flow<List<Trip>> =
        catalog.map { entries -> entries.values.map { it.trip }.filter(filters::matches) }
            .distinctUntilChanged()

    override fun observeTrip(tripId: TripId): Flow<Trip?> =
        catalog.map { it[tripId]?.trip }.distinctUntilChanged()

    override fun observeMyTrips(): Flow<List<Trip>> =
        catalog.map { entries -> entries.values.filter { it.hasActiveReservation }.map { it.trip } }
            .distinctUntilChanged()

    override fun observePendingTripIds(): Flow<Set<TripId>> = flowOf(emptySet())

    override fun observeLastRefreshedAt(): Flow<Instant?> = lastRefreshedAt.asStateFlow()

    override suspend fun refreshTrips(): AppResult<Unit> {
        val since = lastRefreshedAt.value
        val startedAt = clock.instant()
        // A minute of margin, so a trip changed while the previous refresh was running is not lost
        val updatedSince = since?.minusSeconds(60)?.let { OffsetDateTime.ofInstant(it, clock.zone).format(ISO) }
        val result = safeApiCall { remote.getTrips(updatedSince = updatedSince).map { it.toEntry() } }
        if (result is AppResult.Success) {
            catalog.update { current ->
                val merged = if (since == null) {
                    CatalogRetention.replace(current.values, result.data, Entry::id, Entry::mustKeep)
                } else {
                    CatalogRetention.merge(current.values, result.data, Entry::id, Entry::isListed, Entry::mustKeep)
                }
                merged.associateBy { it.id }
            }
            lastRefreshedAt.value = startedAt
        }
        return result.discardValue()
    }

    override suspend fun refreshTrip(tripId: TripId): AppResult<Unit> {
        val result = safeApiCall { remote.getTripDetail(tripId.value).toEntry() }
        if (result is AppResult.Success) catalog.update { it + (result.data.id to result.data) }
        return result.discardValue()
    }

    override suspend fun refreshMyTrips(): AppResult<Unit> {
        val result = safeApiCall { remote.getMyTrips().map { it.toEntry() } }
        if (result is AppResult.Success) catalog.update { current -> current + result.data.associateBy { it.id } }
        return result.discardValue()
    }

    override suspend fun publish(draft: TripDraft): AppResult<TripId> = AppResult.Failure(DataError.UNKNOWN)

    override suspend fun finish(tripId: TripId): AppResult<Unit> = AppResult.Failure(DataError.UNKNOWN)

    override suspend fun cancel(tripId: TripId): AppResult<Unit> = AppResult.Failure(DataError.UNKNOWN)

    private fun TripDto.toEntry() = Entry(
        trip = toDomain(),
        hasActiveReservation = myReservation?.status?.let { ReservationStatus.valueOf(it).isActive } == true,
    )

    private fun <T> AppResult<T>.discardValue(): AppResult<Unit> = when (this) {
        is AppResult.Success -> AppResult.Success(Unit)
        is AppResult.Failure -> this
    }

    private companion object {
        val ISO: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    }
}
