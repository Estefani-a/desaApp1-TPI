package com.example.uniride.presentation.auth.register

import app.cash.turbine.test
import com.example.uniride.R
import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.usecase.auth.RegisterUseCase
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.fake.FakeAccountRepository
import com.example.uniride.fake.FakeNetworkMonitor
import com.example.uniride.fake.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `registro valida dominio, contrasena, confirmacion y campos obligatorios`() = runTest {
        val accountRepo = FakeAccountRepository()
        val vm = RegisterViewModel(
            registerUseCase = RegisterUseCase(accountRepo),
            observeConnectivity = ObserveConnectivityUseCase(FakeNetworkMonitor(true)),
        )

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(RegisterAction.EmailChanged("alumno@hotmail.com"))
        vm.onAction(RegisterAction.PasswordChanged("corta"))
        vm.onAction(RegisterAction.ConfirmPasswordChanged("otra1234"))
        vm.onAction(RegisterAction.PreferredModeChanged(PreferredMode.CONDUCTOR))
        vm.onAction(RegisterAction.Submit)

        val state = vm.uiState.first()
        assertEquals(R.string.auth_error_email_domain, state.emailErrorRes)
        assertEquals(R.string.auth_error_password_short, state.passwordErrorRes)
        assertEquals(R.string.auth_error_password_mismatch, state.confirmPasswordErrorRes)
        assertEquals(R.string.auth_error_full_name_blank, state.fullNameErrorRes)
        assertTrue(state.hasVehicle)
        assertEquals(R.string.auth_error_vehicle_plate_blank, state.vehiclePlateErrorRes)
        assertNull(accountRepo.lastRegisteredEmail)
        collector.cancel()
    }

    @Test
    fun `registro exitoso emite NavigateToCheckEmail con el correo normalizado`() = runTest {
        val accountRepo = FakeAccountRepository()
        val vm = RegisterViewModel(
            registerUseCase = RegisterUseCase(accountRepo),
            observeConnectivity = ObserveConnectivityUseCase(FakeNetworkMonitor(true)),
        )

        vm.effects.test {
            vm.onAction(RegisterAction.EmailChanged("  JPEREZ@UADE.EDU.AR "))
            vm.onAction(RegisterAction.PasswordChanged("Secreta123"))
            vm.onAction(RegisterAction.ConfirmPasswordChanged("Secreta123"))
            vm.onAction(RegisterAction.FullNameChanged("Juan Pérez"))
            vm.onAction(RegisterAction.CareerChanged("Ingeniería en Informática"))
            vm.onAction(RegisterAction.PhoneChanged("+5491144445555"))
            vm.onAction(RegisterAction.Submit)

            assertEquals(RegisterEffect.NavigateToCheckEmail("jperez@uade.edu.ar"), awaitItem())
            assertEquals("jperez@uade.edu.ar", accountRepo.lastRegisteredEmail)
        }
    }

    @Test
    fun `registro con correo duplicado muestra error especifico`() = runTest {
        val accountRepo = FakeAccountRepository().apply {
            registerResult = AppResult.Failure(DataError.CONFLICT)
        }
        val vm = RegisterViewModel(
            registerUseCase = RegisterUseCase(accountRepo),
            observeConnectivity = ObserveConnectivityUseCase(FakeNetworkMonitor(true)),
        )

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(RegisterAction.EmailChanged("lfernandez@uade.edu.ar"))
        vm.onAction(RegisterAction.PasswordChanged("Secreta123"))
        vm.onAction(RegisterAction.ConfirmPasswordChanged("Secreta123"))
        vm.onAction(RegisterAction.FullNameChanged("Lucía Fernández"))
        vm.onAction(RegisterAction.CareerChanged("Ingeniería"))
        vm.onAction(RegisterAction.PhoneChanged("+5491155550001"))
        vm.onAction(RegisterAction.Submit)

        assertEquals(R.string.auth_register_email_exists, vm.uiState.first().generalErrorRes)
        collector.cancel()
    }

    @Test
    fun `registro rechaza modo conductor sin vehiculo y telefono invalido`() = runTest {
        val accountRepo = FakeAccountRepository()
        val vm = RegisterViewModel(
            registerUseCase = RegisterUseCase(accountRepo),
            observeConnectivity = ObserveConnectivityUseCase(FakeNetworkMonitor(true)),
        )

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(RegisterAction.EmailChanged("jperez@uade.edu.ar"))
        vm.onAction(RegisterAction.PasswordChanged("Secreta123"))
        vm.onAction(RegisterAction.ConfirmPasswordChanged("Secreta123"))
        vm.onAction(RegisterAction.FullNameChanged("Juan Pérez"))
        vm.onAction(RegisterAction.CareerChanged("Ingeniería"))
        vm.onAction(RegisterAction.PhoneChanged("hola"))
        vm.onAction(RegisterAction.PreferredModeChanged(PreferredMode.CONDUCTOR))
        vm.onAction(RegisterAction.HasVehicleChanged(false))
        vm.onAction(RegisterAction.Submit)

        val state = vm.uiState.first()
        assertEquals(R.string.auth_error_phone_invalid, state.phoneErrorRes)
        assertEquals(R.string.auth_error_vehicle_required, state.vehicleRequiredErrorRes)
        assertNull(accountRepo.lastRegisteredEmail)
        collector.cancel()
    }

    @Test
    fun `registro rechaza patente con formato invalido`() = runTest {
        val accountRepo = FakeAccountRepository()
        val vm = RegisterViewModel(
            registerUseCase = RegisterUseCase(accountRepo),
            observeConnectivity = ObserveConnectivityUseCase(FakeNetworkMonitor(true)),
        )

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(RegisterAction.EmailChanged("jperez@uade.edu.ar"))
        vm.onAction(RegisterAction.PasswordChanged("Secreta123"))
        vm.onAction(RegisterAction.ConfirmPasswordChanged("Secreta123"))
        vm.onAction(RegisterAction.FullNameChanged("Juan Pérez"))
        vm.onAction(RegisterAction.CareerChanged("Ingeniería"))
        vm.onAction(RegisterAction.PhoneChanged("+5491144445555"))
        vm.onAction(RegisterAction.HasVehicleChanged(true))
        vm.onAction(RegisterAction.VehicleBrandChanged("Ford"))
        vm.onAction(RegisterAction.VehicleModelChanged("Ka"))
        vm.onAction(RegisterAction.VehicleColorChanged("Rojo"))
        vm.onAction(RegisterAction.VehiclePlateChanged("AB123!"))
        vm.onAction(RegisterAction.Submit)

        val state = vm.uiState.first()
        assertEquals(R.string.auth_error_vehicle_plate_invalid, state.vehiclePlateErrorRes)
        assertNull(accountRepo.lastRegisteredEmail)
        collector.cancel()
    }
}
