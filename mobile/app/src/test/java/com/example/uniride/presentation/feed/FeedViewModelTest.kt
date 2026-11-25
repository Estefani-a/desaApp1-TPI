package com.example.uniride.presentation.feed

import app.cash.turbine.test
import com.example.uniride.R
import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.Campus
import com.example.uniride.domain.model.CampusCode
import com.example.uniride.domain.model.DomainFixtures.trip
import com.example.uniride.domain.model.Shift
import com.example.uniride.domain.model.TripFilters
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.usecase.campus.ObserveCampusesUseCase
import com.example.uniride.domain.usecase.campus.RefreshCampusesUseCase
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.domain.usecase.trip.ObserveFeedFiltersUseCase
import com.example.uniride.domain.usecase.trip.ObserveLastTripsRefreshUseCase
import com.example.uniride.domain.usecase.trip.ObserveTripsUseCase
import com.example.uniride.domain.usecase.trip.RefreshTripsUseCase
import com.example.uniride.domain.usecase.trip.SaveFeedFiltersUseCase
import com.example.uniride.fake.FakeCampusRepository
import com.example.uniride.fake.FakeFeedFiltersRepository
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
import java.time.LocalTime
import java.time.ZoneId

/** B1 Explorar (RF03) with the offline scenarios of 12.1. "Now" is 2026-11-20 18:00. */
class FeedViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-11-20T21:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"))
    private val lastRefresh = Instant.parse("2026-11-20T20:40:00Z")

    private val monserrat = trip(campus = CampusCode("MONSERRAT")).copy(id = TripId("monserrat"))
    private val belgrano = trip(campus = CampusCode("BELGRANO"), time = LocalTime.of(20, 0)).copy(id = TripId("belgrano"))

    private val trips = FakeTripRepository()
    private val filters = FakeFeedFiltersRepository()
    private val campuses = FakeCampusRepository(listOf(Campus(CampusCode("MONSERRAT"), "Sede Monserrat", emptyList())))
    private val network = FakeNetworkMonitor(initialOnline = true)

    private fun TestScope.viewModel(): FeedViewModel {
        val viewModel = FeedViewModel(
            observeTrips = ObserveTripsUseCase(trips, clock),
            refreshTrips = RefreshTripsUseCase(trips),
            observeFeedFilters = ObserveFeedFiltersUseCase(filters, clock),
            saveFeedFilters = SaveFeedFiltersUseCase(filters),
            observeCampuses = ObserveCampusesUseCase(campuses),
            refreshCampuses = RefreshCampusesUseCase(campuses),
            observeLastTripsRefresh = ObserveLastTripsRefreshUseCase(trips),
            observeConnectivity = ObserveConnectivityUseCase(network),
            clock = clock,
        )
        // uiState is WhileSubscribed: someone has to collect it, like the screen does
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun givenSavedTrips(vararg saved: com.example.uniride.domain.model.Trip) {
        trips.trips.value = saved.toList()
        trips.lastRefreshedAt.value = lastRefresh
    }

    @Test
    fun `al abrir refresca y muestra los viajes con sus sedes`() = runTest(mainDispatcherRule.testDispatcher) {
        trips.tripsAfterRefresh = listOf(monserrat, belgrano)

        val state = viewModel().uiState.value

        assertEquals(1, trips.refreshCalls)
        assertEquals(ContentState.Success(listOf(belgrano, monserrat)), state.content)
        assertEquals("Sede Monserrat", state.campusNames[CampusCode("MONSERRAT")])
        assertEquals(LocalTime.of(18, 0), state.lastUpdatedAt)
        assertFalse(state.isOffline)
    }

    @Test
    fun `primer inicio sin datos ni conexion muestra el error con reintento`() = runTest(mainDispatcherRule.testDispatcher) {
        network.setOnline(false)
        trips.refreshResult = AppResult.Failure(DataError.NO_CONNECTION)

        val state = viewModel().uiState.value

        assertEquals(ContentState.Error(R.string.feed_first_start_offline), state.content)
        assertTrue(state.isOffline)
    }

    @Test
    fun `sin conexion con datos guardados muestra los viajes y el banner con la hora`() = runTest(mainDispatcherRule.testDispatcher) {
        givenSavedTrips(monserrat)
        network.setOnline(false)
        trips.refreshResult = AppResult.Failure(DataError.NO_CONNECTION)

        val state = viewModel().uiState.value

        assertEquals(ContentState.Success(listOf(monserrat)), state.content)
        assertTrue(state.isOffline)
        assertEquals(LocalTime.of(17, 40), state.lastUpdatedAt)
    }

    @Test
    fun `cambiar un filtro lo guarda y la lista reacciona sin ir al servidor`() = runTest(mainDispatcherRule.testDispatcher) {
        givenSavedTrips(monserrat, belgrano)
        val viewModel = viewModel()
        val refreshesBefore = trips.refreshCalls

        viewModel.onAction(FeedAction.ChangeFilters(TripFilters(campus = CampusCode("BELGRANO"))))

        assertEquals(TripFilters(campus = CampusCode("BELGRANO")), filters.saved.value)
        assertEquals(ContentState.Success(listOf(belgrano)), viewModel.uiState.value.content)
        assertEquals(refreshesBefore, trips.refreshCalls)
    }

    @Test
    fun `los ultimos filtros se restauran al abrir`() = runTest(mainDispatcherRule.testDispatcher) {
        givenSavedTrips(monserrat, belgrano)
        filters.saved.value = TripFilters(campus = CampusCode("MONSERRAT"))

        val state = viewModel().uiState.value

        assertEquals(TripFilters(campus = CampusCode("MONSERRAT")), state.filters)
        assertEquals(ContentState.Success(listOf(monserrat)), state.content)
    }

    @Test
    fun `sin viajes para los filtros la lista queda vacia y quitar filtros la recupera`() = runTest(mainDispatcherRule.testDispatcher) {
        givenSavedTrips(monserrat)
        filters.saved.value = TripFilters(shift = Shift.MANANA)
        val viewModel = viewModel()

        assertEquals(ContentState.Empty, viewModel.uiState.value.content)

        viewModel.onAction(FeedAction.ClearFilters)

        assertEquals(ContentState.Success(listOf(monserrat)), viewModel.uiState.value.content)
    }

    @Test
    fun `al volver la conexion refresca solo`() = runTest(mainDispatcherRule.testDispatcher) {
        givenSavedTrips(monserrat)
        network.setOnline(false)
        viewModel()
        val refreshesWhileOffline = trips.refreshCalls

        network.setOnline(true)

        assertEquals(refreshesWhileOffline + 1, trips.refreshCalls)
    }

    @Test
    fun `un refresco manual fallido con viajes en pantalla avisa con un mensaje`() = runTest(mainDispatcherRule.testDispatcher) {
        givenSavedTrips(monserrat)
        val viewModel = viewModel()
        trips.refreshResult = AppResult.Failure(DataError.SERVER)

        viewModel.effects.test {
            viewModel.onAction(FeedAction.Refresh)

            assertEquals(FeedEffect.ShowMessage(R.string.common_error_server), awaitItem())
        }
        assertEquals(ContentState.Success(listOf(monserrat)), viewModel.uiState.value.content)
    }

    @Test
    fun `tocar un viaje navega a su detalle`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onAction(FeedAction.OpenTrip("monserrat"))

            assertEquals(FeedEffect.NavigateToDetail("monserrat"), awaitItem())
        }
    }

    @Test
    fun `el filtro de fecha ofrece hoy y los seis dias siguientes`() = runTest(mainDispatcherRule.testDispatcher) {
        val options = viewModel().uiState.value.dateOptions

        assertEquals(7, options.size)
        assertEquals(java.time.LocalDate.of(2026, 11, 20), options.first())
    }
}
