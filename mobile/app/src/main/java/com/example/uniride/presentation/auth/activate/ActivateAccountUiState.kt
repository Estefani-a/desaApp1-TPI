package com.example.uniride.presentation.auth.activate

import androidx.annotation.StringRes

data class ActivateAccountUiState(
    val isLoading: Boolean = true,
    @param:StringRes val errorMessageRes: Int? = null,
    val canRetry: Boolean = false,
)

sealed interface ActivateAccountAction {
    data object Retry : ActivateAccountAction
}

sealed interface ActivateAccountEffect {
    data object NavigateToMain : ActivateAccountEffect
}
