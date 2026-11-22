package com.example.uniride.data.remote.datasource

import com.example.uniride.data.remote.dto.ApiErrorCode
import com.example.uniride.data.remote.dto.ApiErrorDto
import com.example.uniride.data.remote.dto.CampusDto
import com.example.uniride.data.remote.dto.ReservationDto
import com.example.uniride.data.remote.dto.TripDto
import com.example.uniride.data.remote.dto.UserDto
import com.example.uniride.data.remote.dto.VehicleDto
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * Shared in-memory backend fixture used by all Fake*RemoteDataSource implementations (A2).
 * Enables working on screens and demos without a running server while preserving contract rules.
 */
class FakeBackendStore {

    internal data class StoredUser(
        val user: UserDto,
        val password: String,
        val isActivated: Boolean,
        val fcmToken: String? = null,
    )

    internal data class StoredTrip(
        val tripId: String,
        val driverId: String,
        val direction: String,
        val campus: String,
        val meetingPoint: String,
        val destinationNeighborhood: String,
        val destinationReference: String?,
        val departureDate: String,
        val departureTime: String,
        val estimatedArrivalTime: String?,
        val totalSeats: Int,
        val availableSeats: Int,
        val contributionPerSeat: Int,
        val vehicleModel: String,
        val vehicleColor: String,
        val vehiclePlate: String,
        val driverPhone: String,
        val status: String,
        val updatedAt: String,
    )

    internal data class StoredReservation(
        val reservationId: String,
        val tripId: String,
        val passengerId: String,
        val status: String,
        val reservedAt: String,
        val updatedAt: String,
    )

    private val mutex = Mutex()
    private val zone = ZoneId.of("America/Argentina/Buenos_Aires")

    internal val campuses: List<CampusDto> = listOf(
        CampusDto(
            code = "MONSERRAT",
            name = "Sede Monserrat",
            meetingPoints = listOf(
                "Salida Lima 717",
                "Salida Lima 757",
                "Salida Chile 1142",
                "Salida Independencia 1072",
            ),
        ),
        CampusDto(
            code = "BELGRANO",
            name = "Sede Belgrano",
            meetingPoints = listOf(
                "Salida 11 de Septiembre 1990",
                "Esquina Echeverría",
            ),
        ),
        CampusDto(
            code = "RECOLETA",
            name = "Sede Recoleta",
            meetingPoints = listOf("Salida Libertad 1340"),
        ),
    )

    internal val users = mutableMapOf<String, StoredUser>()
    internal val activationTokens = mutableMapOf<String, String>()
    internal val passwordResetTokens = mutableMapOf<String, String>()
    internal val trips = mutableMapOf<String, StoredTrip>()
    internal val reservations = mutableMapOf<String, StoredReservation>()

    internal var currentUserId: String = DEFAULT_PASSENGER_ID

    init {
        seedDemoData()
    }

    internal suspend fun <T> withState(block: () -> T): T = mutex.withLock { block() }

    internal fun nowIso(): String = OffsetDateTime.now(zone).withNano(0).toString()

    internal fun expiresIn30DaysIso(): String =
        OffsetDateTime.now(zone).plusDays(30).withNano(0).toString()

    internal fun requireCurrentUser(): StoredUser =
        users[currentUserId] ?: throw apiError(
            httpCode = 401,
            code = ApiErrorCode.UNAUTHENTICATED,
            message = "Sesión no iniciada.",
        )

    internal fun toPublicTripDto(trip: StoredTrip, viewerId: String): TripDto {
        val driver = users[trip.driverId]?.user
        val viewerReservation = reservations.values.firstOrNull {
            it.tripId == trip.tripId && it.passengerId == viewerId
        }
        val canSeePrivateDetails = viewerId == trip.driverId ||
            viewerReservation?.status == STATUS_ACCEPTED

        return TripDto(
            tripId = trip.tripId,
            driverId = trip.driverId,
            driverName = formatPublicName(driver?.fullName ?: "Conductor"),
            driverRating = driver?.ratingAvg,
            direction = trip.direction,
            campus = trip.campus,
            meetingPoint = trip.meetingPoint,
            destinationNeighborhood = trip.destinationNeighborhood,
            destinationReference = trip.destinationReference,
            departureDate = trip.departureDate,
            departureTime = trip.departureTime,
            estimatedArrivalTime = trip.estimatedArrivalTime,
            totalSeats = trip.totalSeats,
            availableSeats = trip.availableSeats,
            contributionPerSeat = trip.contributionPerSeat,
            vehicleModel = trip.vehicleModel,
            vehicleColor = trip.vehicleColor,
            vehiclePlate = if (canSeePrivateDetails) trip.vehiclePlate else null,
            driverPhone = if (canSeePrivateDetails) trip.driverPhone else null,
            status = trip.status,
            myReservation = viewerReservation?.let { toReservationDto(it, viewerId) },
            updatedAt = trip.updatedAt,
        )
    }

