package com.example.uniride.presentation.auth.checkemail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.uniride.R
import com.example.uniride.core.result.AppResult
import com.example.uniride.domain.usecase.auth.ResendActivationUseCase
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.domain.validation.EmailValidationResult
import com.example.uniride.domain.validation.EmailValidator
import com.example.uniride.presentation.auth.CheckEmailRoute
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

/** "Revisá tu correo" screen ViewModel (RF01). */
@HiltViewModel
class CheckEmailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resendActivationUseCase: ResendActivationUseCase,
    observeConnectivity: ObserveConnectivityUseCase,
) : ViewModel() {

    private val initialEmail: String = runCatching {
        savedStateHandle.toRoute<CheckEmailRoute>().email
    }.getOrElse {
        savedStateHandle.get<String>("email").orEmpty()
    }

    private val formState = MutableStateFlow(CheckEmailUiState(email = initialEmail))

    val uiState: StateFlow<CheckEmailUiState> = combine(
        formState,
        observeConnectivity(),
    ) { state, isOnline ->
        state.copy(isOffline = !isOnline)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CheckEmailUiState(email = initialEmail),
    )

    fun onAction(action: CheckEmailAction) {
        when (action) {
            is CheckEmailAction.EmailChanged -> formState.update {
                it.copy(email = action.value, emailErrorRes = null, feedbackMessageRes = null)
            }
            CheckEmailAction.ResendActivation -> resend()
        }
    }

    private fun resend() {
        val current = formState.value
        if (current.isResending) return

        val emailError = when (val res = EmailValidator.validate(current.email)) {
            EmailValidationResult.Valid -> null
            is EmailValidationResult.Invalid -> res.error.messageRes()
        }
        if (emailError != null) {
            formState.update { it.copy(emailErrorRes = emailError) }
            return
        }

        formState.update { it.copy(isResending = true, feedbackMessageRes = null) }
        viewModelScope.launch {
            when (val result = resendActivationUseCase(current.email)) {
                is AppResult.Success -> formState.update {
                    it.copy(
                        isResending = false,
                        feedbackMessageRes = R.string.auth_check_email_resent,
                        isFeedbackError = false,
                    )
                }
                is AppResult.Failure -> formState.update {
                    it.copy(
                        isResending = false,
                        feedbackMessageRes = result.error.messageRes(),
                        isFeedbackError = true,
                    )
                }
            }
        }
    }
}
