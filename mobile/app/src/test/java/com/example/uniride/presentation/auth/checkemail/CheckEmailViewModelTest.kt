package com.example.uniride.presentation.auth.checkemail

import androidx.lifecycle.SavedStateHandle
import com.example.uniride.R
import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.usecase.auth.ResendActivationUseCase
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.fake.FakeAccountRepository
import com.example.uniride.fake.FakeNetworkMonitor
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

class CheckEmailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `reenvia correo de activacion y muestra confirmacion`() = runTest {
        val accountRepo = FakeAccountRepository()
        val vm = CheckEmailViewModel(
            savedStateHandle = SavedStateHandle(mapOf(CheckEmailViewModel.EMAIL_KEY to "jperez@uade.edu.ar")),
            resendActivationUseCase = ResendActivationUseCase(accountRepo),
            observeConnectivity = ObserveConnectivityUseCase(FakeNetworkMonitor(true)),
        )

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(CheckEmailAction.ResendActivation)

        val state = vm.uiState.first()
        assertEquals("jperez@uade.edu.ar", accountRepo.lastResendEmail)
        assertEquals(R.string.auth_check_email_resent, state.feedbackMessageRes)
        assertFalse(state.isFeedbackError)
        collector.cancel()
    }

    @Test
    fun `rechaza reenvio con correo fuera de dominio UADE`() = runTest {
        val accountRepo = FakeAccountRepository()
        val vm = CheckEmailViewModel(
            savedStateHandle = SavedStateHandle(mapOf(CheckEmailViewModel.EMAIL_KEY to "alumno@gmail.com")),
            resendActivationUseCase = ResendActivationUseCase(accountRepo),
            observeConnectivity = ObserveConnectivityUseCase(FakeNetworkMonitor(true)),
        )

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(CheckEmailAction.ResendActivation)

        assertEquals(R.string.auth_error_email_domain, vm.uiState.first().emailErrorRes)
        assertNull(accountRepo.lastResendEmail)
        collector.cancel()
    }

    @Test
    fun `muestra error de red si falla el reenvio`() = runTest {
        val accountRepo = FakeAccountRepository().apply {
            resendResult = AppResult.Failure(DataError.NO_CONNECTION)
        }
        val vm = CheckEmailViewModel(
            savedStateHandle = SavedStateHandle(mapOf(CheckEmailViewModel.EMAIL_KEY to "jperez@uade.edu.ar")),
            resendActivationUseCase = ResendActivationUseCase(accountRepo),
            observeConnectivity = ObserveConnectivityUseCase(FakeNetworkMonitor(false)),
        )

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(CheckEmailAction.ResendActivation)

        val state = vm.uiState.first()
        assertEquals(R.string.common_error_no_connection, state.feedbackMessageRes)
        assertTrue(state.isFeedbackError)
        assertTrue(state.isOffline)
        collector.cancel()
    }
}
