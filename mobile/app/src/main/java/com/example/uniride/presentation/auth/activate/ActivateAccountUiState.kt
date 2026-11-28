package com.example.uniride.presentation.auth.activate

import com.example.uniride.presentation.common.ContentState

data class ActivateAccountUiState(
    val content: ContentState<Unit> = ContentState.Loading,
)

sealed interface ActivateAccountAction {
    data object Retry : ActivateAccountAction
}

sealed interface ActivateAccountEffect {
    data object NavigateToMain : ActivateAccountEffect
}
