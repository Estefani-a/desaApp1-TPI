package com.example.uniride.presentation.auth.resetpassword

import com.example.uniride.R
import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.usecase.auth.RequestPasswordResetUseCase
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.fake.FakeNetworkMonitor
import com.example.uniride.fake.FakePasswordRecoveryRepository
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

class ForgotPasswordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `solicita recuperacion y marca estado enviado`() = runTest {
        val recoveryRepo = FakePasswordRecoveryRepository()
        val vm = ForgotPasswordViewModel(
            requestPasswordResetUseCase = RequestPasswordResetUseCase(recoveryRepo),
            observeConnectivity = ObserveConnectivityUseCase(FakeNetworkMonitor(true)),
        )

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(ForgotPasswordAction.EmailChanged("lfernandez@uade.edu.ar"))
        vm.onAction(ForgotPasswordAction.Submit)

        assertTrue(vm.uiState.first().isSent)
        assertEquals("lfernandez@uade.edu.ar", recoveryRepo.lastResetEmail)
        collector.cancel()
    }

    @Test
    fun `rechaza correo no institucional sin llamar al repositorio`() = runTest {
        val recoveryRepo = FakePasswordRecoveryRepository()
        val vm = ForgotPasswordViewModel(
            requestPasswordResetUseCase = RequestPasswordResetUseCase(recoveryRepo),
            observeConnectivity = ObserveConnectivityUseCase(FakeNetworkMonitor(true)),
        )

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(ForgotPasswordAction.EmailChanged("alumno@gmail.com"))
        vm.onAction(ForgotPasswordAction.Submit)

        assertEquals(R.string.auth_error_email_domain, vm.uiState.first().emailErrorRes)
        assertNull(recoveryRepo.lastResetEmail)
        collector.cancel()
    }

    @Test
    fun `muestra error de red si falla el envio`() = runTest {
        val recoveryRepo = FakePasswordRecoveryRepository().apply {
            requestResult = AppResult.Failure(DataError.TIMEOUT)
        }
        val vm = ForgotPasswordViewModel(
            requestPasswordResetUseCase = RequestPasswordResetUseCase(recoveryRepo),
            observeConnectivity = ObserveConnectivityUseCase(FakeNetworkMonitor(true)),
        )

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(ForgotPasswordAction.EmailChanged("lfernandez@uade.edu.ar"))
        vm.onAction(ForgotPasswordAction.Submit)

        val state = vm.uiState.first()
        assertFalse(state.isSent)
        assertEquals(R.string.common_error_timeout, state.generalErrorRes)
        collector.cancel()
    }
}
