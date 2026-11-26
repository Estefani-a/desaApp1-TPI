package com.example.uniride.fake

import com.example.uniride.data.local.dao.ReservationDao
import com.example.uniride.data.local.entity.ReservationEntity
import com.example.uniride.data.remote.datasource.FakeBackendStore
import com.example.uniride.domain.model.ReservationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * [ReservationDao] in memory. It inherits the real `upsertFromServer`, so the tests exercise the
 * rule "a change not sent yet wins over the server"; only the SQL queries are written again.
 */
class FakeReservationDao : ReservationDao {

    val rows = MutableStateFlow<Map<String, ReservationEntity>>(emptyMap())

    fun all(): List<ReservationEntity> = rows.value.values.toList()

    override fun observeByTrip(tripId: String): Flow<List<ReservationEntity>> =
        rows.map { map -> map.values.filter { it.tripId == tripId }.sortedBy { it.reservedAt } }

    override fun observeByPassenger(passengerId: String): Flow<List<ReservationEntity>> =
        rows.map { map -> map.values.filter { it.passengerId == passengerId }.sortedByDescending { it.reservedAt } }

    override fun observeReservation(reservationId: String): Flow<ReservationEntity?> = rows.map { it[reservationId] }

    override fun observeByTripAndPassenger(tripId: String, passengerId: String): Flow<List<ReservationEntity>> =
        rows.map { map ->
            map.values.filter { it.tripId == tripId && it.passengerId == passengerId }.sortedByDescending { it.reservedAt }
        }

    override suspend fun getReservation(reservationId: String): ReservationEntity? = rows.value[reservationId]

    override fun observePendingReservationIds(): Flow<List<String>> =
        rows.map { map -> map.values.filter { !it.isSync }.map { it.reservationId } }

    override suspend fun getPendingReservations(): List<ReservationEntity> = rows.value.values.filter { !it.isSync }

    override suspend fun upsert(reservation: ReservationEntity) {
        rows.value = rows.value + (reservation.reservationId to reservation)
    }

    override suspend fun upsertAll(reservations: List<ReservationEntity>) {
        rows.value = rows.value + reservations.associateBy { it.reservationId }
    }

    override suspend fun markSynced(reservationId: String) {
        val row = rows.value[reservationId] ?: return
        rows.value = rows.value + (reservationId to row.copy(isSync = true))
    }

    override suspend fun getPendingIds(): List<String> = rows.value.values.filter { !it.isSync }.map { it.reservationId }
}

/** A reservation row for the repository and sync tests. */
fun reservationRow(
    id: String = "res-1",
    tripId: String = FakeBackendStore.DEMO_TRIP_MONSERRAT_ID,
    passengerId: String = FakeBackendStore.DEFAULT_PASSENGER_ID,
    status: ReservationStatus = ReservationStatus.PENDIENTE,
    reservedAt: Instant = Instant.parse("2026-11-20T21:00:00Z"),
    isSync: Boolean = false,
) = ReservationEntity(
    reservationId = id,
    tripId = tripId,
    passengerId = passengerId,
    passengerName = "Martín G.",
    passengerRating = 4.5,
    passengerPhone = null,
    status = status,
    reservedAt = reservedAt,
    updatedAt = reservedAt,
    isSync = isSync,
)
