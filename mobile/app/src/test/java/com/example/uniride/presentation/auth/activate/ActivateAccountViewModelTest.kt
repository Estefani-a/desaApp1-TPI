package com.example.uniride.presentation.auth.activate

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.uniride.R
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.repository.ActivateOutcome
import com.example.uniride.domain.usecase.auth.ActivateAccountUseCase
import com.example.uniride.fake.FakeAccountRepository
import com.example.uniride.fake.MainDispatcherRule
import com.example.uniride.fake.sampleUser
import com.example.uniride.presentation.common.ContentState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ActivateAccountViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `activa automaticamente con el codigo del deep link, muestra Success y navega al inicio`() = runTest {
        val accountRepo = FakeAccountRepository()
        val vm = ActivateAccountViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(ActivateAccountViewModel.ACTIVATION_CODE_KEY to "token-activacion-1"),
            ),
            activateAccountUseCase = ActivateAccountUseCase(accountRepo),
        )

        vm.effects.test {
            assertEquals(ActivateAccountEffect.NavigateToMain, awaitItem())
            assertEquals("token-activacion-1", accountRepo.lastActivatedCode)
            assertEquals(ContentState.Success(Unit), vm.uiState.value.content)
        }
    }

    @Test
    fun `no vuelve a consumir el codigo de un solo uso si se recrea el ViewModel tras activar`() = runTest {
        val accountRepo = FakeAccountRepository()
        val savedState = SavedStateHandle(
            mapOf(
                ActivateAccountViewModel.ACTIVATION_CODE_KEY to "token-ya-usado",
                ActivateAccountViewModel.ATTEMPTED_KEY to true,
                ActivateAccountViewModel.ACTIVATED_KEY to true,
            ),
        )

        val vm = ActivateAccountViewModel(
            savedStateHandle = savedState,
            activateAccountUseCase = ActivateAccountUseCase(accountRepo),
        )

        assertEquals(0, accountRepo.activateCalls)
        assertEquals(ContentState.Success(Unit), vm.uiState.value.content)
    }

    @Test
    fun `con codigo vacio muestra error no reintentable sin llamar al repositorio`() = runTest {
        val accountRepo = FakeAccountRepository()
        val vm = ActivateAccountViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(ActivateAccountViewModel.ACTIVATION_CODE_KEY to "   "),
            ),
            activateAccountUseCase = ActivateAccountUseCase(accountRepo),
        )

        assertEquals(
            ContentState.Error(R.string.auth_activate_invalid_token, canRetry = false),
            vm.uiState.value.content,
        )
        assertNull(accountRepo.lastActivatedCode)
    }

    @Test
    fun `con codigo vencido del servidor muestra error no reintentable`() = runTest {
        val accountRepo = FakeAccountRepository().apply {
            activateOutcome = ActivateOutcome.InvalidOrExpiredCode
        }
        val vm = ActivateAccountViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(ActivateAccountViewModel.ACTIVATION_CODE_KEY to "token-vencido"),
            ),
            activateAccountUseCase = ActivateAccountUseCase(accountRepo),
        )

        assertEquals(
            ContentState.Error(R.string.auth_activate_invalid_token, canRetry = false),
            vm.uiState.value.content,
        )
    }

    @Test
    fun `muestra error reintentable si falla por red y permite reintentar`() = runTest {
        val accountRepo = FakeAccountRepository().apply {
            activateOutcome = ActivateOutcome.Failed(DataError.NO_CONNECTION)
        }
        val vm = ActivateAccountViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(ActivateAccountViewModel.ACTIVATION_CODE_KEY to "token-activacion-2"),
            ),
            activateAccountUseCase = ActivateAccountUseCase(accountRepo),
        )

        assertEquals(
            ContentState.Error(R.string.common_error_no_connection, canRetry = true),
            vm.uiState.value.content,
        )

        accountRepo.activateOutcome = ActivateOutcome.Activated(sampleUser())
        vm.effects.test {
            vm.onAction(ActivateAccountAction.Retry)
            assertEquals(ActivateAccountEffect.NavigateToMain, awaitItem())
        }
    }
}
