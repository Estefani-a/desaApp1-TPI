package com.example.uniride.presentation.profile

import com.example.uniride.R
import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.model.Rating
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
import com.example.uniride.presentation.common.ContentState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        profileRepo: FakeProfileRepository,
        sessionRepo: FakeSessionRepository = FakeSessionRepository(initialLoggedIn = true),
        online: Boolean = true,
    ) = ProfileViewModel(
        observeCurrentUserUseCase = ObserveCurrentUserUseCase(profileRepo),
        refreshProfileUseCase = RefreshProfileUseCase(profileRepo),
        updateProfileUseCase = UpdateProfileUseCase(profileRepo),
        logoutUseCase = LogoutUseCase(sessionRepo),
        observeConnectivity = ObserveConnectivityUseCase(FakeNetworkMonitor(online)),
    )

    @Test
    fun `perfil carga usuario cacheado sin conexion aunque falle el refresh`() = runTest {
        val profileRepo = FakeProfileRepository(initialUser = sampleUser()).apply {
            refreshResult = AppResult.Failure(DataError.NO_CONNECTION)
        }
        val vm = createViewModel(profileRepo = profileRepo, online = false)

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        val state = vm.uiState.first()
        val data = (state.content as ContentState.Success).data
        assertEquals("lfernandez@uade.edu.ar", data.email)
        assertEquals("Lucía Fernández", data.form.fullName)
        assertTrue(state.isOffline)
        collector.cancel()
    }

    @Test
    fun `perfil muestra estado Error si no hay usuario cacheado y falla el refresh`() = runTest {
        val profileRepo = FakeProfileRepository(initialUser = null).apply {
            refreshResult = AppResult.Failure(DataError.NO_CONNECTION)
        }
        val vm = createViewModel(profileRepo = profileRepo, online = false)

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        val state = vm.uiState.first()
        assertEquals(ContentState.Error(R.string.common_error_no_connection, canRetry = true), state.content)
        collector.cancel()
    }

    @Test
    fun `perfil muestra estado Empty si el refresh termina sin usuario en Room`() = runTest {
        val profileRepo = FakeProfileRepository(initialUser = null)
        val vm = createViewModel(profileRepo = profileRepo)

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        assertEquals(ContentState.Empty, vm.uiState.first().content)
        collector.cancel()
    }

    @Test
    fun `perfil guarda cambios con vehiculo y muestra confirmacion`() = runTest {
        val profileRepo = FakeProfileRepository(initialUser = sampleUser())
        val vm = createViewModel(profileRepo = profileRepo)

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(ProfileAction.HasVehicleChanged(true))
        vm.onAction(ProfileAction.VehicleBrandChanged("Toyota"))
        vm.onAction(ProfileAction.VehicleModelChanged("Etios"))
        vm.onAction(ProfileAction.VehicleColorChanged("Blanco"))
        vm.onAction(ProfileAction.VehiclePlateChanged("af 123-cd"))
        vm.onAction(ProfileAction.Save)

        val state = vm.uiState.first()
        assertEquals(R.string.profile_saved_message, state.feedbackMessageRes)
        assertFalse(state.isFeedbackError)
        assertEquals("AF123CD", profileRepo.lastUpdatedProfile?.vehicle?.plate)
        collector.cancel()
    }

    @Test
    fun `perfil conserva ediciones sin guardar cuando Room emite un usuario actualizado`() = runTest {
        val initialUser = sampleUser()
        val profileRepo = FakeProfileRepository(initialUser = initialUser)
        val vm = createViewModel(profileRepo = profileRepo)

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(ProfileAction.FullNameChanged("Lucía Editada"))

        profileRepo.currentUser.value = initialUser.copy(
            rating = Rating(average = 5.0, count = 20),
            profile = initialUser.profile.copy(fullName = "Nombre Desde Servidor"),
        )

        val data = (vm.uiState.first().content as ContentState.Success).data
        assertEquals("Lucía Editada", data.form.fullName)
        assertEquals(Rating(average = 5.0, count = 20), data.rating)
        collector.cancel()
    }

    @Test
    fun `confirmar logout llama a logoutUseCase y cierra el dialogo`() = runTest {
        val profileRepo = FakeProfileRepository(initialUser = sampleUser())
        val sessionRepo = FakeSessionRepository(initialLoggedIn = true)
        val vm = createViewModel(profileRepo = profileRepo, sessionRepo = sessionRepo)

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
        val vm = createViewModel(profileRepo = profileRepo)

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(ProfileAction.PreferredModeChanged(PreferredMode.CONDUCTOR))
        vm.onAction(ProfileAction.HasVehicleChanged(false))
        vm.onAction(ProfileAction.Save)

        val data = (vm.uiState.first().content as ContentState.Success).data
        assertEquals(R.string.auth_error_vehicle_required, data.form.vehicleRequiredErrorRes)
        assertNull(profileRepo.lastUpdatedProfile)
        collector.cancel()
    }

    @Test
    fun `perfil rechaza telefono invalido`() = runTest {
        val profileRepo = FakeProfileRepository(initialUser = sampleUser())
        val vm = createViewModel(profileRepo = profileRepo)

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(ProfileAction.PhoneChanged("abc"))
        vm.onAction(ProfileAction.Save)

        val data = (vm.uiState.first().content as ContentState.Success).data
        assertEquals(R.string.auth_error_phone_invalid, data.form.phoneErrorRes)
        assertNull(profileRepo.lastUpdatedProfile)
        collector.cancel()
    }

    @Test
    fun `perfil rechaza patente invalida`() = runTest {
        val profileRepo = FakeProfileRepository(initialUser = sampleUser())
        val vm = createViewModel(profileRepo = profileRepo)

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(ProfileAction.HasVehicleChanged(true))
        vm.onAction(ProfileAction.VehicleBrandChanged("Toyota"))
        vm.onAction(ProfileAction.VehicleModelChanged("Etios"))
        vm.onAction(ProfileAction.VehicleColorChanged("Blanco"))
        vm.onAction(ProfileAction.VehiclePlateChanged("x"))
        vm.onAction(ProfileAction.Save)

        val data = (vm.uiState.first().content as ContentState.Success).data
        assertEquals(R.string.auth_error_vehicle_plate_invalid, data.form.vehiclePlateErrorRes)
        assertNull(profileRepo.lastUpdatedProfile)
        collector.cancel()
    }
}
