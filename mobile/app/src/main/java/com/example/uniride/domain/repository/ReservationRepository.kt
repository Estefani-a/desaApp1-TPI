package com.example.uniride.domain.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.domain.model.Reservation
import kotlinx.coroutines.flow.Flow

/**
 * Seat requests (RF04, docs/API.md §6). Every write is saved in Room with `isSync = false`
 * and sent by the sync worker (CONVENCIONES §6.2), so all of them work offline.
 */
interface ReservationRepository {

    /** Requests of a trip, for the driver panel (C4). */
    fun observeTripReservations(tripId: String): Flow<List<Reservation>>

    /** Reservations of the current user as passenger, newest first. */
    fun observeMyReservations(): Flow<List<Reservation>>

    /** Downloads `GET /trips/{tripId}/reservations` into Room (driver only). */
    suspend fun refreshTripReservations(tripId: String): AppResult<Unit>

    /** Creates a `PENDIENTE` reservation. Returns the `reservationId` generated in the app. */
    suspend fun requestSeat(tripId: String): AppResult<String>

    suspend fun acceptReservation(tripId: String, reservationId: String): AppResult<Unit>

    suspend fun rejectReservation(tripId: String, reservationId: String): AppResult<Unit>

    suspend fun cancelReservation(tripId: String, reservationId: String): AppResult<Unit>
}
