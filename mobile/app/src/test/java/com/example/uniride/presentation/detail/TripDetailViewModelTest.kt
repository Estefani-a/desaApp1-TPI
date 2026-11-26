package com.example.uniride.presentation.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.uniride.R
import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.DomainFixtures.PASSENGER_ID
import com.example.uniride.domain.model.DomainFixtures.TRIP_ID
import com.example.uniride.domain.model.DomainFixtures.reservation
import com.example.uniride.domain.model.DomainFixtures.trip
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.ReservationId
import com.example.uniride.domain.model.ReservationRejection
import com.example.uniride.domain.model.ReservationStatus
import com.example.uniride.domain.model.User
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.domain.usecase.reservation.CancelReservationUseCase
import com.example.uniride.domain.usecase.reservation.DismissReservationRejectionUseCase
import com.example.uniride.domain.usecase.reservation.ObserveTripDetailUseCase
import com.example.uniride.domain.usecase.reservation.RefreshTripDetailUseCase
import com.example.uniride.domain.usecase.reservation.RequestSeatUseCase
import com.example.uniride.domain.usecase.reservation.TripDetail
import com.example.uniride.domain.validation.SeatRequestDecision
import com.example.uniride.fake.FakeNetworkMonitor
import com.example.uniride.fake.FakeProfileRepository
import com.example.uniride.fake.FakeReservationRepository
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
import java.time.LocalTime
import java.time.ZoneId

