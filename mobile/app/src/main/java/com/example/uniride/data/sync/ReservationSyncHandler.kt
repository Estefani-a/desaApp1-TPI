package com.example.uniride.data.sync

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.data.local.dao.ReservationDao
import com.example.uniride.data.local.dao.TripDao
import com.example.uniride.data.local.dao.UserDao
import com.example.uniride.data.local.entity.ReservationEntity
import com.example.uniride.data.mapper.toDomain
import com.example.uniride.data.mapper.toEntity
import com.example.uniride.data.remote.ApiException
import com.example.uniride.data.remote.datasource.ReservationRemoteDataSource
import com.example.uniride.data.remote.datasource.TripRemoteDataSource
import com.example.uniride.data.remote.dto.ApiErrorCode
import com.example.uniride.data.remote.dto.ReservationDto
import com.example.uniride.data.remote.safeApiCall
import com.example.uniride.data.repository.ReservationRejectionStore
import com.example.uniride.domain.model.ReservationId
import com.example.uniride.domain.model.ReservationRejection
import com.example.uniride.domain.model.ReservationStatus
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.repository.TripRepository
import java.time.Clock
import javax.inject.Inject

/**
 * Sends the reservation changes made on this device (`isSync = false`), following the rules of
 * [SyncHandler]. The status of the row says what to send (docs/API.md §6):
 * `PENDIENTE` → solicitar (idempotent `PUT`), `ACEPTADA` → aceptar, `RECHAZADA` → rechazar and
 * `CANCELADA` → cancelar. The transitions are idempotent too, so sending one twice is safe.
 *
 * When the server rejects a change (409 and the like) it is not retried:
 * - the reason is saved in [ReservationRejectionStore] for the screen (for example,
 *   `TRIP_ALREADY_DEPARTED`: "No se puede cancelar un viaje que ya empezó");
 * - the row takes the real status: a request that never reached the server is closed as
 *   `CANCELADA`, and any other change is replaced by what the server has;
 * - the trip is asked again, because its seats may have changed (docs/API.md §6.3).
 */
