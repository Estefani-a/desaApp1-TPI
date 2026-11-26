package com.example.uniride.presentation.navigation

import app.cash.turbine.test
import com.example.uniride.data.datastore.AppModeDataStore
import com.example.uniride.domain.model.AppMode
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.User
import com.example.uniride.domain.model.UserId
import com.example.uniride.domain.model.Vehicle
import com.example.uniride.domain.usecase.mode.ObserveAppModeUseCase
import com.example.uniride.domain.usecase.mode.SetAppModeUseCase
import com.example.uniride.domain.usecase.auth.ObserveIsLoggedInUseCase
import com.example.uniride.domain.usecase.profile.ObserveCurrentUserUseCase
import com.example.uniride.fake.FakeProfileRepository
import com.example.uniride.fake.FakeSessionRepository
import com.example.uniride.fake.InMemoryPreferencesDataStore
import com.example.uniride.fake.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Shell of the app (G3): session, mode selector and the modes the profile allows. */
class AppViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val session = FakeSessionRepository()
    private val profiles = FakeProfileRepository()
    private val appModes = AppModeDataStore(InMemoryPreferencesDataStore())

    private fun user(preferredMode: PreferredMode, withVehicle: Boolean) = User(
        id = UserId("user-1"),
        email = "user-1@uade.edu.ar",
        profile = Profile(
            fullName = "Lucía Fernández",
            career = "Ingeniería en Informática",
            phone = "+5491155550000",
            preferredMode = preferredMode,
            vehicle = if (withVehicle) Vehicle("Volkswagen", "Gol Trend", "Gris", "AB123CD") else null,
        ),
        rating = null,
    )

    private fun TestScope.viewModel(): AppViewModel {
        val viewModel = AppViewModel(
            observeIsLoggedIn = ObserveIsLoggedInUseCase(session),
            observeCurrentUser = ObserveCurrentUserUseCase(profiles),
            observeAppMode = ObserveAppModeUseCase(appModes),
            setAppMode = SetAppModeUseCase(appModes),
        )
        // uiState is WhileSubscribed: someone has to collect it, like the screen does
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun `sin sesion arranca afuera`() = runTest(mainDispatcherRule.testDispatcher) {
        assertEquals(SessionStatus.LOGGED_OUT, viewModel().uiState.value.session)
    }

    @Test
    fun `con sesion arranca adentro`() = runTest(mainDispatcherRule.testDispatcher) {
        session.login(EMAIL, PASSWORD)

        assertEquals(SessionStatus.LOGGED_IN, viewModel().uiState.value.session)
    }

    @Test
    fun `elegir un modo lo guarda y lo usa`() = runTest(mainDispatcherRule.testDispatcher) {
        session.login(EMAIL, PASSWORD)
        profiles.currentUser.value = user(PreferredMode.AMBOS, withVehicle = true)
        val viewModel = viewModel()

        viewModel.onAction(AppAction.ChangeMode(AppMode.CONDUCTOR))

        assertEquals(AppMode.CONDUCTOR, viewModel.uiState.value.appMode)
        assertEquals(AppMode.CONDUCTOR, appModes.observeAppMode().first())
    }

    @Test
    fun `al abrir la app vuelve al ultimo modo elegido`() = runTest(mainDispatcherRule.testDispatcher) {
        profiles.currentUser.value = user(PreferredMode.AMBOS, withVehicle = true)
        appModes.setAppMode(AppMode.CONDUCTOR)

        assertEquals(AppMode.CONDUCTOR, viewModel().uiState.value.appMode)
    }

    @Test
    fun `sin vehiculo es pasajero aunque haya elegido conductor`() = runTest(mainDispatcherRule.testDispatcher) {
        profiles.currentUser.value = user(PreferredMode.AMBOS, withVehicle = false)
        appModes.setAppMode(AppMode.CONDUCTOR)

        val state = viewModel().uiState.value

        assertEquals(AppMode.PASAJERO, state.appMode)
        assertEquals(setOf(AppMode.PASAJERO), state.availableModes)
    }

    @Test
    fun `un conductor con vehiculo arranca como conductor`() = runTest(mainDispatcherRule.testDispatcher) {
        profiles.currentUser.value = user(PreferredMode.CONDUCTOR, withVehicle = true)

        assertEquals(AppMode.CONDUCTOR, viewModel().uiState.value.appMode)
    }

    @Test
    fun `cuando la sesion termina lo avisa`() = runTest(mainDispatcherRule.testDispatcher) {
        session.login(EMAIL, PASSWORD)
        val viewModel = viewModel()

        viewModel.effects.test {
            session.logout()
            assertEquals(AppEffect.SessionEnded, awaitItem())
        }
    }

    @Test
    fun `abrir la app sin sesion no es una sesion que termina`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.effects.test {
            expectNoEvents()
        }
    }

    private companion object {
        const val EMAIL = "user-1@uade.edu.ar"
        const val PASSWORD = "clave1234"
    }
}
