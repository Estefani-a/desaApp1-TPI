package com.example.uniride.presentation.auth.resetpassword

import androidx.annotation.StringRes

data class ResetPasswordUiState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    @param:StringRes val passwordErrorRes: Int? = null,
    @param:StringRes val confirmPasswordErrorRes: Int? = null,
    @param:StringRes val generalErrorRes: Int? = null,
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val isOffline: Boolean = false,
)

sealed interface ResetPasswordAction {
    data class NewPasswordChanged(val value: String) : ResetPasswordAction
    data class ConfirmPasswordChanged(val value: String) : ResetPasswordAction
    data object Submit : ResetPasswordAction
}
