package com.example.uniride.presentation.auth.resetpassword

import androidx.annotation.StringRes

data class ForgotPasswordUiState(
    val email: String = "",
    @param:StringRes val emailErrorRes: Int? = null,
    @param:StringRes val generalErrorRes: Int? = null,
    val isSubmitting: Boolean = false,
    val isSent: Boolean = false,
    val isOffline: Boolean = false,
)

sealed interface ForgotPasswordAction {
    data class EmailChanged(val value: String) : ForgotPasswordAction
    data object Submit : ForgotPasswordAction
}
