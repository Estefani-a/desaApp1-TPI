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
            val viewerId = store.requireCurrentUser().user.id
            val sinceInstant = updatedSince?.let {
                store.parseOffsetDateTime(it) ?: throw store.apiError(
                    httpCode = 400,
                    code = ApiErrorCode.VALIDATION_ERROR,
                    message = "Hay campos con errores.",
                    fields = mapOf("updatedSince" to "Formato ISO-8601 inválido."),
                )
            }

            store.trips.values
                .asSequence()
                .filter { trip ->
                    if (sinceInstant != null) {
                        val tripUpdatedAt = store.parseOffsetDateTime(trip.updatedAt)
                        tripUpdatedAt != null && !tripUpdatedAt.isBefore(sinceInstant)
                    } else {
                        (trip.status == FakeBackendStore.STATUS_OPEN ||
                            trip.status == FakeBackendStore.STATUS_FULL) &&
                            store.isUpcoming(trip.departureDate, trip.departureTime)
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
            val viewerId = store.requireCurrentUser().user.id
            val trip = store.trips[tripId]
                ?: throw store.apiError(404, ApiErrorCode.NOT_FOUND, "Viaje no encontrado.")
            store.toPublicTripDto(trip, viewerId)
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
                fields = mapOf("vehicle" to "Se requiere un vehículo cargado en el perfil."),
            )

            val fieldErrors = buildMap {
                if (request.totalSeats !in 1..4) {
                    put("totalSeats", "Debe estar entre 1 y 4.")
                }
                if (request.contributionPerSeat < 0) {
                    put("contributionPerSeat", "Debe ser mayor o igual a 0.")
                }
                if (!store.isUpcoming(request.departureDate, request.departureTime)) {
                    put("departureDate", "Debe ser una fecha y hora futura.")
                }
                val campus = store.campuses.firstOrNull { it.code.equals(request.campus, ignoreCase = true) }
                if (campus == null) {
                    put("campus", "La sede indicada no existe.")
                } else if (request.meetingPoint !in campus.meetingPoints) {
                    put("meetingPoint", "El punto de encuentro no corresponde a la sede elegida.")
                }
            }
            if (fieldErrors.isNotEmpty()) {
                throw store.apiError(
                    httpCode = 400,
                    code = ApiErrorCode.VALIDATION_ERROR,
                    message = "Hay campos con errores.",
                    fields = fieldErrors,
                )
            }

            val created = FakeBackendStore.StoredTrip(
                tripId = tripId,
                driverId = driver.id,
                direction = request.direction,
                campus = request.campus.uppercase(),
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
