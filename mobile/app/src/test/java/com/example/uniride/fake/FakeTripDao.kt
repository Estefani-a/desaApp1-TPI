package com.example.uniride.fake

import com.example.uniride.data.local.dao.TripDao
import com.example.uniride.data.local.dao.UserDao
import com.example.uniride.data.local.entity.TripEntity
import com.example.uniride.data.local.entity.UserEntity
import com.example.uniride.data.remote.datasource.TripRemoteDataSource
import com.example.uniride.data.remote.dto.PublishTripRequest
import com.example.uniride.data.remote.dto.TripDto
import com.example.uniride.data.sync.SyncScheduler
import com.example.uniride.domain.model.Direction
import com.example.uniride.domain.model.TripStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * [TripDao] in memory. It inherits the real `replaceCatalog`, so the tests exercise the
 * retention logic; only the SQL queries are written again here. [activeReservationTripIds]
 * stands for the reservations table.
 */
class FakeTripDao : TripDao {

    val rows = MutableStateFlow<Map<String, TripEntity>>(emptyMap())

    /** User id → ids of the trips where the user has an active reservation. */
    val activeReservationTripIds = mutableMapOf<String, Set<String>>()

    private fun all() = rows.map { it.values.toList() }

    override fun observeOpenTrips(campus: String?, direction: Direction?, date: LocalDate?): Flow<List<TripEntity>> =
        all().map { list -> list.filter { it.status == TripStatus.ABIERTO } }

    override fun observeListedTrips(): Flow<List<TripEntity>> =
        all().map { list ->
            list.filter { it.status == TripStatus.ABIERTO || it.status == TripStatus.COMPLETO }
                .sortedWith(compareBy({ it.departureDate }, { it.departureTime }))
        }

    override fun observeMyTrips(userId: String): Flow<List<TripEntity>> =
        all().map { list ->
            val reserved = activeReservationTripIds[userId].orEmpty()
            list.filter { it.driverId == userId || it.tripId in reserved }
        }

    override fun observeTrip(tripId: String): Flow<TripEntity?> = rows.map { it[tripId] }

    override fun observeTripsByDriver(driverId: String): Flow<List<TripEntity>> =
        all().map { list -> list.filter { it.driverId == driverId } }

    override fun observePendingTripIds(): Flow<List<String>> =
        all().map { list -> list.filter { !it.isSync }.map { it.tripId } }

    override suspend fun getPendingTrips(): List<TripEntity> = rows.value.values.filter { !it.isSync }

    override suspend fun upsert(trip: TripEntity) {
        rows.value = rows.value + (trip.tripId to trip)
    }

    override suspend fun upsertAll(trips: List<TripEntity>) {
        rows.value = rows.value + trips.associateBy { it.tripId }
    }

    override suspend fun markSynced(tripId: String) {
        rows.value[tripId]?.let { upsert(it.copy(isSync = true)) }
    }

    override suspend fun getAll(): List<TripEntity> = rows.value.values.toList()

    override suspend fun deleteByIds(tripIds: List<String>) {
        rows.value = rows.value - tripIds.toSet()
    }

    override suspend fun getActiveReservationTripIds(userId: String): List<String> =
        activeReservationTripIds[userId].orEmpty().toList()
}

/** [UserDao] in memory with at most one user. */
class FakeUserDao(initial: UserEntity? = null) : UserDao {

    val user = MutableStateFlow(initial)

    override fun observeCurrentUser(): Flow<UserEntity?> = user

    override suspend fun getCurrentUser(): UserEntity? = user.value

    override suspend fun upsert(user: UserEntity) {
        this.user.value = user
    }

    override suspend fun clear() {
        user.value = null
    }
}

/** Counts how many times a sync was requested. */
class FakeSyncScheduler : SyncScheduler {
    var scheduleCalls = 0
        private set

    override fun schedule() {
        scheduleCalls++
    }
}

/**
 * Wraps a [TripRemoteDataSource]: while [failWith] is set every call throws it (for example an
 * `IOException` for "no connection"), and [calls] counts the calls that reached it.
 */
class ToggleableTripRemote(private val delegate: TripRemoteDataSource) : TripRemoteDataSource {

    var failWith: Throwable? = null
    var calls = 0
        private set

    private suspend fun <T> pass(block: suspend () -> T): T {
        calls++
        failWith?.let { throw it }
        return block()
    }

    override suspend fun getTrips(
        campus: String?,
        direction: String?,
        date: String?,
        shift: String?,
        updatedSince: String?,
    ) = pass { delegate.getTrips(campus, direction, date, shift, updatedSince) }

    override suspend fun getCampuses() = pass { delegate.getCampuses() }

    override suspend fun getTripDetail(tripId: String) = pass { delegate.getTripDetail(tripId) }

    override suspend fun publishTrip(tripId: String, request: PublishTripRequest): TripDto =
        pass { delegate.publishTrip(tripId, request) }

    override suspend fun getMyTrips() = pass { delegate.getMyTrips() }

    override suspend fun finishTrip(tripId: String) = pass { delegate.finishTrip(tripId) }

    override suspend fun cancelTrip(tripId: String) = pass { delegate.cancelTrip(tripId) }
}
