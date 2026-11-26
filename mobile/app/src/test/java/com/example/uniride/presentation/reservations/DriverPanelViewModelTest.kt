package com.example.uniride.presentation.reservations

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.uniride.R
import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.DomainFixtures.DRIVER_ID
import com.example.uniride.domain.model.DomainFixtures.TRIP_ID
import com.example.uniride.domain.model.DomainFixtures.reservation
import com.example.uniride.domain.model.DomainFixtures.trip
import com.example.uniride.domain.model.ReservationId
import com.example.uniride.domain.model.ReservationRejection
import com.example.uniride.domain.model.ReservationStatus
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.domain.usecase.reservation.DismissReservationRejectionUseCase
import com.example.uniride.domain.usecase.reservation.DriverPanel
import com.example.uniride.domain.usecase.reservation.ObserveDriverPanelUseCase
import com.example.uniride.domain.usecase.reservation.RefreshDriverPanelUseCase
import com.example.uniride.domain.usecase.reservation.RespondToRequestUseCase
import com.example.uniride.fake.FakeNetworkMonitor
import com.example.uniride.fake.FakeReservationRepository
import com.example.uniride.fake.FakeTripRepository
import com.example.uniride.fake.MainDispatcherRule
import com.example.uniride.presentation.common.ContentState
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** C4 Panel del conductor (RF04). "Now" is 2026-11-20 18:00 and the trip leaves at 22:15. */
class DriverPanelViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-11-20T21:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"))

    private val trips = FakeTripRepository(listOf(trip()))
    private val reservations = FakeReservationRepository(
        initial = listOf(reservation(ReservationStatus.PENDIENTE)),
        currentUserId = DRIVER_ID.value,
    )
    private val network = FakeNetworkMonitor(initialOnline = true)

    private fun TestScope.viewModel(): DriverPanelViewModel {
        val viewModel = DriverPanelViewModel(
            savedStateHandle = SavedStateHandle(mapOf(DriverPanelViewModel.TRIP_ID_KEY to TRIP_ID.value)),
            observeDriverPanel = ObserveDriverPanelUseCase(trips, reservations, clock),
            observeConnectivity = ObserveConnectivityUseCase(network),
            refreshDriverPanel = RefreshDriverPanelUseCase(trips, reservations),
            respondToRequest = RespondToRequestUseCase(trips, reservations, clock),
            dismissRejection = DismissReservationRejectionUseCase(reservations),
        )
        // uiState is WhileSubscribed: someone has to collect it, like the screen does
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun DriverPanelViewModel.panel(): DriverPanel = (uiState.value.content as ContentState.Success).data

    @Test
    fun `muestra las solicitudes del viaje`() = runTest(mainDispatcherRule.testDispatcher) {
        val panel = viewModel().panel()

        assertEquals(listOf("res-1"), panel.requests.map { it.reservation.id.value })
        assertTrue(panel.requests.single().canBeAnswered)
    }

    @Test
    fun `aceptar una solicitud la guarda y lo avisa`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onAction(DriverPanelAction.Accept("res-1"))
            assertEquals(DriverPanelEffect.ShowMessage(R.string.driver_panel_accepted), awaitItem())
        }
        assertEquals(listOf("accept:res-1"), reservations.calls.filterNot { it.startsWith("refresh") })
        assertNull(viewModel.uiState.value.answeringId)
    }

    @Test
    fun `rechazar una solicitud la guarda y lo avisa`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onAction(DriverPanelAction.Reject("res-1"))
            assertEquals(DriverPanelEffect.ShowMessage(R.string.driver_panel_rejected), awaitItem())
        }
        assertEquals(listOf("reject:res-1"), reservations.calls.filterNot { it.startsWith("refresh") })
    }

    @Test
    fun `sin vacantes no se acepta y se dice por que`() = runTest(mainDispatcherRule.testDispatcher) {
        trips.trips.value = listOf(trip(availableSeats = 0))
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onAction(DriverPanelAction.Accept("res-1"))
            assertEquals(DriverPanelEffect.ShowMessage(R.string.change_block_no_seats), awaitItem())
        }
        assertTrue(reservations.calls.none { it.startsWith("accept") })
    }

    @Test
    fun `despues de la salida no se responde`() = runTest(mainDispatcherRule.testDispatcher) {
        trips.trips.value = listOf(trip(time = LocalTime.of(17, 30)))
        val viewModel = viewModel()

        assertTrue(viewModel.panel().hasStarted)
        viewModel.effects.test {
            viewModel.onAction(DriverPanelAction.Accept("res-1"))
            assertEquals(DriverPanelEffect.ShowMessage(R.string.change_block_respond_departed), awaitItem())
        }
    }

    @Test
    fun `el aviso de un rechazo del servidor se descarta`() = runTest(mainDispatcherRule.testDispatcher) {
        reservations.rejections.value = mapOf(ReservationId("res-1") to ReservationRejection.NO_SEATS)
        val viewModel = viewModel()
        assertEquals(ReservationRejection.NO_SEATS, viewModel.panel().requests.single().rejection)

        viewModel.onAction(DriverPanelAction.DismissRejection("res-1"))

        assertNull(viewModel.panel().requests.single().rejection)
    }

    @Test
    fun `tocar el viaje abre su detalle`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onAction(DriverPanelAction.OpenTrip)
            assertEquals(DriverPanelEffect.NavigateToTrip(TRIP_ID.value), awaitItem())
        }
    }

    @Test
    fun `sin el viaje guardado y sin conexion muestra el error para reintentar`() = runTest(mainDispatcherRule.testDispatcher) {
        trips.trips.value = emptyList()
        trips.refreshTripResult = AppResult.Failure(DataError.NO_CONNECTION)

        val content = viewModel().uiState.value.content

        assertTrue(content is ContentState.Error && content.canRetry)
    }
}