    internal fun toReservationDto(reservation: StoredReservation, viewerId: String): ReservationDto {
        val passenger = users[reservation.passengerId]?.user
        val trip = trips[reservation.tripId]
        val canSeePassengerPhone = trip?.driverId == viewerId &&
            reservation.status == STATUS_ACCEPTED

        return ReservationDto(
            reservationId = reservation.reservationId,
            tripId = reservation.tripId,
            passengerId = reservation.passengerId,
            passengerName = formatPublicName(passenger?.fullName ?: "Pasajero"),
            passengerRating = passenger?.ratingAvg,
            passengerPhone = if (canSeePassengerPhone) passenger?.phone else null,
            status = reservation.status,
            reservedAt = reservation.reservedAt,
            updatedAt = reservation.updatedAt,
        )
    }

    internal fun matchesShift(departureTime: String, shift: String): Boolean {
        val time = runCatching { LocalTime.parse(departureTime) }.getOrNull() ?: return false
        return when (shift.uppercase()) {
            "MANANA" -> !time.isBefore(LocalTime.of(6, 0)) && time.isBefore(LocalTime.of(13, 0))
            "TARDE" -> !time.isBefore(LocalTime.of(13, 0)) && time.isBefore(LocalTime.of(18, 0))
            "NOCHE" -> !time.isBefore(LocalTime.of(18, 0)) || time.isBefore(LocalTime.of(6, 0))
            else -> false
        }
    }

    internal fun apiError(
        httpCode: Int,
        code: ApiErrorCode,
        message: String,
        fields: Map<String, String>? = null,
    ): FakeApiException = FakeApiException(
        httpCode = httpCode,
        apiError = ApiErrorDto(code = code.name, message = message, fields = fields),
    )

