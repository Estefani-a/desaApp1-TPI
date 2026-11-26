package com.example.uniride.data.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.data.local.dao.ReservationDao
import com.example.uniride.data.local.dao.UserDao
import com.example.uniride.data.local.entity.ReservationEntity
import com.example.uniride.data.mapper.toDomain
import com.example.uniride.data.mapper.toEntity
import com.example.uniride.data.remote.datasource.ReservationRemoteDataSource
import com.example.uniride.data.remote.safeApiCall
import com.example.uniride.data.sync.SyncScheduler
import com.example.uniride.domain.model.Reservation
import com.example.uniride.domain.model.ReservationId
import com.example.uniride.domain.model.ReservationRejection
import com.example.uniride.domain.model.ReservationStatus
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.repository.ReservationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ReservationRepository] over Room, the single source of truth (CONVENCIONES §6.1).
 *
 * Every write follows §6.2: the change is saved with `isSync = false`, the scheduler is asked to
 * send it and the screen shows it right away. The status of the row says what to send
 * ([com.example.uniride.data.sync.ReservationSyncHandler] does it). The rules about which change
 * is allowed (own trip, departed, status) are checked by the use cases before calling here.
 *
 * Nothing here throws: everything that may fail goes through `safeApiCall`.
 */
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class ReservationRepositoryImpl @Inject constructor(
    private val reservationDao: ReservationDao,
    private val userDao: UserDao,
    private val remote: ReservationRemoteDataSource,
    private val rejections: ReservationRejectionStore,
    private val syncScheduler: SyncScheduler,
    private val clock: Clock,
) : ReservationRepository {

    override fun observeTripReservations(tripId: TripId): Flow<List<Reservation>> =
        reservationDao.observeByTrip(tripId.value)
            .map { rows -> rows.map(ReservationEntity::toDomain) }
            .distinctUntilChanged()

    override fun observeMyReservation(tripId: TripId): Flow<Reservation?> =
        currentUserId().flatMapLatest { userId ->
            if (userId == null) {
                flowOf(null)
            } else {
                reservationDao.observeByTripAndPassenger(tripId.value, userId)
                    .map { rows -> Reservation.currentOf(rows.map(ReservationEntity::toDomain)) }
            }
        }.distinctUntilChanged()

    override fun observeMyReservations(): Flow<List<Reservation>> =
        currentUserId().flatMapLatest { userId ->
            if (userId == null) {
                flowOf(emptyList())
            } else {
                reservationDao.observeByPassenger(userId).map { rows -> rows.map(ReservationEntity::toDomain) }
            }
        }.distinctUntilChanged()

    override fun observePendingReservationIds(): Flow<Set<ReservationId>> =
        reservationDao.observePendingReservationIds()
            .map { ids -> ids.mapTo(HashSet(), ::ReservationId) }
            .distinctUntilChanged()

    override fun observeRejections(): Flow<Map<ReservationId, ReservationRejection>> = rejections.observe()

    override suspend fun dismissRejection(reservationId: ReservationId) {
        rejections.remove(reservationId)
    }

    override suspend fun refreshTripReservations(tripId: TripId): AppResult<Unit> = safeApiCall {
        reservationDao.upsertFromServer(remote.getTripReservations(tripId.value).map { it.toDomain().toEntity(isSync = true) })
    }

    override suspend fun requestSeat(tripId: TripId): AppResult<ReservationId> {
        val user = when (val result = safeApiCall { userDao.getCurrentUser() }) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data ?: return AppResult.Failure(DataError.UNAUTHORIZED)
        }
        val now = clock.instant()
        val row = ReservationEntity(
            reservationId = UUID.randomUUID().toString(),
            tripId = tripId.value,
            passengerId = user.id,
            passengerName = publicName(user.fullName),
            passengerRating = user.ratingAvg,
            // RNF07: the own phone is not part of the request; the server decides who sees it
            passengerPhone = null,
            status = ReservationStatus.PENDIENTE,
            reservedAt = now,
            updatedAt = now,
            isSync = false,
        )
        return save(row).map { ReservationId(row.reservationId) }
    }

    override suspend fun accept(tripId: TripId, reservationId: ReservationId): AppResult<Unit> =
        change(tripId, reservationId, ReservationStatus.ACEPTADA)

    override suspend fun reject(tripId: TripId, reservationId: ReservationId): AppResult<Unit> =
        change(tripId, reservationId, ReservationStatus.RECHAZADA)

    override suspend fun cancel(tripId: TripId, reservationId: ReservationId): AppResult<Unit> =
        change(tripId, reservationId, ReservationStatus.CANCELADA)

    /** Saves the new status locally; repeating a change that is already there does nothing. */
    private suspend fun change(tripId: TripId, reservationId: ReservationId, status: ReservationStatus): AppResult<Unit> {
        val row = when (val result = safeApiCall { reservationDao.getReservation(reservationId.value) }) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data?.takeIf { it.tripId == tripId.value }
                ?: return AppResult.Failure(DataError.NOT_FOUND)
        }
        if (row.status == status) return AppResult.Success(Unit)
        return save(row.copy(status = status, updatedAt = clock.instant(), isSync = false))
    }

    private suspend fun save(row: ReservationEntity): AppResult<Unit> =
        safeApiCall { reservationDao.upsert(row) }.also {
            if (it is AppResult.Success) syncScheduler.schedule()
        }

    private fun currentUserId(): Flow<String?> = userDao.observeCurrentUser().map { it?.id }.distinctUntilChanged()

    private fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
        is AppResult.Success -> AppResult.Success(transform(data))
        is AppResult.Failure -> this
    }

    private companion object {
        /** How others see the user until the server answers: "Lucía F." (RNF07, docs/API.md §3.2). */
        fun publicName(fullName: String): String {
            val parts = fullName.trim().split(Regex("\\s+"))
            return if (parts.size < 2) parts.first() else "${parts.first()} ${parts.last().first()}."
        }
    }
}
