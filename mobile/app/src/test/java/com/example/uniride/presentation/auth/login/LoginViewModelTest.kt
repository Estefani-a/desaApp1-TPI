package com.example.uniride.presentation.auth.login

import app.cash.turbine.test
import com.example.uniride.R
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.repository.LoginOutcome
import com.example.uniride.domain.usecase.auth.LoginUseCase
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.fake.FakeNetworkMonitor
import com.example.uniride.fake.FakeSessionRepository
import com.example.uniride.fake.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `login rechaza correo no institucional y clave vacia sin llamar al repositorio`() = runTest {
        val sessionRepo = FakeSessionRepository()
        val vm = LoginViewModel(
            loginUseCase = LoginUseCase(sessionRepo),
            observeConnectivity = ObserveConnectivityUseCase(FakeNetworkMonitor(true)),
        )

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(LoginAction.EmailChanged("alumno@gmail.com"))
        vm.onAction(LoginAction.Submit)

        val state = vm.uiState.first()
        assertEquals(R.string.auth_error_email_domain, state.emailErrorRes)
        assertEquals(R.string.auth_error_password_blank, state.passwordErrorRes)
        assertNull(sessionRepo.lastLoginEmail)
        collector.cancel()
    }

    @Test
    fun `login exitoso emite NavigateToMain y normaliza el correo`() = runTest {
        val sessionRepo = FakeSessionRepository()
        val vm = LoginViewModel(
            loginUseCase = LoginUseCase(sessionRepo),
            observeConnectivity = ObserveConnectivityUseCase(FakeNetworkMonitor(true)),
        )

        vm.effects.test {
            vm.onAction(LoginAction.EmailChanged("  LFERNANDEZ@UADE.EDU.AR "))
            vm.onAction(LoginAction.PasswordChanged("Secreta123"))
            vm.onAction(LoginAction.Submit)

            assertEquals(LoginEffect.NavigateToMain, awaitItem())
            assertEquals("lfernandez@uade.edu.ar", sessionRepo.lastLoginEmail)
        }
    }

    @Test
    fun `login con credenciales invalidas muestra error sin boton de reenvio`() = runTest {
        val sessionRepo = FakeSessionRepository().apply {
            loginOutcome = LoginOutcome.InvalidCredentials
        }
        val vm = LoginViewModel(
            loginUseCase = LoginUseCase(sessionRepo),
            observeConnectivity = ObserveConnectivityUseCase(FakeNetworkMonitor(false)),
        )

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(LoginAction.EmailChanged("lfernandez@uade.edu.ar"))
        vm.onAction(LoginAction.PasswordChanged("ClaveIncorrecta9"))
        vm.onAction(LoginAction.Submit)

        val state = vm.uiState.first()
        assertEquals(R.string.auth_login_invalid_credentials, state.generalErrorRes)
        assertFalse(state.showResendActivation)
        assertFalse(state.isSubmitting)
        assertTrue(state.isOffline)
        collector.cancel()
    }

    @Test
    fun `login con cuenta pendiente muestra aviso y habilita reenvio de activacion`() = runTest {
        val sessionRepo = FakeSessionRepository().apply {
            loginOutcome = LoginOutcome.AccountNotActivated
        }
        val vm = LoginViewModel(
            loginUseCase = LoginUseCase(sessionRepo),
            observeConnectivity = ObserveConnectivityUseCase(FakeNetworkMonitor(true)),
        )

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(LoginAction.EmailChanged("pendiente@uade.edu.ar"))
        vm.onAction(LoginAction.PasswordChanged("Secreta123"))
        vm.onAction(LoginAction.Submit)

        val state = vm.uiState.first()
        assertEquals(R.string.auth_login_account_not_activated, state.generalErrorRes)
        assertTrue(state.showResendActivation)
        collector.cancel()
    }

    @Test
    fun `login propaga error de red en generalErrorRes`() = runTest {
        val sessionRepo = FakeSessionRepository().apply {
            loginOutcome = LoginOutcome.Failed(DataError.NO_CONNECTION)
        }
        val vm = LoginViewModel(
            loginUseCase = LoginUseCase(sessionRepo),
            observeConnectivity = ObserveConnectivityUseCase(FakeNetworkMonitor(false)),
        )

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(LoginAction.EmailChanged("lfernandez@uade.edu.ar"))
        vm.onAction(LoginAction.PasswordChanged("Secreta123"))
        vm.onAction(LoginAction.Submit)

        assertEquals(R.string.common_error_no_connection, vm.uiState.first().generalErrorRes)
        collector.cancel()
    }
}
