package com.example.uniride.data.remote.datasource

import com.example.uniride.data.remote.dto.ApiErrorCode
import com.example.uniride.data.remote.dto.CampusDto
import com.example.uniride.data.remote.dto.PublishTripRequest
import com.example.uniride.data.remote.dto.TripDto
import kotlinx.coroutines.delay

/**
 * In-memory fake of [TripRemoteDataSource] supporting filters and idempotent PUT (A2).
 */
class FakeTripRemoteDataSource(
    private val store: FakeBackendStore,
    private val simulatedDelayMs: Long = 0L,
) : TripRemoteDataSource {

    override suspend fun getTrips(
        campus: String?,
        direction: String?,
        date: String?,
        shift: String?,
        updatedSince: String?,
    ): List<TripDto> {
        maybeDelay()
        return store.withState {
            val viewerId = store.currentUserId
            store.trips.values
                .asSequence()
                .filter { trip ->
                    if (updatedSince != null) {
                        trip.updatedAt >= updatedSince
                    } else {
                        trip.status == FakeBackendStore.STATUS_OPEN ||
                            trip.status == FakeBackendStore.STATUS_FULL
                    }
                }
                .filter { campus == null || it.campus.equals(campus, ignoreCase = true) }
                .filter { direction == null || it.direction.equals(direction, ignoreCase = true) }
                .filter { date == null || it.departureDate == date }
                .filter { shift == null || store.matchesShift(it.departureTime, shift) }
                .sortedWith(compareBy({ it.departureDate }, { it.departureTime }))
                .take(MAX_TRIPS)
                .map { store.toPublicTripDto(it, viewerId) }
                .toList()
        }
    }

    override suspend fun getCampuses(): List<CampusDto> {
        maybeDelay()
        return store.withState { store.campuses }
    }

    override suspend fun getTripDetail(tripId: String): TripDto {
        maybeDelay()
        return store.withState {
            val trip = store.trips[tripId]
                ?: throw store.apiError(404, ApiErrorCode.NOT_FOUND, "Viaje no encontrado.")
            store.toPublicTripDto(trip, store.currentUserId)
        }
    }

    override suspend fun publishTrip(tripId: String, request: PublishTripRequest): TripDto {
        maybeDelay()
        return store.withState {
            val driver = store.requireCurrentUser().user
            val existing = store.trips[tripId]
            if (existing != null) {
                if (existing.driverId == driver.id) {
                    return@withState store.toPublicTripDto(existing, driver.id)
                }
                throw store.apiError(409, ApiErrorCode.ID_CONFLICT, "El identificador del viaje ya pertenece a otro usuario.")
            }

            val vehicle = driver.vehicle ?: throw store.apiError(
                httpCode = 400,
                code = ApiErrorCode.VALIDATION_ERROR,
                message = "Debés cargar un vehículo en tu perfil antes de publicar un viaje.",
            )
            if (request.totalSeats !in 1..4 || request.contributionPerSeat < 0) {
                throw store.apiError(400, ApiErrorCode.VALIDATION_ERROR, "Hay campos con errores.")
            }

            val created = FakeBackendStore.StoredTrip(
                tripId = tripId,
                driverId = driver.id,
                direction = request.direction,
                campus = request.campus,
                meetingPoint = request.meetingPoint,
                destinationNeighborhood = request.destinationNeighborhood,
                destinationReference = request.destinationReference,
                departureDate = request.departureDate,
                departureTime = request.departureTime,
                estimatedArrivalTime = request.estimatedArrivalTime,
                totalSeats = request.totalSeats,
                availableSeats = request.totalSeats,
                contributionPerSeat = request.contributionPerSeat,
                vehicleModel = vehicle.model,
                vehicleColor = vehicle.color,
                vehiclePlate = vehicle.plate,
                driverPhone = driver.phone,
                status = FakeBackendStore.STATUS_OPEN,
                updatedAt = store.nowIso(),
            )
            store.trips[tripId] = created
            store.toPublicTripDto(created, driver.id)
        }
    }

    override suspend fun getMyTrips(): List<TripDto> {
        maybeDelay()
        return store.withState {
            val viewerId = store.requireCurrentUser().user.id
            val reservedTripIds = store.reservations.values
                .filter { it.passengerId == viewerId }
                .map { it.tripId }
                .toSet()

            store.trips.values
                .filter { it.driverId == viewerId || it.tripId in reservedTripIds }
                .sortedWith(compareBy({ it.departureDate }, { it.departureTime }))
                .map { store.toPublicTripDto(it, viewerId) }
        }
    }

    override suspend fun finishTrip(tripId: String): TripDto {
        maybeDelay()
        return store.withState {
            val viewerId = store.requireCurrentUser().user.id
            val trip = store.trips[tripId]
                ?: throw store.apiError(404, ApiErrorCode.NOT_FOUND, "Viaje no encontrado.")
            if (trip.driverId != viewerId) {
                throw store.apiError(403, ApiErrorCode.FORBIDDEN, "Solo el conductor puede finalizar el viaje.")
            }
            if (trip.status == FakeBackendStore.STATUS_FINISHED) {
                return@withState store.toPublicTripDto(trip, viewerId)
            }
            if (trip.status == FakeBackendStore.STATUS_CANCELLED) {
                throw store.apiError(409, ApiErrorCode.INVALID_STATUS_TRANSITION, "No se puede finalizar un viaje cancelado.")
            }
            val updated = trip.copy(status = FakeBackendStore.STATUS_FINISHED, updatedAt = store.nowIso())
            store.trips[tripId] = updated
            store.toPublicTripDto(updated, viewerId)
        }
    }

    override suspend fun cancelTrip(tripId: String): TripDto {
        maybeDelay()
        return store.withState {
            val viewerId = store.requireCurrentUser().user.id
            val trip = store.trips[tripId]
                ?: throw store.apiError(404, ApiErrorCode.NOT_FOUND, "Viaje no encontrado.")
            if (trip.driverId != viewerId) {
                throw store.apiError(403, ApiErrorCode.FORBIDDEN, "Solo el conductor puede cancelar el viaje.")
            }
            if (trip.status == FakeBackendStore.STATUS_CANCELLED) {
                return@withState store.toPublicTripDto(trip, viewerId)
            }
            if (trip.status == FakeBackendStore.STATUS_FINISHED) {
                throw store.apiError(409, ApiErrorCode.INVALID_STATUS_TRANSITION, "No se puede cancelar un viaje finalizado.")
            }
            val now = store.nowIso()
            val updated = trip.copy(status = FakeBackendStore.STATUS_CANCELLED, updatedAt = now)
            store.trips[tripId] = updated

            store.reservations.values
                .filter {
                    it.tripId == tripId && (
                        it.status == FakeBackendStore.STATUS_PENDING ||
                            it.status == FakeBackendStore.STATUS_ACCEPTED
                        )
                }
                .forEach { res ->
                    store.reservations[res.reservationId] = res.copy(
                        status = FakeBackendStore.STATUS_RESERVATION_CANCELLED,
                        updatedAt = now,
                    )
                }

            store.toPublicTripDto(updated, viewerId)
        }
    }

    private suspend fun maybeDelay() {
        if (simulatedDelayMs > 0L) delay(simulatedDelayMs)
    }

    private companion object {
        const val MAX_TRIPS = 100
    }
}
