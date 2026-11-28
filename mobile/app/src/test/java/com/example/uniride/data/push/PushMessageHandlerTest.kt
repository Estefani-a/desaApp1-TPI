package com.example.uniride.data.push

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.NotificationType
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.repository.LoginOutcome
import com.example.uniride.domain.repository.SessionRepository
import com.example.uniride.fake.FakeReservationRepository
import com.example.uniride.fake.FakeTripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PushMessageHandlerTest {

    private val tripId = TripId("0b7c4f0e-5d1a-4f5a-8a52-7c9e5e0d2b31")

    @Test
    fun `ignora payload invalido sin notificar ni refrescar`() = runTest {
        val notifier = RecordingPushNotifier()
        val trips = FakeTripRepository()
        val reservations = FakeReservationRepository()
        val handler = PushMessageHandler(
            sessionRepository = StubSessionRepository(loggedIn = true),
            tripRepository = trips,
            reservationRepository = reservations,
            notifier = notifier,
        )

        handler.handleMessage(mapOf("type" to "DESCONOCIDO", "tripId" to tripId.value))

        assertTrue(notifier.shown.isEmpty())
        assertTrue(trips.refreshedTripIds.isEmpty())
        assertTrue(reservations.calls.isEmpty())
    }

    @Test
    fun `sin sesion ignora la push sin notificar ni refrescar`() = runTest {
        val notifier = RecordingPushNotifier()
        val trips = FakeTripRepository()
        val reservations = FakeReservationRepository()
        val handler = PushMessageHandler(
            sessionRepository = StubSessionRepository(loggedIn = false),
            tripRepository = trips,
            reservationRepository = reservations,
            notifier = notifier,
        )

        handler.handleMessage(
            mapOf(
                PushPayload.KEY_TYPE to NotificationType.SOLICITUD_ACEPTADA.name,
                PushPayload.KEY_TRIP_ID to tripId.value,
                PushPayload.KEY_TITLE to "¡Tenés lugar!",
                PushPayload.KEY_BODY to "Lucía F. aceptó tu solicitud.",
            ),
        )

        assertTrue(notifier.shown.isEmpty())
        assertTrue(trips.refreshedTripIds.isEmpty())
        assertTrue(reservations.calls.isEmpty())
    }

    @Test
    fun `nueva solicitud muestra notificacion y refresca solicitudes y viaje`() = runTest {
        val notifier = RecordingPushNotifier()
        val trips = FakeTripRepository()
        val reservations = FakeReservationRepository()
        val handler = PushMessageHandler(
            sessionRepository = StubSessionRepository(loggedIn = true),
            tripRepository = trips,
            reservationRepository = reservations,
            notifier = notifier,
        )

        handler.handleMessage(
            mapOf(
                PushPayload.KEY_TYPE to NotificationType.NUEVA_SOLICITUD.name,
                PushPayload.KEY_TRIP_ID to tripId.value,
                PushPayload.KEY_TITLE to "Nueva solicitud de asiento",
                PushPayload.KEY_BODY to "Martín G. pidió un lugar.",
            ),
        )

        assertEquals(1, notifier.shown.size)
        assertEquals(NotificationType.NUEVA_SOLICITUD, notifier.shown.single().type)
        assertEquals(listOf("refresh:${tripId.value}"), reservations.calls)
        assertEquals(listOf(tripId), trips.refreshedTripIds)
    }

    @Test
    fun `solicitud aceptada muestra notificacion y refresca solo el viaje`() = runTest {
        val notifier = RecordingPushNotifier()
        val trips = FakeTripRepository()
        val reservations = FakeReservationRepository()
        val handler = PushMessageHandler(
            sessionRepository = StubSessionRepository(loggedIn = true),
            tripRepository = trips,
            reservationRepository = reservations,
            notifier = notifier,
        )

        handler.handleMessage(
            mapOf(
                PushPayload.KEY_TYPE to NotificationType.SOLICITUD_ACEPTADA.name,
                PushPayload.KEY_TRIP_ID to tripId.value,
            ),
        )

        assertEquals(1, notifier.shown.size)
        assertEquals(listOf(tripId), trips.refreshedTripIds)
        assertTrue(reservations.calls.isEmpty())
    }

    @Test
    fun `solicitud rechazada muestra notificacion aunque falle el refresh por red`() = runTest {
        val notifier = RecordingPushNotifier()
        val trips = FakeTripRepository().apply {
            refreshTripResult = AppResult.Failure(DataError.NO_CONNECTION)
        }
        val reservations = FakeReservationRepository()
        val handler = PushMessageHandler(
            sessionRepository = StubSessionRepository(loggedIn = true),
            tripRepository = trips,
            reservationRepository = reservations,
            notifier = notifier,
        )

        handler.handleMessage(
            mapOf(
                PushPayload.KEY_TYPE to NotificationType.SOLICITUD_RECHAZADA.name,
                PushPayload.KEY_TRIP_ID to tripId.value,
            ),
        )

        assertEquals(1, notifier.shown.size)
        assertEquals(listOf(tripId), trips.refreshedTripIds)
    }

    private class RecordingPushNotifier : PushNotifier {
        val shown = mutableListOf<PushPayload>()
        override fun ensureChannel() = Unit
        override fun show(payload: PushPayload) {
            shown += payload
        }
    }

    private class StubSessionRepository(loggedIn: Boolean) : SessionRepository {
        private val state = MutableStateFlow(loggedIn)
        override fun observeIsLoggedIn(): Flow<Boolean> = state
        override suspend fun login(email: String, password: String): LoginOutcome =
            error("No se usa en este test")
        override suspend fun logout() = Unit
    }
}
