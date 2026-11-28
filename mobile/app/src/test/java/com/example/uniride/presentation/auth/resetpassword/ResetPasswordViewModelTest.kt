package com.example.uniride.presentation.auth.resetpassword

import androidx.lifecycle.SavedStateHandle
import com.example.uniride.R
import com.example.uniride.domain.repository.ConfirmPasswordResetOutcome
import com.example.uniride.domain.usecase.auth.ConfirmPasswordResetUseCase
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.fake.FakeNetworkMonitor
import com.example.uniride.fake.FakePasswordRecoveryRepository
import com.example.uniride.fake.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ResetPasswordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        repo: FakePasswordRecoveryRepository,
        code: String = "reset-code-123",
    ) = ResetPasswordViewModel(
        savedStateHandle = SavedStateHandle(mapOf(ResetPasswordViewModel.RESET_CODE_KEY to code)),
        confirmPasswordResetUseCase = ConfirmPasswordResetUseCase(repo),
        observeConnectivity = ObserveConnectivityUseCase(FakeNetworkMonitor(true)),
    )

    @Test
    fun `rechaza nueva contrasena sin numeros`() = runTest {
        val recoveryRepo = FakePasswordRecoveryRepository()
        val vm = createViewModel(recoveryRepo)

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(ResetPasswordAction.NewPasswordChanged("sinnumeros"))
        vm.onAction(ResetPasswordAction.ConfirmPasswordChanged("sinnumeros"))
        vm.onAction(ResetPasswordAction.Submit)

        assertEquals(R.string.auth_error_password_digit, vm.uiState.first().passwordErrorRes)
        assertNull(recoveryRepo.lastConfirmCode)
        collector.cancel()
    }

    @Test
    fun `rechaza confirmacion de contrasena distinta`() = runTest {
        val recoveryRepo = FakePasswordRecoveryRepository()
        val vm = createViewModel(recoveryRepo)

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(ResetPasswordAction.NewPasswordChanged("NuevaClave123"))
        vm.onAction(ResetPasswordAction.ConfirmPasswordChanged("OtraClave456"))
        vm.onAction(ResetPasswordAction.Submit)

        assertEquals(R.string.auth_error_password_mismatch, vm.uiState.first().confirmPasswordErrorRes)
        assertNull(recoveryRepo.lastConfirmCode)
        collector.cancel()
    }

    @Test
    fun `confirma cambio con el token del deep link y marca exito`() = runTest {
        val recoveryRepo = FakePasswordRecoveryRepository()
        val vm = createViewModel(recoveryRepo)

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(ResetPasswordAction.NewPasswordChanged("NuevaClave123"))
        vm.onAction(ResetPasswordAction.ConfirmPasswordChanged("NuevaClave123"))
        vm.onAction(ResetPasswordAction.Submit)

        assertTrue(vm.uiState.first().isSuccess)
        assertEquals("reset-code-123", recoveryRepo.lastConfirmCode)
        assertEquals("NuevaClave123", recoveryRepo.lastNewPassword)
        collector.cancel()
    }

    @Test
    fun `muestra error de contrasena cuando el servidor responde WEAK_PASSWORD y no enlace vencido`() = runTest {
        val recoveryRepo = FakePasswordRecoveryRepository().apply {
            confirmOutcome = ConfirmPasswordResetOutcome.WeakPassword
        }
        val vm = createViewModel(recoveryRepo)

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(ResetPasswordAction.NewPasswordChanged("NuevaClave123"))
        vm.onAction(ResetPasswordAction.ConfirmPasswordChanged("NuevaClave123"))
        vm.onAction(ResetPasswordAction.Submit)

        val state = vm.uiState.first()
        assertEquals(R.string.auth_reset_password_weak_server, state.passwordErrorRes)
        assertNull(state.generalErrorRes)
        collector.cancel()
    }

    @Test
    fun `muestra enlace vencido cuando el servidor responde INVALID_OR_EXPIRED_TOKEN`() = runTest {
        val recoveryRepo = FakePasswordRecoveryRepository().apply {
            confirmOutcome = ConfirmPasswordResetOutcome.InvalidOrExpiredCode
        }
        val vm = createViewModel(recoveryRepo)

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(ResetPasswordAction.NewPasswordChanged("NuevaClave123"))
        vm.onAction(ResetPasswordAction.ConfirmPasswordChanged("NuevaClave123"))
        vm.onAction(ResetPasswordAction.Submit)

        assertEquals(R.string.auth_reset_password_invalid_token, vm.uiState.first().generalErrorRes)
        collector.cancel()
    }
}
