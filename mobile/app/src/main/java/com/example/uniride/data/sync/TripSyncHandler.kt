package com.example.uniride.data.sync

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.data.local.dao.TripDao
import com.example.uniride.data.local.entity.TripEntity
import com.example.uniride.data.mapper.toDomain
import com.example.uniride.data.mapper.toEntity
import com.example.uniride.data.remote.datasource.TripRemoteDataSource
import com.example.uniride.data.remote.dto.PublishTripRequest
import com.example.uniride.data.remote.safeApiCall
import com.example.uniride.data.repository.TripRejectionStore
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.model.TripStatus
import java.time.Clock
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Sends the trips published offline (`isSync = false`) with the idempotent `PUT /trips/{id}`
 * (API §5.4), following the rules of [SyncHandler].
 *
 * A trip the server rejects is not retried: it stays as `CANCELADO` with `isSync = true`, and
 * the reason is saved in [TripRejectionStore] for the UI (`TripRepository.observeRejectedTrips`).
 */
class TripSyncHandler @Inject constructor(
    private val tripDao: TripDao,
    private val remote: TripRemoteDataSource,
    private val rejections: TripRejectionStore,
    private val clock: Clock,
) : SyncHandler {

    override suspend fun syncPending(): SyncOutcome {
        val pending = tripDao.getPendingTrips()
        if (pending.isEmpty()) return SyncOutcome.DONE

        for (trip in pending) {
            val result = safeApiCall { remote.publishTrip(trip.tripId, trip.toRequest()).toDomain().toEntity(isSync = true) }
            when (result) {
                is AppResult.Success -> tripDao.upsert(result.data)
                is AppResult.Failure -> when (result.error) {
                    DataError.NO_CONNECTION, DataError.TIMEOUT, DataError.SERVER -> return SyncOutcome.RETRY
                    DataError.UNAUTHORIZED -> return SyncOutcome.STOP
                    else -> reject(trip, result.error)
                }
            }
        }
        return SyncOutcome.PROGRESS
    }

    /** The reason is saved first: if the process dies in between, the next pass rejects it again. */
    private suspend fun reject(trip: TripEntity, error: DataError) {
        rejections.add(TripId(trip.tripId), error)
        tripDao.upsert(trip.copy(status = TripStatus.CANCELADO, isSync = true, updatedAt = clock.instant()))
    }

    private fun TripEntity.toRequest() = PublishTripRequest(
        direction = direction.name,
        campus = campus,
        meetingPoint = meetingPoint,
        destinationNeighborhood = destinationNeighborhood,
        destinationReference = destinationReference,
        departureDate = departureDate.toString(),
        departureTime = departureTime.format(HOUR_MINUTE),
        estimatedArrivalTime = estimatedArrivalTime?.format(HOUR_MINUTE),
        totalSeats = totalSeats,
        contributionPerSeat = contributionPerSeat,
    )

    private companion object {
        val HOUR_MINUTE: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
