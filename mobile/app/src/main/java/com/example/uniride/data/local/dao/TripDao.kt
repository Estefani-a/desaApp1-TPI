package com.example.uniride.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.uniride.data.local.CatalogRetention
import com.example.uniride.data.local.entity.TripEntity
import com.example.uniride.domain.model.Direction
import com.example.uniride.domain.model.TripStatus
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    /**
     * Trips that still take passengers, soonest first. A `null` filter means "any". The shift
     * filter is not here: it is a range of hours of the day and the repository applies it.
     */
    @Query(
        """
        SELECT * FROM trips
        WHERE status = 'ABIERTO'
            AND (:campus IS NULL OR campus = :campus)
            AND (:direction IS NULL OR direction = :direction)
            AND (:date IS NULL OR departureDate = :date)
        ORDER BY departureDate, departureTime
        """,
    )
    fun observeOpenTrips(campus: String?, direction: Direction?, date: LocalDate?): Flow<List<TripEntity>>

    /**
     * Trips of the feed: the ones that are listed (`ABIERTO` or `COMPLETO`). The repository
     * applies the filters, including the shift, with `TripFilters.matches`.
     */
    @Query("SELECT * FROM trips WHERE status IN ('ABIERTO', 'COMPLETO') ORDER BY departureDate, departureTime")
    fun observeListedTrips(): Flow<List<TripEntity>>

    /** Trips [userId] offers or has an active reservation on, newest first (RF04 "Mi viaje"). */
    @Query(
        """
        SELECT * FROM trips
        WHERE driverId = :userId
            OR tripId IN (
                SELECT tripId FROM reservations
                WHERE passengerId = :userId AND status IN ('PENDIENTE', 'ACEPTADA')
            )
        ORDER BY departureDate DESC, departureTime DESC
        """,
    )
    fun observeMyTrips(userId: String): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE tripId = :tripId")
    fun observeTrip(tripId: String): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE driverId = :driverId ORDER BY departureDate DESC, departureTime DESC")
    fun observeTripsByDriver(driverId: String): Flow<List<TripEntity>>

    /** Ids of the trips created or changed on this device that the server did not confirm yet. */
    @Query("SELECT tripId FROM trips WHERE isSync = 0")
    fun observePendingTripIds(): Flow<List<String>>

    @Query("SELECT * FROM trips WHERE isSync = 0")
    suspend fun getPendingTrips(): List<TripEntity>

    @Upsert
    suspend fun upsert(trip: TripEntity)

    @Upsert
    suspend fun upsertAll(trips: List<TripEntity>)

    @Query("UPDATE trips SET isSync = 1 WHERE tripId = :tripId")
    suspend fun markSynced(tripId: String)

    @Query("SELECT * FROM trips")
    suspend fun getAll(): List<TripEntity>

    @Query("DELETE FROM trips WHERE tripId IN (:tripIds)")
    suspend fun deleteByIds(tripIds: List<String>)

    /** Trips where [userId] holds an active reservation. Used by [replaceCatalog]. */
    @Query("SELECT tripId FROM reservations WHERE passengerId = :userId AND status IN ('PENDIENTE', 'ACEPTADA')")
    suspend fun getActiveReservationTripIds(userId: String): List<String>

    /**
     * Applies a refresh of the catalog (preentrega 11.2) in one transaction with
     * [CatalogRetention]: [full] is `GET /trips` without `updatedSince` (replace), otherwise
     * the changed trips are merged. A trip stays if it is not synced, if [userId] has an active
     * reservation on it (read here, inside the transaction, so a reservation just created is
     * not lost) or if its id is in [keepTripIds]. [fromServer] must come with `isSync = true`.
     */
    @Transaction
    suspend fun replaceCatalog(fromServer: List<TripEntity>, full: Boolean, userId: String?, keepTripIds: Set<String>) {
        val keep = keepTripIds + (userId?.let { getActiveReservationTripIds(it) } ?: emptyList())
        val current = getAll()
        val mustKeep = { trip: TripEntity -> !trip.isSync || trip.tripId in keep }
        val result = if (full) {
            CatalogRetention.replace(current, fromServer, TripEntity::tripId, mustKeep)
        } else {
            CatalogRetention.merge(current, fromServer, TripEntity::tripId, ::isListed, mustKeep)
        }
        val keptIds = result.mapTo(HashSet(), TripEntity::tripId)
        current.map { it.tripId }.filter { it !in keptIds }.chunked(DELETE_CHUNK).forEach { deleteByIds(it) }
        upsertAll(result)
    }
}

/** SQLite limits the variables of a statement. */
private const val DELETE_CHUNK = 500

private fun isListed(trip: TripEntity): Boolean =
    trip.status == TripStatus.ABIERTO || trip.status == TripStatus.COMPLETO