    private fun formatPublicName(fullName: String): String {
        val parts = fullName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            parts.isEmpty() -> fullName
            parts.size == 1 -> parts.first()
            else -> "${parts.first()} ${parts.last().first().uppercaseChar()}."
        }
    }

    private fun seedDemoData() {
        val lucia = UserDto(
            id = DEFAULT_DRIVER_ID,
            fullName = "Lucía Fernández",
            email = "lfernandez@uade.edu.ar",
            career = "Ingeniería en Informática",
            phone = "+5491155550001",
            preferredMode = "AMBOS",
            ratingAvg = 4.8,
            ratingCount = 12,
            vehicle = VehicleDto(
                brand = "Volkswagen",
                model = "Gol Trend",
                color = "Gris",
                plate = "AB123CD",
            ),
        )
        val martin = UserDto(
            id = DEFAULT_PASSENGER_ID,
            fullName = "Martín Gómez",
            email = "mgomez@uade.edu.ar",
            career = "Licenciatura en Gestión de Tecnología",
            phone = "+5491155550002",
            preferredMode = "PASAJERO",
            ratingAvg = 4.6,
            ratingCount = 7,
            vehicle = null,
        )
        val sofia = UserDto(
            id = SECOND_DRIVER_ID,
            fullName = "Sofía Pérez",
            email = "sperez@uade.edu.ar",
            career = "Contador Público",
            phone = "+5491155550003",
            preferredMode = "CONDUCTOR",
            ratingAvg = 4.9,
            ratingCount = 19,
            vehicle = VehicleDto(
                brand = "Toyota",
                model = "Yaris",
                color = "Blanco",
                plate = "AF456GH",
            ),
        )

        users[lucia.id] = StoredUser(lucia, DEMO_PASSWORD, isActivated = true)
        users[martin.id] = StoredUser(martin, DEMO_PASSWORD, isActivated = true)
        users[sofia.id] = StoredUser(sofia, DEMO_PASSWORD, isActivated = true)

        val now = nowIso()
        trips[DEMO_TRIP_MONSERRAT_ID] = StoredTrip(
            tripId = DEMO_TRIP_MONSERRAT_ID,
            driverId = lucia.id,
            direction = "VUELTA",
            campus = "MONSERRAT",
            meetingPoint = "Salida Lima 717",
            destinationNeighborhood = "Ramos Mejía",
            destinationReference = "Estación de tren",
            departureDate = "2026-11-25",
            departureTime = "22:15",
            estimatedArrivalTime = "23:00",
            totalSeats = 3,
            availableSeats = 2,
            contributionPerSeat = 3500,
            vehicleModel = "Gol Trend",
            vehicleColor = "Gris",
            vehiclePlate = "AB123CD",
            driverPhone = lucia.phone,
            status = STATUS_OPEN,
            updatedAt = now,
        )
        trips[DEMO_TRIP_BELGRANO_ID] = StoredTrip(
            tripId = DEMO_TRIP_BELGRANO_ID,
            driverId = sofia.id,
            direction = "IDA",
            campus = "BELGRANO",
            meetingPoint = "Salida 11 de Septiembre 1990",
            destinationNeighborhood = "Palermo",
            destinationReference = "Plaza Italia",
            departureDate = "2026-11-26",
            departureTime = "08:00",
            estimatedArrivalTime = "08:30",
            totalSeats = 2,
            availableSeats = 1,
            contributionPerSeat = 2500,
            vehicleModel = "Yaris",
            vehicleColor = "Blanco",
            vehiclePlate = "AF456GH",
            driverPhone = sofia.phone,
            status = STATUS_OPEN,
            updatedAt = now,
        )
        trips[DEMO_TRIP_RECOLETA_ID] = StoredTrip(
            tripId = DEMO_TRIP_RECOLETA_ID,
            driverId = lucia.id,
            direction = "VUELTA",
            campus = "RECOLETA",
            meetingPoint = "Salida Libertad 1340",
            destinationNeighborhood = "Caballito",
            destinationReference = "Parque Rivadavia",
            departureDate = "2026-11-26",
            departureTime = "15:30",
            estimatedArrivalTime = "16:10",
            totalSeats = 2,
            availableSeats = 2,
            contributionPerSeat = 2000,
            vehicleModel = "Gol Trend",
            vehicleColor = "Gris",
            vehiclePlate = "AB123CD",
            driverPhone = lucia.phone,
            status = STATUS_OPEN,
            updatedAt = now,
        )
    }

    companion object {
        const val DEMO_PASSWORD = "Secreta123"
        const val DEFAULT_DRIVER_ID = "6f1c2a4e-1b0e-4a8e-9a55-2c1f3e7b9d10"
        const val DEFAULT_PASSENGER_ID = "9a2e5c7b-3d8f-4b1a-a6e0-1c4f7d9b2e58"
        const val SECOND_DRIVER_ID = "4d8b1a3c-7e2f-4c9a-b5d1-8a0e3f6c2b41"

        const val DEMO_TRIP_MONSERRAT_ID = "0b7c4f0e-5d1a-4f5a-8a52-7c9e5e0d2b31"
        const val DEMO_TRIP_BELGRANO_ID = "1a9d3e2f-4c8b-4d6e-9f01-2b3c4d5e6f70"
        const val DEMO_TRIP_RECOLETA_ID = "2b8e4f1a-5d9c-4e7f-8a12-3c4d5e6f7081"

        internal const val STATUS_OPEN = "ABIERTO"
        internal const val STATUS_FULL = "COMPLETO"
        internal const val STATUS_FINISHED = "FINALIZADO"
        internal const val STATUS_CANCELLED = "CANCELADO"

        internal const val STATUS_PENDING = "PENDIENTE"
        internal const val STATUS_ACCEPTED = "ACEPTADA"
        internal const val STATUS_REJECTED = "RECHAZADA"
        internal const val STATUS_RESERVATION_CANCELLED = "CANCELADA"
    }
}
