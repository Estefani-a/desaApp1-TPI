package com.example.uniride.data.remote.datasource

import com.example.uniride.data.remote.ApiException
import com.example.uniride.data.remote.dto.ActivateRequest
import com.example.uniride.data.remote.dto.ApiErrorCode
import com.example.uniride.data.remote.dto.LoginRequest
import com.example.uniride.data.remote.dto.PublishTripRequest
import com.example.uniride.data.remote.dto.RegisterRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FakeRemoteDataSourcesTest {

    private val zone = ZoneId.of("America/Argentina/Buenos_Aires")

    @Test
    fun `sin sesion iniciada los endpoints protegidos devuelven 401 UNAUTHENTICATED`() = runBlocking {
        val store = FakeBackendStore()
        val trips = FakeTripRemoteDataSource(store)

        try {
            trips.getTrips()
            fail("Se esperaba ApiException 401")
        } catch (ex: ApiException) {
            assertEquals(401, ex.httpCode)
            assertEquals(ApiErrorCode.UNAUTHENTICATED, ex.apiError.errorCode)
        }

        // GET /campuses es público y responde sin sesión
        assertEquals(3, trips.getCampuses().size)
    }

    @Test
    fun `registro valida contrasena debil y activacion inicia sesion automaticamente`() = runBlocking {
        val store = FakeBackendStore()
        val auth = FakeAuthRemoteDataSource(store)

        try {
            auth.register(
                RegisterRequest(
                    email = "nuevo@uade.edu.ar",
                    password = "debil",
                    fullName = "Juan Perez",
                    career = "Ingeniería en Informática",
                    phone = "+5491144445555",
                    preferredMode = "PASAJERO",
                ),
            )
            fail("Se esperaba WEAK_PASSWORD")
        } catch (ex: ApiException) {
            assertEquals(400, ex.httpCode)
            assertEquals(ApiErrorCode.WEAK_PASSWORD, ex.apiError.errorCode)
        }

        auth.register(
            RegisterRequest(
                email = "nuevo@uade.edu.ar",
                password = "Password1",
                fullName = "Juan Perez",
                career = "Ingeniería en Informática",
                phone = "+5491144445555",
                preferredMode = "PASAJERO",
            ),
        )

        val response = auth.activate(ActivateRequest(token = "fake-activate-nuevo@uade.edu.ar"))

        assertEquals("nuevo@uade.edu.ar", response.user.email)
        assertTrue(response.accessToken.isNotBlank())
        assertEquals("nuevo@uade.edu.ar", auth.getProfile().email)
    }

    @Test
    fun `login con credenciales invalidas devuelve 401 INVALID_CREDENTIALS`() = runBlocking {
        val store = FakeBackendStore()
        val auth = FakeAuthRemoteDataSource(store)

        try {
            auth.login(LoginRequest("lfernandez@uade.edu.ar", "ClaveIncorrecta9"))
            fail("Se esperaba ApiException")
        } catch (ex: ApiException) {
            assertEquals(401, ex.httpCode)
            assertEquals(ApiErrorCode.INVALID_CREDENTIALS, ex.apiError.errorCode)
        }
    }

    @Test
    fun `listado de viajes filtra por sede y turno y oculta patente y telefono sin reserva aceptada`() = runBlocking {
        val store = FakeBackendStore()
        val auth = FakeAuthRemoteDataSource(store)
        val trips = FakeTripRemoteDataSource(store)

        auth.login(LoginRequest("mgomez@uade.edu.ar", FakeBackendStore.DEMO_PASSWORD))
        val nightMonserratTrips = trips.getTrips(campus = "MONSERRAT", shift = "NOCHE")

        assertEquals(1, nightMonserratTrips.size)
        val trip = nightMonserratTrips.first()
        assertEquals(FakeBackendStore.DEMO_TRIP_MONSERRAT_ID, trip.tripId)
        assertNull(trip.vehiclePlate)
        assertNull(trip.driverPhone)
        assertTrue(trip.updatedAt.matches(Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}$")))
    }

    @Test
    fun `publicar viaje valida fecha futura y punto de encuentro de la sede con fields`() = runBlocking {
        val store = FakeBackendStore()
        val auth = FakeAuthRemoteDataSource(store)
        val trips = FakeTripRemoteDataSource(store)

        auth.login(LoginRequest("lfernandez@uade.edu.ar", FakeBackendStore.DEMO_PASSWORD))
        val invalidRequest = PublishTripRequest(
            direction = "VUELTA",
            campus = "MONSERRAT",
            meetingPoint = "Salida Inventada 999",
            destinationNeighborhood = "Belgrano",
            destinationReference = null,
            departureDate = LocalDate.now(zone).minusDays(1).toString(),
            departureTime = "10:00",
            estimatedArrivalTime = null,
            totalSeats = 3,
            contributionPerSeat = 3000,
        )

        try {
            trips.publishTrip("trip-invalid-1", invalidRequest)
            fail("Se esperaba VALIDATION_ERROR")
        } catch (ex: ApiException) {
            assertEquals(400, ex.httpCode)
            assertEquals(ApiErrorCode.VALIDATION_ERROR, ex.apiError.errorCode)
            assertNotNull(ex.apiError.fields?.get("departureDate"))
            assertNotNull(ex.apiError.fields?.get("meetingPoint"))
        }
    }

    @Test
    fun `publicar viaje es idempotente cuando el mismo conductor reintenta el mismo UUID`() = runBlocking {
        val store = FakeBackendStore()
        val auth = FakeAuthRemoteDataSource(store)
        val trips = FakeTripRemoteDataSource(store)

        auth.login(LoginRequest("lfernandez@uade.edu.ar", FakeBackendStore.DEMO_PASSWORD))
        val request = PublishTripRequest(
            direction = "VUELTA",
            campus = "MONSERRAT",
            meetingPoint = "Salida Chile 1142",
            destinationNeighborhood = "Belgrano",
            destinationReference = "Barrancas",
            departureDate = LocalDate.now(zone).plusDays(3).toString(),
            departureTime = "21:30",
            estimatedArrivalTime = "22:05",
            totalSeats = 3,
            contributionPerSeat = 3000,
        )

        val first = trips.publishTrip("trip-uuid-1", request)
        val retry = trips.publishTrip("trip-uuid-1", request)

        assertEquals(first.tripId, retry.tripId)
        assertEquals(3, retry.availableSeats)
    }

    @Test
    fun `aceptar reserva descuenta vacante y habilita patente y telefono para el pasajero`() = runBlocking {
        val store = FakeBackendStore()
        val auth = FakeAuthRemoteDataSource(store)
        val trips = FakeTripRemoteDataSource(store)
        val reservations = FakeReservationRemoteDataSource(store)
        val tripId = FakeBackendStore.DEMO_TRIP_BELGRANO_ID

        auth.login(LoginRequest("mgomez@uade.edu.ar", FakeBackendStore.DEMO_PASSWORD))
        val pending = reservations.requestSeat(tripId = tripId, reservationId = "res-uuid-1")
        assertEquals("PENDIENTE", pending.status)

        auth.login(LoginRequest("sperez@uade.edu.ar", FakeBackendStore.DEMO_PASSWORD))
        val accepted = reservations.acceptReservation(tripId = tripId, reservationId = "res-uuid-1")
        assertEquals("ACEPTADA", accepted.status)
        assertNotNull(accepted.passengerPhone)

        auth.login(LoginRequest("mgomez@uade.edu.ar", FakeBackendStore.DEMO_PASSWORD))
        val detail = trips.getTripDetail(tripId)
        assertEquals("COMPLETO", detail.status)
        assertEquals(0, detail.availableSeats)
        assertNotNull(detail.vehiclePlate)
        assertNotNull(detail.driverPhone)
    }

    @Test
    fun `no permite aceptar una reserva si el viaje fue finalizado`() = runBlocking {
        val store = FakeBackendStore()
        val auth = FakeAuthRemoteDataSource(store)
        val trips = FakeTripRemoteDataSource(store)
        val reservations = FakeReservationRemoteDataSource(store)
        val tripId = FakeBackendStore.DEMO_TRIP_MONSERRAT_ID

        auth.login(LoginRequest("mgomez@uade.edu.ar", FakeBackendStore.DEMO_PASSWORD))
        reservations.requestSeat(tripId = tripId, reservationId = "res-finished-1")

        auth.login(LoginRequest("lfernandez@uade.edu.ar", FakeBackendStore.DEMO_PASSWORD))
        trips.finishTrip(tripId)

        try {
            reservations.acceptReservation(tripId = tripId, reservationId = "res-finished-1")
            fail("Se esperaba TRIP_NOT_OPEN")
        } catch (ex: ApiException) {
            assertEquals(409, ex.httpCode)
            assertEquals(ApiErrorCode.TRIP_NOT_OPEN, ex.apiError.errorCode)
        }
    }

    @Test
    fun `cancelar reserva aceptada devuelve la vacante y reabre el viaje`() = runBlocking {
        val store = FakeBackendStore()
        val auth = FakeAuthRemoteDataSource(store)
        val trips = FakeTripRemoteDataSource(store)
        val reservations = FakeReservationRemoteDataSource(store)
        val tripId = FakeBackendStore.DEMO_TRIP_BELGRANO_ID

        auth.login(LoginRequest("mgomez@uade.edu.ar", FakeBackendStore.DEMO_PASSWORD))
        reservations.requestSeat(tripId = tripId, reservationId = "res-uuid-2")

        auth.login(LoginRequest("sperez@uade.edu.ar", FakeBackendStore.DEMO_PASSWORD))
        reservations.acceptReservation(tripId = tripId, reservationId = "res-uuid-2")

        auth.login(LoginRequest("mgomez@uade.edu.ar", FakeBackendStore.DEMO_PASSWORD))
        val cancelled = reservations.cancelReservation(tripId = tripId, reservationId = "res-uuid-2")
        val detail = trips.getTripDetail(tripId)

        assertEquals("CANCELADA", cancelled.status)
        assertEquals("ABIERTO", detail.status)
        assertEquals(1, detail.availableSeats)
    }

    @Test
    fun `viaje de madrugada pertenece al turno NOCHE y no a MANANA`() {
        val store = FakeBackendStore()

        assertTrue(store.matchesShift("00:30", "NOCHE"))
        assertTrue(!store.matchesShift("00:30", "MANANA"))
        assertTrue(store.matchesShift("06:00", "MANANA"))
        assertTrue(store.matchesShift("13:00", "TARDE"))
        assertTrue(store.matchesShift("18:00", "NOCHE"))
    }
}
