package com.example.uniride.data.remote.datasource

import com.example.uniride.data.remote.dto.ApiErrorCode
import com.example.uniride.data.remote.dto.ReservationDto
import kotlinx.coroutines.delay

/**
 * In-memory fake of [ReservationRemoteDataSource] enforcing seat counts, idempotency and RNF07 (A2).
 */
class FakeReservationRemoteDataSource(
    private val store: FakeBackendStore,
    private val simulatedDelayMs: Long = 0L,
) : ReservationRemoteDataSource {

    override suspend fun requestSeat(tripId: String, reservationId: String): ReservationDto {
        maybeDelay()
        return store.withState {
            val passengerId = store.requireCurrentUser().user.id
            val existing = store.reservations[reservationId]
            if (existing != null) {
                if (existing.passengerId == passengerId && existing.tripId == tripId) {
                    return@withState store.toReservationDto(existing, passengerId)
                }
                throw store.apiError(409, ApiErrorCode.ID_CONFLICT, "El identificador de reserva ya existe.")
            }

            val trip = store.trips[tripId]
                ?: throw store.apiError(404, ApiErrorCode.NOT_FOUND, "Viaje no encontrado.")
            if (trip.driverId == passengerId) {
                throw store.apiError(403, ApiErrorCode.CANNOT_RESERVE_OWN_TRIP, "No podés reservar en tu propio viaje.")
            }
            // Same order as the backend: an active reservation wins over the state of the trip
            val alreadyActive = store.reservations.values.any {
                it.tripId == tripId &&
                    it.passengerId == passengerId &&
                    (it.status == FakeBackendStore.STATUS_PENDING || it.status == FakeBackendStore.STATUS_ACCEPTED)
            }
            if (alreadyActive) {
                throw store.apiError(409, ApiErrorCode.ALREADY_RESERVED, "Ya tenés una reserva activa en este viaje.")
            }
            if (trip.status == FakeBackendStore.STATUS_FINISHED ||
                trip.status == FakeBackendStore.STATUS_CANCELLED ||
                !store.isUpcoming(trip.departureDate, trip.departureTime)
            ) {
                throw store.apiError(409, ApiErrorCode.TRIP_NOT_OPEN, "El viaje ya no está disponible.")
            }
            if (trip.status == FakeBackendStore.STATUS_FULL || trip.availableSeats <= 0) {
                throw store.apiError(409, ApiErrorCode.NO_SEATS_AVAILABLE, "El viaje ya no tiene plazas disponibles.")
            }

            val now = store.nowIso()
            val created = FakeBackendStore.StoredReservation(
                reservationId = reservationId,
                tripId = tripId,
                passengerId = passengerId,
                status = FakeBackendStore.STATUS_PENDING,
                reservedAt = now,
                updatedAt = now,
            )
            store.reservations[reservationId] = created
            store.toReservationDto(created, passengerId)
        }
    }

    override suspend fun getTripReservations(tripId: String): List<ReservationDto> {
        maybeDelay()
        return store.withState {
            val viewerId = store.requireCurrentUser().user.id
            val trip = store.trips[tripId]
                ?: throw store.apiError(404, ApiErrorCode.NOT_FOUND, "Viaje no encontrado.")
            if (trip.driverId != viewerId) {
                throw store.apiError(403, ApiErrorCode.FORBIDDEN, "Solo el conductor puede ver las solicitudes.")
            }
            store.reservations.values
                .filter { it.tripId == tripId }
                .sortedBy { it.reservedAt }
                .map { store.toReservationDto(it, viewerId) }
        }
    }

    override suspend fun acceptReservation(tripId: String, reservationId: String): ReservationDto {
        maybeDelay()
        return store.withState {
            val viewerId = store.requireCurrentUser().user.id
            val trip = store.trips[tripId]
                ?: throw store.apiError(404, ApiErrorCode.NOT_FOUND, "Viaje no encontrado.")
            if (trip.driverId != viewerId) {
                throw store.apiError(403, ApiErrorCode.FORBIDDEN, "Solo el conductor puede aceptar solicitudes.")
            }
            val reservation = store.reservations[reservationId]?.takeIf { it.tripId == tripId }
                ?: throw store.apiError(404, ApiErrorCode.NOT_FOUND, "Reserva no encontrada.")

            if (reservation.status == FakeBackendStore.STATUS_ACCEPTED) {
                return@withState store.toReservationDto(reservation, viewerId)
            }
            store.requireReservationsChangeable(trip, "El viaje ya empezó: sus reservas no se pueden cambiar.")
            if (reservation.status != FakeBackendStore.STATUS_PENDING) {
                throw store.apiError(409, ApiErrorCode.INVALID_STATUS_TRANSITION, "La reserva no está pendiente.")
            }
            if (trip.availableSeats <= 0) {
                throw store.apiError(409, ApiErrorCode.NO_SEATS_AVAILABLE, "El viaje ya no tiene plazas disponibles.")
            }

            val now = store.nowIso()
            val remainingSeats = trip.availableSeats - 1
            store.trips[tripId] = trip.copy(
                availableSeats = remainingSeats,
                status = if (remainingSeats == 0) FakeBackendStore.STATUS_FULL else trip.status,
                updatedAt = now,
            )
            val updated = reservation.copy(status = FakeBackendStore.STATUS_ACCEPTED, updatedAt = now)
            store.reservations[reservationId] = updated
            store.toReservationDto(updated, viewerId)
        }
    }

    override suspend fun rejectReservation(tripId: String, reservationId: String): ReservationDto {
        maybeDelay()
        return store.withState {
            val viewerId = store.requireCurrentUser().user.id
            val trip = store.trips[tripId]
                ?: throw store.apiError(404, ApiErrorCode.NOT_FOUND, "Viaje no encontrado.")
            if (trip.driverId != viewerId) {
                throw store.apiError(403, ApiErrorCode.FORBIDDEN, "Solo el conductor puede rechazar solicitudes.")
            }
            val reservation = store.reservations[reservationId]?.takeIf { it.tripId == tripId }
                ?: throw store.apiError(404, ApiErrorCode.NOT_FOUND, "Reserva no encontrada.")

            if (reservation.status == FakeBackendStore.STATUS_REJECTED) {
                return@withState store.toReservationDto(reservation, viewerId)
            }
            store.requireReservationsChangeable(trip, "El viaje ya empezó: sus reservas no se pueden cambiar.")
            if (reservation.status != FakeBackendStore.STATUS_PENDING) {
                throw store.apiError(409, ApiErrorCode.INVALID_STATUS_TRANSITION, "La reserva no está pendiente.")
            }

            val updated = reservation.copy(
                status = FakeBackendStore.STATUS_REJECTED,
                updatedAt = store.nowIso(),
            )
            store.reservations[reservationId] = updated
            store.toReservationDto(updated, viewerId)
        }
    }

    override suspend fun cancelReservation(tripId: String, reservationId: String): ReservationDto {
        maybeDelay()
        return store.withState {
            val viewerId = store.requireCurrentUser().user.id
            val trip = store.trips[tripId]
                ?: throw store.apiError(404, ApiErrorCode.NOT_FOUND, "Viaje no encontrado.")
            val reservation = store.reservations[reservationId]?.takeIf { it.tripId == tripId }
                ?: throw store.apiError(404, ApiErrorCode.NOT_FOUND, "Reserva no encontrada.")
            if (reservation.passengerId != viewerId) {
                throw store.apiError(403, ApiErrorCode.FORBIDDEN, "Solo el pasajero puede cancelar su reserva.")
            }
            if (reservation.status == FakeBackendStore.STATUS_RESERVATION_CANCELLED) {
                return@withState store.toReservationDto(reservation, viewerId)
            }
            store.requireReservationsChangeable(trip, "No se puede cancelar un viaje que ya empezó.")
            if (reservation.status != FakeBackendStore.STATUS_PENDING &&
                reservation.status != FakeBackendStore.STATUS_ACCEPTED
            ) {
                throw store.apiError(409, ApiErrorCode.INVALID_STATUS_TRANSITION, "La reserva no se puede cancelar.")
            }

            val now = store.nowIso()
            if (reservation.status == FakeBackendStore.STATUS_ACCEPTED) {
                val restoredSeats = (trip.availableSeats + 1).coerceAtMost(trip.totalSeats)
                store.trips[tripId] = trip.copy(
                    availableSeats = restoredSeats,
                    status = if (trip.status == FakeBackendStore.STATUS_FULL) {
                        FakeBackendStore.STATUS_OPEN
                    } else {
                        trip.status
                    },
                    updatedAt = now,
                )
            }

            val updated = reservation.copy(
                status = FakeBackendStore.STATUS_RESERVATION_CANCELLED,
                updatedAt = now,
            )
            store.reservations[reservationId] = updated
            store.toReservationDto(updated, viewerId)
        }
    }

    /** docs/API.md §6.3: a finished or cancelled trip, or one that already started, does not change. */
    private fun FakeBackendStore.requireReservationsChangeable(trip: FakeBackendStore.StoredTrip, departedMessage: String) {
        if (trip.status == FakeBackendStore.STATUS_FINISHED || trip.status == FakeBackendStore.STATUS_CANCELLED) {
            throw apiError(409, ApiErrorCode.INVALID_STATUS_TRANSITION, "El viaje ya terminó o se canceló.")
        }
        if (!isUpcoming(trip.departureDate, trip.departureTime)) {
            throw apiError(409, ApiErrorCode.TRIP_ALREADY_DEPARTED, departedMessage)
        }
    }

    private suspend fun maybeDelay() {
        if (simulatedDelayMs > 0L) delay(simulatedDelayMs)
    }
}
