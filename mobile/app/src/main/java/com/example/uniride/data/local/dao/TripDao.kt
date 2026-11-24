package com.example.uniride.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.uniride.data.local.entity.TripEntity
import com.example.uniride.domain.model.Direction
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
}
