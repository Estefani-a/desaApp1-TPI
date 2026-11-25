package com.example.uniride.presentation.auth.resetpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniride.core.result.AppResult
import com.example.uniride.domain.usecase.auth.RequestPasswordResetUseCase
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.domain.validation.EmailValidationResult
import com.example.uniride.domain.validation.EmailValidator
import com.example.uniride.presentation.auth.components.messageRes
import com.example.uniride.presentation.common.messageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Password recovery request ViewModel (RF01). */
@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val requestPasswordResetUseCase: RequestPasswordResetUseCase,
    observeConnectivity: ObserveConnectivityUseCase,
) : ViewModel() {

    private val formState = MutableStateFlow(ForgotPasswordUiState())

    val uiState: StateFlow<ForgotPasswordUiState> = combine(
        formState,
        observeConnectivity(),
    ) { state, isOnline ->
        state.copy(isOffline = !isOnline)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ForgotPasswordUiState(),
    )

    fun onAction(action: ForgotPasswordAction) {
        when (action) {
            is ForgotPasswordAction.EmailChanged -> formState.update {
                it.copy(email = action.value, emailErrorRes = null, generalErrorRes = null)
            }
            ForgotPasswordAction.Submit -> submit()
        }
    }

    private fun submit() {
        val current = formState.value
        if (current.isSubmitting) return

        val emailError = when (val res = EmailValidator.validate(current.email)) {
            EmailValidationResult.Valid -> null
            is EmailValidationResult.Invalid -> res.error.messageRes()
        }
        if (emailError != null) {
            formState.update { it.copy(emailErrorRes = emailError) }
            return
        }

        formState.update { it.copy(isSubmitting = true, generalErrorRes = null) }
        viewModelScope.launch {
            when (val result = requestPasswordResetUseCase(current.email)) {
                is AppResult.Success -> formState.update {
                    it.copy(isSubmitting = false, isSent = true)
                }
                is AppResult.Failure -> formState.update {
                    it.copy(isSubmitting = false, generalErrorRes = result.error.messageRes())
                }
            }
        }
    }
}
