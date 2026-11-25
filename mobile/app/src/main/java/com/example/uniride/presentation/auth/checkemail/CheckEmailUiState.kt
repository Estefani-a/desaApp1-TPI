package com.example.uniride.presentation.auth.checkemail

import androidx.annotation.StringRes

data class CheckEmailUiState(
    val email: String = "",
    @param:StringRes val emailErrorRes: Int? = null,
    @param:StringRes val feedbackMessageRes: Int? = null,
    val isFeedbackError: Boolean = false,
    val isResending: Boolean = false,
    val isOffline: Boolean = false,
)

sealed interface CheckEmailAction {
    data class EmailChanged(val value: String) : CheckEmailAction
    data object ResendActivation : CheckEmailAction
}
