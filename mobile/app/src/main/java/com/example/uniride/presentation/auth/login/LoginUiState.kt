package com.example.uniride.presentation.auth.login

import androidx.annotation.StringRes

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    @param:StringRes val emailErrorRes: Int? = null,
    @param:StringRes val passwordErrorRes: Int? = null,
    @param:StringRes val generalErrorRes: Int? = null,
    val isSubmitting: Boolean = false,
    val isOffline: Boolean = false,
)

sealed interface LoginAction {
    data class EmailChanged(val value: String) : LoginAction
    data class PasswordChanged(val value: String) : LoginAction
    data object Submit : LoginAction
}

sealed interface LoginEffect {
    data object NavigateToMain : LoginEffect
}
