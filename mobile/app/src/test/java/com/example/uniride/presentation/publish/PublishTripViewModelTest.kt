package com.example.uniride.presentation.publish

import app.cash.turbine.test
import com.example.uniride.R
import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.Campus
import com.example.uniride.domain.model.CampusCode
import com.example.uniride.domain.model.Direction
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.usecase.campus.ObserveCampusesUseCase
import com.example.uniride.domain.usecase.campus.RefreshCampusesUseCase
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.domain.usecase.trip.PublishTripUseCase
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** C2 Publicar un viaje (RF02). "Now" is 2026-11-20 18:00 in Buenos Aires. */
class PublishTripViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-11-20T21:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"))

    private val monserrat = CampusCode("MONSERRAT")
    private val belgrano = CampusCode("BELGRANO")
    private val serverCampuses = listOf(
        Campus(monserrat, "Sede Monserrat", listOf("Salida Lima 717", "Av. Independencia 1000")),
        Campus(belgrano, "Sede Belgrano", listOf("Puerta Zapiola")),
    )

    private val trips = FakeTripRepository()
    private val campuses = FakeCampusRepository(serverCampuses)
    private val network = FakeNetworkMonitor(initialOnline = true)

    private fun viewModel() = PublishTripViewModel(
        observeCampuses = ObserveCampusesUseCase(campuses),
        refreshCampuses = RefreshCampusesUseCase(campuses),
        publishTrip = PublishTripUseCase(trips, campuses, clock),
        observeConnectivity = ObserveConnectivityUseCase(network),
        clock = clock,
    )

    /** The view model with its state being collected, as a screen would. */
    private fun TestScope.collectedViewModel(): PublishTripViewModel {
        val viewModel = viewModel()
        // uiState is WhileSubscribed: someone has to collect it, like the screen does
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun PublishTripViewModel.fillValidForm() {
        onAction(PublishTripAction.SelectCampus(monserrat))
        onAction(PublishTripAction.SelectMeetingPoint("Salida Lima 717"))
        onAction(PublishTripAction.ChangeNeighborhood("Ramos Mejía"))
        onAction(PublishTripAction.ChangeDepartureDate(LocalDate.of(2026, 11, 21)))
        onAction(PublishTripAction.ChangeDepartureTime(LocalTime.of(9, 0)))
        onAction(PublishTripAction.ChangeContribution("3500"))
    }

    @Test
    fun `publicar un viaje válido emite el efecto de publicado y guarda el borrador`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = collectedViewModel()
        viewModel.fillValidForm()

        viewModel.effects.test {
            viewModel.onAction(PublishTripAction.Submit)
            assertEquals(PublishTripEffect.Published(R.string.publish_success), awaitItem())
        }

        val draft = trips.publishedDrafts.single()
        assertEquals(monserrat, draft.campus)
        assertEquals(Direction.IDA, draft.route.direction)
        assertEquals("Salida Lima 717", draft.route.meetingPoint)
        assertEquals("Ramos Mejía", draft.route.destinationNeighborhood)
        assertNull(draft.route.destinationReference)
        assertEquals(3500, draft.contributionPerSeat)
        assertEquals(PublishTripForm.DEFAULT_SEATS, draft.totalSeats)
    }

    @Test
    fun `publicar sin conexión avisa que se publicará cuando vuelva la conexión`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = collectedViewModel()
        viewModel.fillValidForm()
        network.setOnline(false)

        viewModel.effects.test {
            viewModel.onAction(PublishTripAction.Submit)
            assertEquals(PublishTripEffect.Published(R.string.publish_success_offline), awaitItem())
        }
        assertEquals(1, trips.publishedDrafts.size)
        assertTrue(viewModel.uiState.value.isOffline)
    }

    @Test
    fun `los errores no se muestran antes de tocar publicar ni de dejar el campo`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = collectedViewModel()

        assertTrue(viewModel.uiState.value.errors.isEmpty())
    }

    @Test
    fun `al dejar un campo vacío se marca solo ese campo`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = collectedViewModel()

        viewModel.onAction(PublishTripAction.FieldLeft(PublishField.NEIGHBORHOOD))

        assertEquals(
            mapOf(PublishField.NEIGHBORHOOD to R.string.publish_error_neighborhood),
            viewModel.uiState.value.errors,
        )
    }

    @Test
    fun `publicar con el formulario vacío marca cada campo y no guarda nada`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = collectedViewModel()

        viewModel.onAction(PublishTripAction.Submit)

        val state = viewModel.uiState.value
        assertEquals(
            mapOf(
                PublishField.CAMPUS to R.string.publish_error_campus,
                PublishField.MEETING_POINT to R.string.publish_error_meeting_point,
                PublishField.NEIGHBORHOOD to R.string.publish_error_neighborhood,
                PublishField.DEPARTURE to R.string.publish_error_departure_missing,
                PublishField.CONTRIBUTION to R.string.publish_error_contribution,
            ),
            state.errors,
        )
        assertFalse(state.isSubmitting)
        assertTrue(trips.publishedDrafts.isEmpty())
    }

    @Test
    fun `una salida en el pasado se marca en el campo de la salida`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = collectedViewModel()
        viewModel.fillValidForm()
        viewModel.onAction(PublishTripAction.ChangeDepartureDate(LocalDate.of(2026, 11, 20)))
        viewModel.onAction(PublishTripAction.ChangeDepartureTime(LocalTime.of(17, 0)))

        viewModel.onAction(PublishTripAction.Submit)

        assertEquals(
            mapOf(PublishField.DEPARTURE to R.string.publish_error_departure_past),
            viewModel.uiState.value.errors,
        )
        assertTrue(trips.publishedDrafts.isEmpty())
    }

    @Test
    fun `el aporte admite como máximo 6 dígitos y solo números`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = collectedViewModel()

        viewModel.onAction(PublishTripAction.ChangeContribution("12a3456789"))

        assertEquals("123456", viewModel.uiState.value.form.contribution)
    }

    @Test
    fun `con un aporte de 6 dígitos se puede publicar porque entra en un Int`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = collectedViewModel()
        viewModel.fillValidForm()
        viewModel.onAction(PublishTripAction.ChangeContribution("999999"))

        viewModel.effects.test {
            viewModel.onAction(PublishTripAction.Submit)
            awaitItem()
        }

        assertEquals(999_999, trips.publishedDrafts.single().contributionPerSeat)
    }

    @Test
    fun `si falta el vehículo muestra el mensaje del perfil y deja volver a intentar`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = collectedViewModel()
        viewModel.fillValidForm()
        trips.publishResult = AppResult.Failure(DataError.VALIDATION)

        viewModel.effects.test {
            viewModel.onAction(PublishTripAction.Submit)
            assertEquals(PublishTripEffect.ShowMessage(R.string.publish_error_vehicle), awaitItem())
        }

        assertFalse(viewModel.uiState.value.isSubmitting)
        trips.publishResult = AppResult.Success(TripId("published-trip"))
        viewModel.effects.test {
            viewModel.onAction(PublishTripAction.Submit)
            assertEquals(PublishTripEffect.Published(R.string.publish_success), awaitItem())
        }
    }

    @Test
    fun `una sesión vencida muestra el mensaje de sesión`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = collectedViewModel()
        viewModel.fillValidForm()
        trips.publishResult = AppResult.Failure(DataError.UNAUTHORIZED)

        viewModel.effects.test {
            viewModel.onAction(PublishTripAction.Submit)
            assertEquals(PublishTripEffect.ShowMessage(R.string.common_error_unauthorized), awaitItem())
        }
    }

    @Test
    fun `cambiar de sede limpia el punto de encuentro`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = collectedViewModel()
        viewModel.fillValidForm()
        assertEquals("Salida Lima 717", viewModel.uiState.value.form.meetingPoint)

        viewModel.onAction(PublishTripAction.SelectCampus(belgrano))

        val state = viewModel.uiState.value
        assertNull(state.form.meetingPoint)
        assertEquals(listOf("Puerta Zapiola"), state.meetingPoints)
        assertFalse(PublishField.MEETING_POINT in state.errors)
    }

    @Test
    fun `no se puede publicar dos veces seguidas`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = collectedViewModel()
        viewModel.fillValidForm()

        viewModel.effects.test {
            viewModel.onAction(PublishTripAction.Submit)
            viewModel.onAction(PublishTripAction.Submit)
            assertEquals(PublishTripEffect.Published(R.string.publish_success), awaitItem())
            expectNoEvents()
        }

        assertEquals(1, trips.publishedDrafts.size)
        assertTrue(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `con las sedes cargadas muestra el contenido`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = collectedViewModel()

        assertEquals(ContentState.Success(serverCampuses), viewModel.uiState.value.campuses)
    }

    @Test
    fun `sin sedes y sin conexión muestra el error y al reintentar carga las sedes`() = runTest(mainDispatcherRule.testDispatcher) {
        campuses.refreshResult = AppResult.Failure(DataError.NO_CONNECTION)
        network.setOnline(false)
        val viewModel = collectedViewModel()

        assertEquals(ContentState.Error(R.string.publish_campuses_offline), viewModel.uiState.value.campuses)

        campuses.refreshResult = AppResult.Success(Unit)
        network.setOnline(true)
        viewModel.onAction(PublishTripAction.RetryCampuses)

        assertEquals(ContentState.Success(serverCampuses), viewModel.uiState.value.campuses)
    }
}
