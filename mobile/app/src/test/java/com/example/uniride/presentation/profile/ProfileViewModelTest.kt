package com.example.uniride.presentation.profile

import com.example.uniride.R
import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.usecase.auth.LogoutUseCase
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.domain.usecase.profile.ObserveCurrentUserUseCase
import com.example.uniride.domain.usecase.profile.RefreshProfileUseCase
import com.example.uniride.domain.usecase.profile.UpdateProfileUseCase
import com.example.uniride.fake.FakeNetworkMonitor
import com.example.uniride.fake.FakeProfileRepository
import com.example.uniride.fake.FakeSessionRepository
import com.example.uniride.fake.MainDispatcherRule
import com.example.uniride.fake.sampleUser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `perfil carga usuario cacheado sin conexion aunque falle el refresh`() = runTest {
        val profileRepo = FakeProfileRepository(initialUser = sampleUser()).apply {
            refreshResult = AppResult.Failure(DataError.NO_CONNECTION)
        }
        val sessionRepo = FakeSessionRepository(initialLoggedIn = true)
        val vm = ProfileViewModel(
            observeCurrentUserUseCase = ObserveCurrentUserUseCase(profileRepo),
            refreshProfileUseCase = RefreshProfileUseCase(profileRepo),
            updateProfileUseCase = UpdateProfileUseCase(profileRepo),
            logoutUseCase = LogoutUseCase(sessionRepo),
            observeConnectivity = ObserveConnectivityUseCase(FakeNetworkMonitor(false)),
        )

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        val state = vm.uiState.first()
        assertTrue(state.hasLoadedUser)
        assertEquals("lfernandez@uade.edu.ar", state.email)
        assertEquals("Lucía Fernández", state.fullName)
        assertTrue(state.isOffline)
        assertEquals(null, state.loadErrorRes)
        collector.cancel()
    }

    @Test
    fun `perfil guarda cambios con vehiculo y muestra confirmacion`() = runTest {
        val profileRepo = FakeProfileRepository(initialUser = sampleUser())
        val sessionRepo = FakeSessionRepository(initialLoggedIn = true)
        val vm = ProfileViewModel(
            observeCurrentUserUseCase = ObserveCurrentUserUseCase(profileRepo),
            refreshProfileUseCase = RefreshProfileUseCase(profileRepo),
            updateProfileUseCase = UpdateProfileUseCase(profileRepo),
            logoutUseCase = LogoutUseCase(sessionRepo),
            observeConnectivity = ObserveConnectivityUseCase(FakeNetworkMonitor(true)),
        )

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(ProfileAction.HasVehicleChanged(true))
        vm.onAction(ProfileAction.VehicleBrandChanged("Toyota"))
        vm.onAction(ProfileAction.VehicleModelChanged("Etios"))
        vm.onAction(ProfileAction.VehicleColorChanged("Blanco"))
        vm.onAction(ProfileAction.VehiclePlateChanged("af123cd"))
        vm.onAction(ProfileAction.Save)

        val state = vm.uiState.first()
        assertEquals(R.string.profile_saved_message, state.feedbackMessageRes)
        assertFalse(state.isFeedbackError)
        assertEquals("AF123CD", profileRepo.lastUpdatedProfile?.vehicle?.plate)
        collector.cancel()
    }

    @Test
    fun `confirmar logout llama a logoutUseCase y cierra el dialogo`() = runTest {
        val profileRepo = FakeProfileRepository(initialUser = sampleUser())
        val sessionRepo = FakeSessionRepository(initialLoggedIn = true)
        val vm = ProfileViewModel(
            observeCurrentUserUseCase = ObserveCurrentUserUseCase(profileRepo),
            refreshProfileUseCase = RefreshProfileUseCase(profileRepo),
            updateProfileUseCase = UpdateProfileUseCase(profileRepo),
            logoutUseCase = LogoutUseCase(sessionRepo),
            observeConnectivity = ObserveConnectivityUseCase(FakeNetworkMonitor(true)),
        )

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(ProfileAction.RequestLogout)
        assertTrue(vm.uiState.first().showLogoutDialog)

        vm.onAction(ProfileAction.ConfirmLogout)
        assertEquals(1, sessionRepo.logoutCalls)
        assertFalse(vm.uiState.first().showLogoutDialog)
        assertFalse(vm.uiState.first().isLoggingOut)
        collector.cancel()
    }

    @Test
    fun `perfil rechaza modo conductor sin vehiculo`() = runTest {
        val profileRepo = FakeProfileRepository(initialUser = sampleUser())
        val sessionRepo = FakeSessionRepository(initialLoggedIn = true)
        val vm = ProfileViewModel(
            observeCurrentUserUseCase = ObserveCurrentUserUseCase(profileRepo),
            refreshProfileUseCase = RefreshProfileUseCase(profileRepo),
            updateProfileUseCase = UpdateProfileUseCase(profileRepo),
            logoutUseCase = LogoutUseCase(sessionRepo),
            observeConnectivity = ObserveConnectivityUseCase(FakeNetworkMonitor(true)),
        )

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(ProfileAction.PreferredModeChanged(PreferredMode.CONDUCTOR))
        vm.onAction(ProfileAction.HasVehicleChanged(false))
        vm.onAction(ProfileAction.Save)

        val state = vm.uiState.first()
        assertEquals(R.string.auth_error_vehicle_required, state.vehicleRequiredErrorRes)
        assertEquals(null, profileRepo.lastUpdatedProfile)
        collector.cancel()
    }

    @Test
    fun `perfil rechaza telefono invalido y patente invalida`() = runTest {
        val profileRepo = FakeProfileRepository(initialUser = sampleUser())
        val sessionRepo = FakeSessionRepository(initialLoggedIn = true)
        val vm = ProfileViewModel(
            observeCurrentUserUseCase = ObserveCurrentUserUseCase(profileRepo),
            refreshProfileUseCase = RefreshProfileUseCase(profileRepo),
            updateProfileUseCase = UpdateProfileUseCase(profileRepo),
            logoutUseCase = LogoutUseCase(sessionRepo),
            observeConnectivity = ObserveConnectivityUseCase(FakeNetworkMonitor(true)),
        )

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(ProfileAction.PhoneChanged("abc"))
        vm.onAction(ProfileAction.HasVehicleChanged(true))
        vm.onAction(ProfileAction.VehicleBrandChanged("Toyota"))
        vm.onAction(ProfileAction.VehicleModelChanged("Etios"))
        vm.onAction(ProfileAction.VehicleColorChanged("Blanco"))
        vm.onAction(ProfileAction.VehiclePlateChanged("x"))
        vm.onAction(ProfileAction.Save)

        val state = vm.uiState.first()
        assertEquals(R.string.auth_error_phone_invalid, state.phoneErrorRes)
        assertEquals(R.string.auth_error_vehicle_plate_invalid, state.vehiclePlateErrorRes)
        assertEquals(null, profileRepo.lastUpdatedProfile)
        collector.cancel()
    }
}
