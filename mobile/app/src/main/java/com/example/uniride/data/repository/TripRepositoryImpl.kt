package com.example.uniride.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.data.local.dao.ReservationDao
import com.example.uniride.data.local.dao.TripDao
import com.example.uniride.data.local.dao.UserDao
import com.example.uniride.data.local.entity.TripEntity
import com.example.uniride.data.mapper.toDomain
import com.example.uniride.data.mapper.toEntity
import com.example.uniride.data.remote.datasource.TripRemoteDataSource
import com.example.uniride.data.remote.dto.TripDto
import com.example.uniride.data.remote.safeApiCall
import com.example.uniride.data.sync.SyncScheduler
import com.example.uniride.domain.model.Participant
import com.example.uniride.domain.model.ReservationStatus
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.TripDraft
import com.example.uniride.domain.model.TripFilters
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.model.TripStatus
import com.example.uniride.domain.model.TripVehicle
import com.example.uniride.domain.repository.TripRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [TripRepository] over Room, the single source of truth: the screens observe the DAO flows
 * and never the answer of the API. The `refresh` functions only bring news into Room.
 *
 * - Refresh is full the first time and partial (`updatedSince`, with a minute of margin) after
 *   that. The retention rule of 11.2 is applied by `TripDao.replaceCatalog`.
 * - [publish] works offline: it saves the trip with `isSync = false` and asks the scheduler to
 *   send it (`TripSyncHandler` does).
 * - Every refresh also saves the user's reservation that comes inside each trip (`myReservation`,
 *   docs/API.md §3.3), so the reservations screens read it from Room too.
 * - [finish] and [cancel] are **online only**: they are not queued, because their result (a 409
 *   if the trip already changed) decides what the user sees. Without connection they return the
 *   [DataError] and nothing changes. When they work, the trip the server returns is saved.
 *
 * Everything that may fail goes through `safeApiCall`, so nothing here throws.
 */
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class TripRepositoryImpl @Inject constructor(
    private val tripDao: TripDao,
    private val reservationDao: ReservationDao,
    private val userDao: UserDao,
    private val remote: TripRemoteDataSource,
    private val rejections: TripRejectionStore,
    private val syncScheduler: SyncScheduler,
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock,
) : TripRepository {

    override fun observeTrips(filters: TripFilters): Flow<List<Trip>> =
        tripDao.observeListedTrips()
            .map { rows -> rows.map(TripEntity::toDomain).filter(filters::matches) }
            .distinctUntilChanged()

    override fun observeTrip(tripId: TripId): Flow<Trip?> =
        tripDao.observeTrip(tripId.value).map { it?.toDomain() }.distinctUntilChanged()

    override fun observeMyTrips(): Flow<List<Trip>> =
        userDao.observeCurrentUser().flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                tripDao.observeMyTrips(user.id).map { rows -> rows.map(TripEntity::toDomain) }
            }
        }.distinctUntilChanged()

    override fun observeMyPublishedTrips(): Flow<List<Trip>> =
        userDao.observeCurrentUser().flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                tripDao.observeTripsByDriver(user.id).map { rows -> rows.map(TripEntity::toDomain) }
            }
        }.distinctUntilChanged()

    override fun observePendingTripIds(): Flow<Set<TripId>> =
        tripDao.observePendingTripIds().map { ids -> ids.mapTo(HashSet(), ::TripId) }.distinctUntilChanged()

    override fun observeLastRefreshedAt(): Flow<Instant?> =
        dataStore.data.map { prefs -> prefs[LAST_REFRESHED_AT]?.let(Instant::ofEpochMilli) }.distinctUntilChanged()

    override fun observeRejectedTrips(): Flow<Map<TripId, DataError>> = rejections.observe()

    override suspend fun dismissRejection(tripId: TripId) {
        rejections.remove(tripId)
        // The server never accepted this trip, so there is nothing to keep
        safeApiCall { tripDao.deleteByIds(listOf(tripId.value)) }
    }

    override suspend fun clearLocalData(): AppResult<Unit> = safeApiCall {
        tripDao.deleteAll()
        rejections.clear()
        dataStore.edit { it.remove(LAST_REFRESHED_AT) }
    }

    override suspend fun refreshTrips(): AppResult<Unit> {
        val since = observeLastRefreshedAt().first()
        val startedAt = clock.instant()
        // A minute of margin, so a trip changed while the previous refresh was running is not lost
        val updatedSince = since?.minusSeconds(MARGIN_SECONDS)?.let { OffsetDateTime.ofInstant(it, clock.zone).format(ISO) }
        val result = safeApiCall {
            val dtos = remote.getTrips(updatedSince = updatedSince)
            tripDao.replaceCatalog(
                fromServer = dtos.map { it.toDomain().toEntity(isSync = true) },
                full = since == null,
                userId = userDao.getCurrentUser()?.id,
                keepTripIds = rejections.ids() + dtos.filter { it.hasActiveReservation() }.map { it.tripId },
            )
            saveMyReservations(dtos)
        }
        if (result is AppResult.Success) {
            dataStore.edit { it[LAST_REFRESHED_AT] = startedAt.toEpochMilli() }
        }
        return result
    }

    override suspend fun refreshTrip(tripId: TripId): AppResult<Unit> = safeApiCall {
        val dto = remote.getTripDetail(tripId.value)
        tripDao.upsert(dto.toDomain().toEntity(isSync = true))
        saveMyReservations(listOf(dto))
    }

    override suspend fun refreshMyTrips(): AppResult<Unit> = safeApiCall {
        val dtos = remote.getMyTrips()
        tripDao.upsertAll(dtos.map { it.toDomain().toEntity(isSync = true) })
        saveMyReservations(dtos)
    }

    /** A reservation changed on this device and not sent yet is kept (`upsertFromServer`). */
    private suspend fun saveMyReservations(dtos: List<TripDto>) {
        reservationDao.upsertFromServer(dtos.mapNotNull { it.myReservation?.toDomain()?.toEntity(isSync = true) })
    }

    override suspend fun publish(draft: TripDraft): AppResult<TripId> {
        val userEntity = safeApiCall { userDao.getCurrentUser() }.let {
            when (it) {
                is AppResult.Failure -> return it
                is AppResult.Success -> it.data ?: return AppResult.Failure(DataError.UNAUTHORIZED)
            }
        }
        val user = userEntity.toDomain()
        val vehicle = user.profile.vehicle ?: return AppResult.Failure(DataError.VALIDATION)
        val trip = Trip(
            id = TripId(UUID.randomUUID().toString()),
            driver = Participant(user.id, user.profile.fullName, user.rating?.average, user.profile.phone),
            campus = draft.campus,
            route = draft.route,
            schedule = draft.schedule,
            totalSeats = draft.totalSeats,
            availableSeats = draft.totalSeats,
            contributionPerSeat = draft.contributionPerSeat,
            vehicle = TripVehicle(vehicle.model, vehicle.color, vehicle.plate),
            status = TripStatus.ABIERTO,
            updatedAt = clock.instant(),
        )
        return safeApiCall {
            tripDao.upsert(trip.toEntity(isSync = false))
            syncScheduler.schedule()
            trip.id
        }
    }

    override suspend fun finish(tripId: TripId): AppResult<Unit> =
        safeApiCall { tripDao.upsert(remote.finishTrip(tripId.value).toDomain().toEntity(isSync = true)) }

    override suspend fun cancel(tripId: TripId): AppResult<Unit> =
        safeApiCall { tripDao.upsert(remote.cancelTrip(tripId.value).toDomain().toEntity(isSync = true)) }

    private fun TripDto.hasActiveReservation(): Boolean =
        myReservation?.status?.let { ReservationStatus.valueOf(it).isActive } == true

    private companion object {
        const val MARGIN_SECONDS = 60L
        val ISO: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val LAST_REFRESHED_AT = longPreferencesKey("trips_last_refreshed_at")
    }
}
