package com.example.uniride.domain.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.domain.model.Reservation
import com.example.uniride.domain.model.ReservationId
import com.example.uniride.domain.model.ReservationRejection
import com.example.uniride.domain.model.TripId
import kotlinx.coroutines.flow.Flow

/**
 * Seat requests (RF04). Every change works offline: it is visible right away, appears in
 * [observePendingReservationIds] and is sent when there is connection. If the server rejects
 * it, the reservation shows the real status and [observeRejections] says why.
 */
interface ReservationRepository {

    /** Requests on a trip the user drives. */
    fun observeTripReservations(tripId: TripId): Flow<List<Reservation>>

    /** The user's reservation on [tripId]: the active one, or the latest one, or `null`. */
    fun observeMyReservation(tripId: TripId): Flow<Reservation?>

    /** The user's reservations as passenger, newest first. */
    fun observeMyReservations(): Flow<List<Reservation>>

    /**
     * Reservations created or changed on this device that the server did not confirm yet. The
     * screens combine it with the reservations to show "Pendiente de sincronizar".
     */
    fun observePendingReservationIds(): Flow<Set<ReservationId>>

    /** Changes the server did not accept, with the reason, until the user dismisses them. */
    fun observeRejections(): Flow<Map<ReservationId, ReservationRejection>>

    suspend fun dismissRejection(reservationId: ReservationId)

    suspend fun refreshTripReservations(tripId: TripId): AppResult<Unit>

    /** Asks for a seat. Returns the id of the new reservation. */
    suspend fun requestSeat(tripId: TripId): AppResult<ReservationId>

    suspend fun accept(tripId: TripId, reservationId: ReservationId): AppResult<Unit>

    suspend fun reject(tripId: TripId, reservationId: ReservationId): AppResult<Unit>

    suspend fun cancel(tripId: TripId, reservationId: ReservationId): AppResult<Unit>
}
