package com.example.uniride.presentation.auth

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.uniride.R
import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.usecase.auth.ActivateAccountUseCase
import com.example.uniride.domain.usecase.auth.ConfirmPasswordResetUseCase
import com.example.uniride.domain.usecase.auth.ObserveIsLoggedInUseCase
import com.example.uniride.domain.usecase.auth.RequestPasswordResetUseCase
import com.example.uniride.domain.usecase.auth.ResendActivationUseCase
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.domain.usecase.profile.ObserveCurrentUserUseCase
import com.example.uniride.fake.FakeAccountRepository
import com.example.uniride.fake.FakeNetworkMonitor
import com.example.uniride.fake.FakePasswordRecoveryRepository
import com.example.uniride.fake.FakeProfileRepository
import com.example.uniride.fake.FakeSessionRepository
import com.example.uniride.fake.MainDispatcherRule
import com.example.uniride.fake.sampleUser
import com.example.uniride.presentation.auth.activate.ActivateAccountAction
import com.example.uniride.presentation.auth.activate.ActivateAccountEffect
import com.example.uniride.presentation.auth.activate.ActivateAccountViewModel
import com.example.uniride.presentation.auth.checkemail.CheckEmailAction
import com.example.uniride.presentation.auth.checkemail.CheckEmailViewModel
import com.example.uniride.presentation.auth.resetpassword.ForgotPasswordAction
import com.example.uniride.presentation.auth.resetpassword.ForgotPasswordViewModel
import com.example.uniride.presentation.auth.resetpassword.ResetPasswordAction
import com.example.uniride.presentation.auth.resetpassword.ResetPasswordViewModel
import com.example.uniride.presentation.navigation.AppSessionUiState
import com.example.uniride.presentation.navigation.AppSessionViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthFlowsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `CheckEmail reenvia correo de activacion y muestra confirmacion`() = runTest {
        val accountRepo = FakeAccountRepository()
        val vm = CheckEmailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("email" to "jperez@uade.edu.ar")),
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
    fun `ActivateAccount activa automaticamente con el codigo del deep link y navega al inicio`() = runTest {
        val accountRepo = FakeAccountRepository()
        val vm = ActivateAccountViewModel(
            savedStateHandle = SavedStateHandle(mapOf("activationCode" to "token-activacion-1")),
            activateAccountUseCase = ActivateAccountUseCase(accountRepo),
        )

        vm.effects.test {
            assertEquals(ActivateAccountEffect.NavigateToMain, awaitItem())
            assertEquals("token-activacion-1", accountRepo.lastActivatedCode)
        }
    }

    @Test
    fun `ActivateAccount con codigo vacio muestra error no reintentable sin llamar al repositorio`() = runTest {
        val accountRepo = FakeAccountRepository()
        val vm = ActivateAccountViewModel(
            savedStateHandle = SavedStateHandle(mapOf("activationCode" to "   ")),
            activateAccountUseCase = ActivateAccountUseCase(accountRepo),
        )

        assertEquals(R.string.auth_activate_invalid_token, vm.uiState.value.errorMessageRes)
        assertFalse(vm.uiState.value.canRetry)
        assertEquals(null, accountRepo.lastActivatedCode)
    }

    @Test
    fun `ActivateAccount muestra error reintentable si falla por red y permite reintentar`() = runTest {
        val accountRepo = FakeAccountRepository().apply {
            activateResult = AppResult.Failure(DataError.NO_CONNECTION)
        }
        val vm = ActivateAccountViewModel(
            savedStateHandle = SavedStateHandle(mapOf("activationCode" to "token-activacion-2")),
            activateAccountUseCase = ActivateAccountUseCase(accountRepo),
        )

        assertEquals(R.string.common_error_no_connection, vm.uiState.value.errorMessageRes)
        assertTrue(vm.uiState.value.canRetry)

        accountRepo.activateResult = AppResult.Success(sampleUser())
        vm.effects.test {
            vm.onAction(ActivateAccountAction.Retry)
            assertEquals(ActivateAccountEffect.NavigateToMain, awaitItem())
        }
    }

    @Test
    fun `ForgotPassword solicita recuperacion y marca estado enviado`() = runTest {
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
    fun `ResetPassword valida politica de clave y confirma cambio con el token del deep link`() = runTest {
        val recoveryRepo = FakePasswordRecoveryRepository()
        val vm = ResetPasswordViewModel(
            savedStateHandle = SavedStateHandle(mapOf("resetCode" to "reset-code-123")),
            confirmPasswordResetUseCase = ConfirmPasswordResetUseCase(recoveryRepo),
            observeConnectivity = ObserveConnectivityUseCase(FakeNetworkMonitor(true)),
        )

        val collector = backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(ResetPasswordAction.NewPasswordChanged("sinnumeros"))
        vm.onAction(ResetPasswordAction.ConfirmPasswordChanged("sinnumeros"))
        vm.onAction(ResetPasswordAction.Submit)
        assertEquals(R.string.auth_error_password_digit, vm.uiState.first().passwordErrorRes)

        vm.onAction(ResetPasswordAction.NewPasswordChanged("NuevaClave123"))
        vm.onAction(ResetPasswordAction.ConfirmPasswordChanged("NuevaClave123"))
        vm.onAction(ResetPasswordAction.Submit)

        assertTrue(vm.uiState.first().isSuccess)
        assertEquals("reset-code-123", recoveryRepo.lastConfirmCode)
        assertEquals("NuevaClave123", recoveryRepo.lastNewPassword)
        collector.cancel()
    }

    @Test
    fun `AppSessionViewModel emite Ready con sesion activa y perfil`() = runTest {
        val user = sampleUser()
        val vm = AppSessionViewModel(
            observeIsLoggedIn = ObserveIsLoggedInUseCase(FakeSessionRepository(initialLoggedIn = true)),
            observeCurrentUser = ObserveCurrentUserUseCase(FakeProfileRepository(initialUser = user)),
        )

        vm.uiState.test {
            val state = awaitItem()
            val ready = if (state is AppSessionUiState.Loading) awaitItem() else state
            assertEquals(AppSessionUiState.Ready(isLoggedIn = true, profile = user.profile), ready)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
