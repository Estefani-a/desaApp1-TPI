package com.example.uniride.presentation.auth.checkemail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniride.R
import com.example.uniride.domain.usecase.auth.ResendActivationResult
import com.example.uniride.domain.usecase.auth.ResendActivationUseCase
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
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

/** A4 "Revisá tu correo" screen ViewModel (RF01). */
@HiltViewModel
class CheckEmailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resendActivationUseCase: ResendActivationUseCase,
    observeConnectivity: ObserveConnectivityUseCase,
) : ViewModel() {

    private val initialEmail: String = savedStateHandle.get<String>(EMAIL_KEY).orEmpty()

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

        formState.update { it.copy(isResending = true, emailErrorRes = null, feedbackMessageRes = null) }
        viewModelScope.launch {
            when (val result = resendActivationUseCase(current.email)) {
                ResendActivationResult.Sent -> formState.update {
                    it.copy(
                        isResending = false,
                        feedbackMessageRes = R.string.auth_check_email_resent,
                        isFeedbackError = false,
                    )
                }
                is ResendActivationResult.InvalidEmail -> formState.update {
                    it.copy(
                        isResending = false,
                        emailErrorRes = result.error.messageRes(),
                    )
                }
                is ResendActivationResult.Failed -> formState.update {
                    it.copy(
                        isResending = false,
                        feedbackMessageRes = result.error.messageRes(),
                        isFeedbackError = true,
                    )
                }
            }
        }
    }

    companion object {
        const val EMAIL_KEY = "email"
    }
}
