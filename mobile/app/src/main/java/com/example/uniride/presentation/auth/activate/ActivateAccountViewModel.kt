package com.example.uniride.presentation.auth.activate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.uniride.R
import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.usecase.auth.ActivateAccountUseCase
import com.example.uniride.presentation.auth.ActivateAccountRoute
import com.example.uniride.presentation.common.messageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles the `uniride://activate?token=...` deep link (RF01).
 * Automatically activates the account and signs the student in upon arrival.
 */
@HiltViewModel
class ActivateAccountViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val activateAccountUseCase: ActivateAccountUseCase,
) : ViewModel() {

    private val activationCode: String = runCatching {
        savedStateHandle.toRoute<ActivateAccountRoute>().activationCode
    }.getOrElse {
        savedStateHandle.get<String>("activationCode").orEmpty()
    }

    private val _uiState = MutableStateFlow(ActivateAccountUiState())
    val uiState: StateFlow<ActivateAccountUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ActivateAccountEffect>(Channel.BUFFERED)
    val effects: Flow<ActivateAccountEffect> = _effects.receiveAsFlow()

    init {
        activate()
    }

    fun onAction(action: ActivateAccountAction) {
        when (action) {
            ActivateAccountAction.Retry -> activate()
        }
    }

    private fun activate() {
        _uiState.update { it.copy(isLoading = true, errorMessageRes = null) }
        viewModelScope.launch {
            when (val result = activateAccountUseCase(activationCode)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effects.send(ActivateAccountEffect.NavigateToMain)
                }
                is AppResult.Failure -> {
                    val isInvalidToken = result.error == DataError.VALIDATION || result.error == DataError.NOT_FOUND
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessageRes = if (isInvalidToken) {
                                R.string.auth_activate_invalid_token
                            } else {
                                result.error.messageRes()
                            },
                            canRetry = !isInvalidToken,
                        )
                    }
                }
            }
        }
    }
}