class ReservationSyncHandler @Inject constructor(
    private val reservationDao: ReservationDao,
    private val userDao: UserDao,
    private val remote: ReservationRemoteDataSource,
    private val tripRemote: TripRemoteDataSource,
    private val tripDao: TripDao,
    private val tripRepository: TripRepository,
    private val rejections: ReservationRejectionStore,
    private val clock: Clock,
) : SyncHandler {

    override suspend fun syncPending(): SyncOutcome {
        val pending = reservationDao.getPendingReservations()
        if (pending.isEmpty()) return SyncOutcome.DONE

        for (row in pending) {
            val sent = send { remote.sendChange(row) }
            when (val result = sent.result) {
                is AppResult.Success -> {
                    reservationDao.upsert(result.data.toDomain().toEntity(isSync = true))
                    refreshTrip(row)
                }
                is AppResult.Failure -> when (result.error) {
                    DataError.NO_CONNECTION, DataError.TIMEOUT, DataError.SERVER -> return SyncOutcome.RETRY
                    DataError.UNAUTHORIZED -> return SyncOutcome.STOP
                    else -> resolveRejection(row, sent.code)?.let { return it }
                }
            }
        }
        return SyncOutcome.PROGRESS
    }

    private suspend fun ReservationRemoteDataSource.sendChange(row: ReservationEntity): ReservationDto =
        when (row.status) {
            ReservationStatus.PENDIENTE -> requestSeat(row.tripId, row.reservationId)
            ReservationStatus.ACEPTADA -> acceptReservation(row.tripId, row.reservationId)
            ReservationStatus.RECHAZADA -> rejectReservation(row.tripId, row.reservationId)
            ReservationStatus.CANCELADA -> cancelReservation(row.tripId, row.reservationId)
        }

    /**
     * Applies a rejection. Returns an outcome only if the pass has to stop (the real status could
     * not be read), otherwise `null` to go on with the next row.
     */
    private suspend fun resolveRejection(row: ReservationEntity, code: ApiErrorCode?): SyncOutcome? {
        // A request made and cancelled offline never reached the server: there is nothing to undo
        if (row.status == ReservationStatus.CANCELADA && code == ApiErrorCode.NOT_FOUND) {
            reservationDao.markSynced(row.reservationId)
            return null
        }
        val isPassenger = row.passengerId == userDao.getCurrentUser()?.id
        val realState = if (isPassenger) readAsPassenger(row) else readAsDriver(row)
        when (realState) {
            is AppResult.Failure -> return outcomeOf(realState.error)
            is AppResult.Success -> {
                // The reason first: if the process dies in between, the next pass rejects it again
                rejections.add(ReservationId(row.reservationId), code.toRejection())
                reservationDao.upsert(realState.data)
            }
        }
        return null
    }

    /**
     * What the server has for a change of the passenger: the trip is asked again, which brings
     * their reservation (`myReservation`). If this one is not there, it never reached the server.
     */
    private suspend fun readAsPassenger(row: ReservationEntity): AppResult<ReservationEntity> {
        val detail = send { tripRemote.getTripDetail(row.tripId) }.result
        if (detail is AppResult.Failure && detail.error.isTransient()) return detail
        val trip = (detail as? AppResult.Success)?.data
        if (trip != null) {
            tripDao.upsert(trip.toDomain().toEntity(isSync = true))
        }
        val mine = trip?.myReservation
        // The server may have another reservation of the passenger (ALREADY_RESERVED): keep it too
        if (mine != null && mine.reservationId != row.reservationId) {
            reservationDao.upsertFromServer(listOf(mine.toDomain().toEntity(isSync = true)))
        }
        val real = when {
            mine != null && mine.reservationId == row.reservationId -> mine.toDomain().toEntity(isSync = true)
            row.status == ReservationStatus.PENDIENTE -> row.copy(status = ReservationStatus.CANCELADA, updatedAt = clock.instant())
            else -> row
        }
        return AppResult.Success(real.copy(isSync = true))
    }

    /** What the server has for a change of the driver: the requests of the trip. */
    private suspend fun readAsDriver(row: ReservationEntity): AppResult<ReservationEntity> {
        val result = send { remote.getTripReservations(row.tripId) }.result
        if (result is AppResult.Failure && result.error.isTransient()) return result
        tripRepository.refreshTrip(TripId(row.tripId))
        val real = (result as? AppResult.Success)?.data?.firstOrNull { it.reservationId == row.reservationId }
        return AppResult.Success(real?.toDomain()?.toEntity(isSync = true) ?: row.copy(isSync = true))
    }

    /** Best effort: the seats changed; if it fails, the next refresh brings them. */
    private suspend fun refreshTrip(row: ReservationEntity) {
        tripRepository.refreshTrip(TripId(row.tripId))
    }

    private fun DataError.isTransient() =
        this == DataError.NO_CONNECTION || this == DataError.TIMEOUT || this == DataError.SERVER || this == DataError.UNAUTHORIZED

    private fun outcomeOf(error: DataError) = if (error == DataError.UNAUTHORIZED) SyncOutcome.STOP else SyncOutcome.RETRY

    private fun ApiErrorCode?.toRejection(): ReservationRejection = when (this) {
        ApiErrorCode.NO_SEATS_AVAILABLE -> ReservationRejection.NO_SEATS
        ApiErrorCode.TRIP_NOT_OPEN -> ReservationRejection.TRIP_NOT_OPEN
        ApiErrorCode.ALREADY_RESERVED -> ReservationRejection.ALREADY_RESERVED
        ApiErrorCode.TRIP_ALREADY_DEPARTED -> ReservationRejection.TRIP_ALREADY_DEPARTED
        else -> ReservationRejection.OTHER
    }

    /** `safeApiCall` that also keeps the `code` of the error body, to know why it was rejected. */
    private suspend fun <T> send(block: suspend () -> T): Sent<T> {
        var code: ApiErrorCode? = null
        val result = safeApiCall {
            try {
                block()
            } catch (e: ApiException) {
                code = e.apiError.errorCode
                throw e
            }
        }
        return Sent(result, code)
    }

    private class Sent<T>(val result: AppResult<T>, val code: ApiErrorCode?)
}
