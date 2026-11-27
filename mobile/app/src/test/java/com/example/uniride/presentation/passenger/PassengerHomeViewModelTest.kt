package com.example.uniride.presentation.passenger

import app.cash.turbine.test
import com.example.uniride.R
import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.Campus
import com.example.uniride.domain.model.CampusCode
import com.example.uniride.domain.model.DomainFixtures.PASSENGER_ID
import com.example.uniride.domain.model.DomainFixtures.TRIP_ID
import com.example.uniride.domain.model.DomainFixtures.reservation
import com.example.uniride.domain.model.DomainFixtures.trip
import com.example.uniride.domain.model.ReservationId
import com.example.uniride.domain.model.ReservationRejection
import com.example.uniride.domain.model.ReservationStatus
import com.example.uniride.domain.usecase.campus.ObserveCampusesUseCase
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.domain.usecase.reservation.DismissReservationRejectionUseCase
import com.example.uniride.domain.usecase.reservation.ObserveMyActiveTripUseCase
import com.example.uniride.domain.usecase.trip.RefreshMyTripsUseCase
import com.example.uniride.fake.FakeCampusRepository
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
import java.time.ZoneId

/** Start of the passenger (RF04) with the offline scenarios of 12.1. */
class PassengerHomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val trips = FakeTripRepository(listOf(trip()))
    private val reservations = FakeReservationRepository(currentUserId = PASSENGER_ID.value)
    private val campuses = FakeCampusRepository().apply {
        campuses.value = listOf(Campus(CampusCode("MONSERRAT"), "Sede Monserrat", emptyList()))
    }
    private val network = FakeNetworkMonitor(initialOnline = true)
    private val clock = Clock.fixed(Instant.parse("2026-11-20T21:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"))

    private fun TestScope.viewModel(): PassengerHomeViewModel {
        val viewModel = PassengerHomeViewModel(
            observeMyActiveTrip = ObserveMyActiveTripUseCase(trips, reservations, clock),
            observeCampuses = ObserveCampusesUseCase(campuses),
            observeConnectivity = ObserveConnectivityUseCase(network),
            refreshMyTrips = RefreshMyTripsUseCase(trips),
            dismissRejection = DismissReservationRejectionUseCase(reservations),
        )
        // uiState is WhileSubscribed: someone has to collect it, like the screen does
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun PassengerHomeViewModel.content(): PassengerHomeContent =
        (uiState.value.content as ContentState.Success).data

    @Test
    fun `sin reservas activas invita a explorar`() = runTest(mainDispatcherRule.testDispatcher) {
        reservations.reservations.value = listOf(reservation(ReservationStatus.CANCELADA))

        assertEquals(ContentState.Empty, viewModel().uiState.value.content)
    }

    @Test
    fun `muestra el proximo viaje con el estado de la solicitud y la sede`() = runTest(mainDispatcherRule.testDispatcher) {
        reservations.reservations.value = listOf(reservation(ReservationStatus.PENDIENTE))
        reservations.pendingIds.value = setOf(ReservationId("res-1"))

        val content = viewModel().content()

        assertEquals(TRIP_ID, content.myTrip.trip.id)
        assertEquals(ReservationStatus.PENDIENTE, content.myTrip.reservation.status)
        assertTrue(content.myTrip.isPendingSync)
        assertEquals("Sede Monserrat", content.campusName)
    }

    @Test
    fun `tocar el viaje abre mi viaje`() = runTest(mainDispatcherRule.testDispatcher) {
        reservations.reservations.value = listOf(reservation(ReservationStatus.ACEPTADA))
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onAction(PassengerHomeAction.OpenMyTrip)
            assertEquals(PassengerHomeEffect.NavigateToMyTrip(TRIP_ID.value), awaitItem())
        }
    }

    @Test
    fun `explorar lleva al feed`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onAction(PassengerHomeAction.ExploreTrips)
            assertEquals(PassengerHomeEffect.NavigateToFeed, awaitItem())
        }
    }

    @Test
    fun `el aviso de un rechazo se descarta`() = runTest(mainDispatcherRule.testDispatcher) {
        reservations.reservations.value = listOf(reservation(ReservationStatus.ACEPTADA))
        reservations.rejections.value = mapOf(ReservationId("res-1") to ReservationRejection.TRIP_ALREADY_DEPARTED)
        val viewModel = viewModel()

        viewModel.onAction(PassengerHomeAction.DismissRejection)

        assertNull(viewModel.content().myTrip.rejection)
    }

    @Test
    fun `sin viaje guardado un error del servidor se puede reintentar`() = runTest(mainDispatcherRule.testDispatcher) {
        trips.refreshMyTripsResult = AppResult.Failure(DataError.SERVER)

        assertEquals(ContentState.Error(R.string.common_error_server), viewModel().uiState.value.content)
    }

    @Test
    fun `con un viaje en pantalla un error del servidor es un aviso`() = runTest(mainDispatcherRule.testDispatcher) {
        reservations.reservations.value = listOf(reservation(ReservationStatus.ACEPTADA))
        val viewModel = viewModel()
        trips.refreshMyTripsResult = AppResult.Failure(DataError.SERVER)

        viewModel.effects.test {
            viewModel.onAction(PassengerHomeAction.Refresh)
            assertEquals(PassengerHomeEffect.ShowMessage(R.string.common_error_server), awaitItem())
        }
        assertEquals(TRIP_ID, viewModel.content().myTrip.trip.id)
    }

    @Test
    fun `sin conexion el viaje guardado se sigue viendo sin aviso`() = runTest(mainDispatcherRule.testDispatcher) {
        reservations.reservations.value = listOf(reservation(ReservationStatus.ACEPTADA))
        network.setOnline(false)
        trips.refreshMyTripsResult = AppResult.Failure(DataError.NO_CONNECTION)
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onAction(PassengerHomeAction.Refresh)
            expectNoEvents()
        }
        assertTrue(viewModel.uiState.value.isOffline)
        assertEquals(TRIP_ID, viewModel.content().myTrip.trip.id)
    }
}
