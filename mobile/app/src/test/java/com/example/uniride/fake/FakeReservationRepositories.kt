package com.example.uniride.fake

import com.example.uniride.core.result.AppResult
import com.example.uniride.domain.model.Participant
import com.example.uniride.domain.model.Reservation
import com.example.uniride.domain.model.ReservationId
import com.example.uniride.domain.model.ReservationRejection
import com.example.uniride.domain.model.ReservationStatus
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.model.UserId
import com.example.uniride.domain.repository.ReservationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * [ReservationRepository] in memory for the use case tests. [reservations] holds every
 * reservation of the phone; "mine" are the ones of [currentUserId].
 */
class FakeReservationRepository(
    initial: List<Reservation> = emptyList(),
    var currentUserId: String? = null,
) : ReservationRepository {

    val reservations = MutableStateFlow(initial)
    val pendingIds = MutableStateFlow<Set<ReservationId>>(emptySet())
    val rejections = MutableStateFlow<Map<ReservationId, ReservationRejection>>(emptyMap())

    /** Calls received, as `"accept:res-1"`, `"requestSeat:trip-1"`… */
    val calls = mutableListOf<String>()
    var refreshResult: AppResult<Unit> = AppResult.Success(Unit)

    override fun observeTripReservations(tripId: TripId): Flow<List<Reservation>> =
        reservations.map { list -> list.filter { it.tripId == tripId } }

    override fun observeMyReservation(tripId: TripId): Flow<Reservation?> =
        reservations.map { list -> Reservation.currentOf(list.filter { it.tripId == tripId && it.passenger.id.value == currentUserId }) }

    override fun observeMyReservations(): Flow<List<Reservation>> =
        reservations.map { list -> list.filter { it.passenger.id.value == currentUserId }.sortedByDescending { it.reservedAt } }

    override fun observePendingReservationIds(): Flow<Set<ReservationId>> = pendingIds

    override fun observeRejections(): Flow<Map<ReservationId, ReservationRejection>> = rejections

    override suspend fun dismissRejection(reservationId: ReservationId) {
        rejections.value = rejections.value - reservationId
    }

    override suspend fun refreshTripReservations(tripId: TripId): AppResult<Unit> {
        calls += "refresh:${tripId.value}"
        return refreshResult
    }

    /** Like the real one: it saves a pending reservation of [currentUserId] on the phone. */
    override suspend fun requestSeat(tripId: TripId): AppResult<ReservationId> {
        calls += "requestSeat:${tripId.value}"
        val id = ReservationId("nueva")
        val now = Instant.parse("2026-11-20T21:00:00Z")
        reservations.value = reservations.value + Reservation(
            id = id,
            tripId = tripId,
            passenger = Participant(UserId(currentUserId.orEmpty()), "Martín G.", null, phone = null),
            status = ReservationStatus.PENDIENTE,
            reservedAt = now,
            updatedAt = now,
        )
        pendingIds.value = pendingIds.value + id
        return AppResult.Success(id)
    }

    override suspend fun accept(tripId: TripId, reservationId: ReservationId): AppResult<Unit> = record("accept", reservationId)

    override suspend fun reject(tripId: TripId, reservationId: ReservationId): AppResult<Unit> = record("reject", reservationId)

    override suspend fun cancel(tripId: TripId, reservationId: ReservationId): AppResult<Unit> = record("cancel", reservationId)

    private fun record(action: String, reservationId: ReservationId): AppResult<Unit> {
        calls += "$action:${reservationId.value}"
        return AppResult.Success(Unit)
    }
}
