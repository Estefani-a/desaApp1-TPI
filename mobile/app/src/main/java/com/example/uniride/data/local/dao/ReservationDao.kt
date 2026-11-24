package com.example.uniride.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.uniride.data.local.entity.ReservationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReservationDao {

    @Query("SELECT * FROM reservations WHERE tripId = :tripId ORDER BY reservedAt")
    fun observeByTrip(tripId: String): Flow<List<ReservationEntity>>

    /** Newest first. */
    @Query("SELECT * FROM reservations WHERE passengerId = :passengerId ORDER BY reservedAt DESC")
    fun observeByPassenger(passengerId: String): Flow<List<ReservationEntity>>

    @Query("SELECT * FROM reservations WHERE reservationId = :reservationId")
    fun observeReservation(reservationId: String): Flow<ReservationEntity?>

    /** Ids of the reservations created or changed on this device that the server did not confirm. */
    @Query("SELECT reservationId FROM reservations WHERE isSync = 0")
    fun observePendingReservationIds(): Flow<List<String>>

    @Query("SELECT * FROM reservations WHERE isSync = 0")
    suspend fun getPendingReservations(): List<ReservationEntity>

    @Upsert
    suspend fun upsert(reservation: ReservationEntity)

    @Upsert
    suspend fun upsertAll(reservations: List<ReservationEntity>)

    @Query("UPDATE reservations SET isSync = 1 WHERE reservationId = :reservationId")
    suspend fun markSynced(reservationId: String)
}