/** B3 Detalle del viaje / B6 Mi viaje (RF04). "Now" is 2026-11-20 18:00 and the trip leaves at 22:15. */
class TripDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-11-20T21:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"))

    private val trips = FakeTripRepository(listOf(trip()))
    private val reservations = FakeReservationRepository(currentUserId = PASSENGER_ID.value)
    private val profiles = FakeProfileRepository(
        User(
            id = PASSENGER_ID,
            email = "passenger-1@uade.edu.ar",
            profile = Profile("Martín Gómez", "Administración", "+5491155550001", PreferredMode.PASAJERO, vehicle = null),
            rating = null,
        ),
    )
    private val network = FakeNetworkMonitor(initialOnline = true)

    private fun TestScope.viewModel(): TripDetailViewModel {
        val viewModel = TripDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf(TripDetailViewModel.TRIP_ID_KEY to TRIP_ID.value)),
            observeTripDetail = ObserveTripDetailUseCase(trips, reservations, profiles, clock),
            observeConnectivity = ObserveConnectivityUseCase(network),
            refreshTripDetail = RefreshTripDetailUseCase(trips),
            requestSeat = RequestSeatUseCase(trips, reservations, profiles, clock),
            cancelReservation = CancelReservationUseCase(trips, reservations, clock),
            dismissRejection = DismissReservationRejectionUseCase(reservations),
        )
        // uiState is WhileSubscribed: someone has to collect it, like the screen does
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun TripDetailViewModel.detail(): TripDetail = (uiState.value.content as ContentState.Success).data

    @Test
    fun `un pasajero sin reserva puede pedir asiento`() = runTest(mainDispatcherRule.testDispatcher) {
        val detail = viewModel().detail()

        assertEquals(SeatRequestDecision.ALLOWED, detail.seatRequest)
        assertFalse(detail.canCancel)
    }

    @Test
    fun `pedir asiento guarda la solicitud y lleva a la confirmacion`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onAction(TripDetailAction.RequestSeat)
            assertEquals(TripDetailEffect.NavigateToSeatRequested(TRIP_ID.value), awaitItem())
        }
        assertEquals(listOf("requestSeat:${TRIP_ID.value}"), reservations.calls)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `despues de pedir asiento no se pide otro y se dice por que`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onAction(TripDetailAction.RequestSeat)
            awaitItem()
            viewModel.onAction(TripDetailAction.RequestSeat)
            assertEquals(TripDetailEffect.ShowMessage(R.string.seat_decision_already_reserved), awaitItem())
        }
        assertEquals(1, reservations.calls.count { it.startsWith("requestSeat") })
        assertTrue(viewModel.detail().isPendingSync)
    }

    @Test
    fun `cancelar pide confirmacion antes de guardar`() = runTest(mainDispatcherRule.testDispatcher) {
        reservations.reservations.value = listOf(reservation(ReservationStatus.ACEPTADA))
        val viewModel = viewModel()

        viewModel.onAction(TripDetailAction.AskCancel)

        assertTrue(viewModel.uiState.value.isConfirmingCancel)
        assertTrue(reservations.calls.none { it.startsWith("cancel") })
    }

    @Test
    fun `confirmar la cancelacion la guarda y lo avisa`() = runTest(mainDispatcherRule.testDispatcher) {
        reservations.reservations.value = listOf(reservation(ReservationStatus.ACEPTADA))
        val viewModel = viewModel()
        viewModel.onAction(TripDetailAction.AskCancel)

        viewModel.effects.test {
            viewModel.onAction(TripDetailAction.ConfirmCancel)
            assertEquals(TripDetailEffect.ShowMessage(R.string.detail_cancel_done), awaitItem())
        }
        assertEquals(listOf("cancel:res-1"), reservations.calls)
        assertFalse(viewModel.uiState.value.isConfirmingCancel)
    }

    @Test
    fun `no se puede cancelar un viaje que ya empezo`() = runTest(mainDispatcherRule.testDispatcher) {
        trips.trips.value = listOf(trip(time = LocalTime.of(17, 30)))
        reservations.reservations.value = listOf(reservation(ReservationStatus.ACEPTADA))
        val viewModel = viewModel()

        assertFalse(viewModel.detail().canCancel)
        viewModel.effects.test {
            viewModel.onAction(TripDetailAction.ConfirmCancel)
            assertEquals(TripDetailEffect.ShowMessage(R.string.change_block_cancel_departed), awaitItem())
        }
        assertTrue(reservations.calls.none { it.startsWith("cancel") })
    }

    @Test
    fun `muestra el motivo de un rechazo del servidor hasta que se descarta`() = runTest(mainDispatcherRule.testDispatcher) {
        reservations.reservations.value = listOf(reservation(ReservationStatus.ACEPTADA))
        reservations.rejections.value = mapOf(ReservationId("res-1") to ReservationRejection.TRIP_ALREADY_DEPARTED)
        val viewModel = viewModel()
        assertEquals(ReservationRejection.TRIP_ALREADY_DEPARTED, viewModel.detail().rejection)

        viewModel.onAction(TripDetailAction.DismissRejection)

        assertEquals(null, viewModel.detail().rejection)
    }

    @Test
    fun `ver solicitudes lleva al panel del conductor`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onAction(TripDetailAction.OpenDriverPanel)
            assertEquals(TripDetailEffect.NavigateToDriverPanel(TRIP_ID.value), awaitItem())
        }
    }

    @Test
    fun `un viaje que no esta ni en el telefono ni en el servidor no se encuentra`() = runTest(mainDispatcherRule.testDispatcher) {
        trips.trips.value = emptyList()
        trips.refreshTripResult = AppResult.Failure(DataError.NOT_FOUND)

        val content = viewModel().uiState.value.content

        assertEquals(ContentState.Error(R.string.detail_not_found, canRetry = false), content)
    }

    @Test
    fun `sin conexion y sin el viaje guardado muestra el error para reintentar`() = runTest(mainDispatcherRule.testDispatcher) {
        trips.trips.value = emptyList()
        trips.refreshTripResult = AppResult.Failure(DataError.NO_CONNECTION)

        val content = viewModel().uiState.value.content

        assertTrue(content is ContentState.Error && content.canRetry)
    }

    @Test
    fun `sin conexion el viaje guardado se sigue viendo`() = runTest(mainDispatcherRule.testDispatcher) {
        network.setOnline(false)
        trips.refreshTripResult = AppResult.Failure(DataError.NO_CONNECTION)

        val viewModel = viewModel()

        assertTrue(viewModel.uiState.value.isOffline)
        assertEquals(TRIP_ID, viewModel.detail().trip.id)
    }
}
