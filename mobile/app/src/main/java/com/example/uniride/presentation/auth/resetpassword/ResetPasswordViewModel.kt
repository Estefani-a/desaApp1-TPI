package com.example.uniride.presentation.auth.resetpassword

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.uniride.R
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.usecase.auth.ConfirmPasswordResetResult
import com.example.uniride.domain.usecase.auth.ConfirmPasswordResetUseCase
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.domain.validation.PasswordPolicy
import com.example.uniride.domain.validation.PasswordValidationResult
import com.example.uniride.presentation.auth.ResetPasswordRoute
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

/** Handles `uniride://reset-password?token=...` deep link confirmation (RF01). */
@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val confirmPasswordResetUseCase: ConfirmPasswordResetUseCase,
    observeConnectivity: ObserveConnectivityUseCase,
) : ViewModel() {

    private val resetCode: String = runCatching {
        savedStateHandle.toRoute<ResetPasswordRoute>().resetCode
    }.getOrElse {
        savedStateHandle.get<String>("resetCode").orEmpty()
    }

    private val formState = MutableStateFlow(ResetPasswordUiState())

    val uiState: StateFlow<ResetPasswordUiState> = combine(
        formState,
        observeConnectivity(),
    ) { state, isOnline ->
        state.copy(isOffline = !isOnline)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ResetPasswordUiState(),
    )

    fun onAction(action: ResetPasswordAction) {
        when (action) {
            is ResetPasswordAction.NewPasswordChanged -> formState.update {
                it.copy(newPassword = action.value, passwordErrorRes = null, generalErrorRes = null)
            }
            is ResetPasswordAction.ConfirmPasswordChanged -> formState.update {
                it.copy(confirmPassword = action.value, confirmPasswordErrorRes = null, generalErrorRes = null)
            }
            ResetPasswordAction.Submit -> submit()
        }
    }

    private fun submit() {
        val current = formState.value
        if (current.isSubmitting) return

        val confirmError = when {
            current.confirmPassword.isBlank() -> R.string.auth_error_password_blank
            current.confirmPassword != current.newPassword -> R.string.auth_error_password_mismatch
            else -> null
        }

        if (confirmError != null) {
            val passwordError = (PasswordPolicy.validate(current.newPassword) as? PasswordValidationResult.Invalid)
                ?.primaryError?.messageRes()
            formState.update {
                it.copy(
                    passwordErrorRes = passwordError,
                    confirmPasswordErrorRes = confirmError,
                )
            }
            return
        }

        formState.update {
            it.copy(
                isSubmitting = true,
                passwordErrorRes = null,
                confirmPasswordErrorRes = null,
                generalErrorRes = null,
            )
        }
        viewModelScope.launch {
            when (val result = confirmPasswordResetUseCase(resetCode, current.newPassword)) {
                ConfirmPasswordResetResult.Confirmed -> formState.update {
                    it.copy(isSubmitting = false, isSuccess = true)
                }
                is ConfirmPasswordResetResult.Invalid -> formState.update {
                    it.copy(
                        isSubmitting = false,
                        passwordErrorRes = result.passwordErrors.firstOrNull()?.messageRes(),
                        generalErrorRes = if (result.codeBlank) {
                            R.string.auth_reset_password_invalid_token
                        } else {
                            null
                        },
                    )
                }
                is ConfirmPasswordResetResult.Failed -> {
                    val errorRes = if (result.error == DataError.VALIDATION || result.error == DataError.NOT_FOUND) {
                        R.string.auth_reset_password_invalid_token
                    } else {
                        result.error.messageRes()
                    }
                    formState.update { it.copy(isSubmitting = false, generalErrorRes = errorRes) }
                }
            }
        }
    }
}
