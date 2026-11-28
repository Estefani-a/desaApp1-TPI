package com.example.uniride.presentation.auth.resetpassword

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniride.R
import com.example.uniride.domain.usecase.auth.ConfirmPasswordResetResult
import com.example.uniride.domain.usecase.auth.ConfirmPasswordResetUseCase
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

/** A7 Nueva contraseña ViewModel (RF01). Delegates all validation to [ConfirmPasswordResetUseCase]. */
@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val confirmPasswordResetUseCase: ConfirmPasswordResetUseCase,
    observeConnectivity: ObserveConnectivityUseCase,
) : ViewModel() {

    private val resetCode: String = savedStateHandle.get<String>(RESET_CODE_KEY).orEmpty()

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

        formState.update {
            it.copy(
                isSubmitting = true,
                passwordErrorRes = null,
                confirmPasswordErrorRes = null,
                generalErrorRes = null,
            )
        }
        viewModelScope.launch {
            when (
                val result = confirmPasswordResetUseCase(
                    resetCode = resetCode,
                    newPassword = current.newPassword,
                    confirmPassword = current.confirmPassword,
                )
            ) {
                ConfirmPasswordResetResult.Confirmed -> formState.update {
                    it.copy(isSubmitting = false, isSuccess = true)
                }
                is ConfirmPasswordResetResult.Invalid -> {
                    val confirmErrorRes = when {
                        result.confirmPasswordBlank -> R.string.auth_error_password_blank
                        result.passwordMismatch -> R.string.auth_error_password_mismatch
                        else -> null
                    }
                    formState.update {
                        it.copy(
                            isSubmitting = false,
                            passwordErrorRes = result.passwordErrors.firstOrNull()?.messageRes(),
                            confirmPasswordErrorRes = confirmErrorRes,
                            generalErrorRes = if (result.codeBlank) {
                                R.string.auth_reset_password_invalid_token
                            } else {
                                null
                            },
                        )
                    }
                }
                ConfirmPasswordResetResult.InvalidCode -> formState.update {
                    it.copy(
                        isSubmitting = false,
                        generalErrorRes = R.string.auth_reset_password_invalid_token,
                    )
                }
                ConfirmPasswordResetResult.WeakPassword -> formState.update {
                    it.copy(
                        isSubmitting = false,
                        passwordErrorRes = R.string.auth_reset_password_weak_server,
                    )
                }
                is ConfirmPasswordResetResult.Failed -> formState.update {
                    it.copy(
                        isSubmitting = false,
                        generalErrorRes = result.error.messageRes(),
                    )
                }
            }
        }
    }

    companion object {
        const val RESET_CODE_KEY = "resetCode"
    }
}
