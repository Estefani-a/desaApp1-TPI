package com.example.uniride.domain.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.domain.model.Reservation
import com.example.uniride.domain.model.ReservationId
import com.example.uniride.domain.model.TripId
import kotlinx.coroutines.flow.Flow

/**
 * Seat requests (RF04). Every change works offline: it is visible right away as pending and
 * is sent when there is connection. If the server rejects it, the reservation shows the real
 * status.
 */
interface ReservationRepository {

    /** Requests on a trip the user drives. */
    fun observeTripReservations(tripId: TripId): Flow<List<Reservation>>

    /** The user's reservation on [tripId]: the active one, or the latest one, or `null`. */
    fun observeMyReservation(tripId: TripId): Flow<Reservation?>

    /** The user's reservations as passenger, newest first. */
    fun observeMyReservations(): Flow<List<Reservation>>

    suspend fun refreshTripReservations(tripId: TripId): AppResult<Unit>

    /** Asks for a seat. Returns the id of the new reservation. */
    suspend fun requestSeat(tripId: TripId): AppResult<ReservationId>

    suspend fun accept(tripId: TripId, reservationId: ReservationId): AppResult<Unit>

    suspend fun reject(tripId: TripId, reservationId: ReservationId): AppResult<Unit>

    suspend fun cancel(tripId: TripId, reservationId: ReservationId): AppResult<Unit>
}
