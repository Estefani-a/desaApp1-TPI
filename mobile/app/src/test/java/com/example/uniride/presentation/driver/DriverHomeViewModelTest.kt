package com.example.uniride.presentation.driver

import app.cash.turbine.test
import com.example.uniride.R
import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.Campus
import com.example.uniride.domain.model.CampusCode
import com.example.uniride.domain.model.DomainFixtures.trip
import com.example.uniride.domain.model.RejectedTrip
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.model.TripStatus
import com.example.uniride.domain.usecase.campus.ObserveCampusesUseCase
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.domain.usecase.trip.DismissTripRejectionUseCase
import com.example.uniride.domain.usecase.trip.ObservePendingTripIdsUseCase
import com.example.uniride.domain.usecase.trip.ObserveRejectedTripsUseCase
import com.example.uniride.domain.usecase.trip.ObserveUpcomingDriverTripsUseCase
import com.example.uniride.domain.usecase.trip.RefreshMyTripsUseCase
import com.example.uniride.fake.FakeCampusRepository
import com.example.uniride.fake.FakeNetworkMonitor
import com.example.uniride.fake.FakeTripRepository
import com.example.uniride.fake.MainDispatcherRule
import com.example.uniride.presentation.common.ContentState
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** C1 Inicio del conductor (RF02) with the offline scenarios of 12.1. "Now" is 2026-11-20 18:00. */
class DriverHomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-11-20T21:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"))

    private val tonight = trip().copy(id = TripId("tonight"))
    private val tomorrow = trip(date = LocalDate.of(2026, 11, 21), time = LocalTime.of(8, 0)).copy(id = TripId("tomorrow"))

    private val trips = FakeTripRepository()
    private val campuses = FakeCampusRepository().apply {
        campuses.value = listOf(Campus(CampusCode("MONSERRAT"), "Sede Monserrat", emptyList()))
    }
    private val network = FakeNetworkMonitor(initialOnline = true)

    private fun TestScope.viewModel(): DriverHomeViewModel {
        val viewModel = DriverHomeViewModel(
            observeUpcomingTrips = ObserveUpcomingDriverTripsUseCase(trips, clock),
            observeRejectedTrips = ObserveRejectedTripsUseCase(trips),
            observePendingTripIds = ObservePendingTripIdsUseCase(trips),
            observeCampuses = ObserveCampusesUseCase(campuses),
            observeConnectivity = ObserveConnectivityUseCase(network),
            refreshMyTrips = RefreshMyTripsUseCase(trips),
            dismissTripRejection = DismissTripRejectionUseCase(trips),
        )
        // uiState is WhileSubscribed: someone has to collect it, like the screen does
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun DriverHomeViewModel.content(): DriverHomeContent =
        (uiState.value.content as ContentState.Success).data

    private fun givenSavedTrips(vararg saved: Trip) {
        trips.publishedTrips.value = saved.toList()
    }

    @Test
    fun `muestra los proximos viajes con el pendiente de sincronizar marcado`() = runTest(mainDispatcherRule.testDispatcher) {
        givenSavedTrips(tomorrow, tonight)
        trips.pendingTripIds.value = setOf(TripId("tomorrow"))

        val content = viewModel().content()

        assertEquals(listOf(tonight, tomorrow), content.trips)
        assertEquals(setOf(TripId("tomorrow")), content.pendingTripIds)
        assertEquals("Sede Monserrat", content.campusNames[CampusCode("MONSERRAT")])
    }

    @Test
    fun `un viaje publicado sin conexion aparece enseguida como pendiente y no bloquea la pantalla`() =
        runTest(mainDispatcherRule.testDispatcher) {
            network.setOnline(false)
            trips.refreshMyTripsResult = AppResult.Failure(DataError.NO_CONNECTION)
            val viewModel = viewModel()

            givenSavedTrips(tonight)
            trips.pendingTripIds.value = setOf(TripId("tonight"))

            assertTrue(viewModel.uiState.value.isOffline)
            assertEquals(listOf(tonight), viewModel.content().trips)
            assertEquals(setOf(TripId("tonight")), viewModel.content().pendingTripIds)
        }

    @Test
    fun `no muestra los finalizados, los cancelados ni los que ya salieron`() = runTest(mainDispatcherRule.testDispatcher) {
        givenSavedTrips(
            tonight,
            tomorrow.copy(id = TripId("finished"), status = TripStatus.FINALIZADO),
            tomorrow.copy(id = TripId("cancelled"), status = TripStatus.CANCELADO),
            trip(time = LocalTime.of(9, 0)).copy(id = TripId("departed")),
        )

        assertEquals(listOf(tonight), viewModel().content().trips)
    }

    @Test
    fun `un viaje completo sigue en la lista`() = runTest(mainDispatcherRule.testDispatcher) {
        val full = tomorrow.copy(status = TripStatus.COMPLETO, availableSeats = 0)
        givenSavedTrips(full)

        assertEquals(listOf(full), viewModel().content().trips)
    }

    @Test
    fun `un rechazo se muestra con su motivo y el viaje guardado`() = runTest(mainDispatcherRule.testDispatcher) {
        val rejected = tomorrow.copy(id = TripId("rejected"), status = TripStatus.CANCELADO)
        givenSavedTrips(tonight, rejected)
        trips.rejectedTrips.value = mapOf(TripId("rejected") to DataError.VALIDATION)

        val content = viewModel().content()

        assertEquals(listOf(RejectedTrip(TripId("rejected"), rejected, DataError.VALIDATION)), content.rejections)
        assertEquals(listOf(tonight), content.trips)
    }

    @Test
    fun `descartar un rechazo llama a dismissRejection y lo saca de la pantalla`() = runTest(mainDispatcherRule.testDispatcher) {
        givenSavedTrips(tonight)
        trips.rejectedTrips.value = mapOf(TripId("rejected") to DataError.VALIDATION)
        val viewModel = viewModel()

        viewModel.onAction(DriverHomeAction.DismissRejection("rejected"))

        assertEquals(listOf(TripId("rejected")), trips.dismissedRejections)
        assertTrue(viewModel.content().rejections.isEmpty())
    }

    @Test
    fun `sin viajes ni rechazos esta vacio`() = runTest(mainDispatcherRule.testDispatcher) {
        val state = viewModel().uiState.value

        assertEquals(ContentState.Empty, state.content)
        assertFalse(state.isOffline)
    }

    @Test
    fun `con solo un rechazo no esta vacio`() = runTest(mainDispatcherRule.testDispatcher) {
        trips.rejectedTrips.value = mapOf(TripId("rejected") to DataError.VALIDATION)

        assertTrue(viewModel().uiState.value.content is ContentState.Success)
    }

    @Test
    fun `al abrir refresca mis viajes`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel()

        assertEquals(1, trips.refreshMyTripsCalls)
    }

    @Test
    fun `al volver la conexion refresca de nuevo`() = runTest(mainDispatcherRule.testDispatcher) {
        network.setOnline(false)
        viewModel()
        assertEquals(1, trips.refreshMyTripsCalls)

        network.setOnline(true)

        assertEquals(2, trips.refreshMyTripsCalls)
    }

    @Test
    fun `un refresco fallido con viajes guardados no borra la lista y avisa con un mensaje`() =
        runTest(mainDispatcherRule.testDispatcher) {
            givenSavedTrips(tonight, tomorrow)
            trips.refreshMyTripsResult = AppResult.Failure(DataError.SERVER)
            val viewModel = viewModel()

            viewModel.effects.test {
                viewModel.onAction(DriverHomeAction.Refresh)
                assertEquals(DriverHomeEffect.ShowMessage(R.string.common_error_server), awaitItem())
            }
            assertEquals(listOf(tonight, tomorrow), viewModel.content().trips)
        }

    @Test
    fun `sin conexion y con viajes guardados no hay mensaje, alcanza con el banner`() =
        runTest(mainDispatcherRule.testDispatcher) {
            givenSavedTrips(tonight)
            network.setOnline(false)
            trips.refreshMyTripsResult = AppResult.Failure(DataError.NO_CONNECTION)
            val viewModel = viewModel()

            viewModel.effects.test {
                viewModel.onAction(DriverHomeAction.Refresh)
                expectNoEvents()
            }
            assertEquals(listOf(tonight), viewModel.content().trips)
            assertTrue(viewModel.uiState.value.isOffline)
        }

    @Test
    fun `sin viajes y con un error reintentable muestra el error con reintento`() = runTest(mainDispatcherRule.testDispatcher) {
        trips.refreshMyTripsResult = AppResult.Failure(DataError.SERVER)

        assertEquals(ContentState.Error(R.string.common_error_server), viewModel().uiState.value.content)
    }

    @Test
    fun `sin viajes ni conexion tambien muestra el error con reintento`() = runTest(mainDispatcherRule.testDispatcher) {
        network.setOnline(false)
        trips.refreshMyTripsResult = AppResult.Failure(DataError.NO_CONNECTION)

        val state = viewModel().uiState.value

        assertEquals(ContentState.Error(R.string.common_error_no_connection), state.content)
        assertTrue(state.isOffline)
    }

    @Test
    fun `sin sesion (401) no muestra error y queda vacio`() = runTest(mainDispatcherRule.testDispatcher) {
        trips.refreshMyTripsResult = AppResult.Failure(DataError.UNAUTHORIZED)

        assertEquals(ContentState.Empty, viewModel().uiState.value.content)
    }

    @Test
    fun `sin sesion (401) con viajes guardados tampoco avisa`() = runTest(mainDispatcherRule.testDispatcher) {
        givenSavedTrips(tonight)
        trips.refreshMyTripsResult = AppResult.Failure(DataError.UNAUTHORIZED)
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onAction(DriverHomeAction.Refresh)
            expectNoEvents()
        }
        assertEquals(listOf(tonight), viewModel.content().trips)
    }

    @Test
    fun `reintentar despues de un error trae los viajes`() = runTest(mainDispatcherRule.testDispatcher) {
        trips.refreshMyTripsResult = AppResult.Failure(DataError.SERVER)
        val viewModel = viewModel()
        assertTrue(viewModel.uiState.value.content is ContentState.Error)

        trips.refreshMyTripsResult = AppResult.Success(Unit)
        givenSavedTrips(tonight)
        viewModel.onAction(DriverHomeAction.Refresh)

        assertEquals(listOf(tonight), viewModel.content().trips)
        assertEquals(2, trips.refreshMyTripsCalls)
    }

    @Test
    fun `publicar y abrir un viaje emiten los efectos de navegacion`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onAction(DriverHomeAction.PublishTrip)
            assertEquals(DriverHomeEffect.NavigateToPublish, awaitItem())

            viewModel.onAction(DriverHomeAction.OpenTrip("tonight"))
            assertEquals(DriverHomeEffect.NavigateToPanel("tonight"), awaitItem())
        }
    }

    @Test
    fun `abrir inicio con viajes ya publicados no pide permiso de notificaciones`() =
        runTest(mainDispatcherRule.testDispatcher) {
            givenSavedTrips(tonight)

            val viewModel = viewModel()

            assertFalse(viewModel.uiState.value.shouldPromptNotifications)
        }

    @Test
    fun `publicar un viaje nuevo desde inicio habilita el pedido contextual de notificaciones`() =
        runTest(mainDispatcherRule.testDispatcher) {
            givenSavedTrips(tonight)
            val viewModel = viewModel()
            assertFalse(viewModel.uiState.value.shouldPromptNotifications)

            viewModel.onAction(DriverHomeAction.PublishTrip)
            assertFalse(viewModel.uiState.value.shouldPromptNotifications)

            givenSavedTrips(tonight, tomorrow)

            assertTrue(viewModel.uiState.value.shouldPromptNotifications)
        }
}
