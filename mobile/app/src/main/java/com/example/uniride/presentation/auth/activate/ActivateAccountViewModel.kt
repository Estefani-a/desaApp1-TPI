package com.example.uniride.presentation.auth.activate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniride.R
import com.example.uniride.domain.usecase.auth.ActivateAccountResult
import com.example.uniride.domain.usecase.auth.ActivateAccountUseCase
import com.example.uniride.presentation.common.ContentState
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
 * A5 Activar cuenta ViewModel (RF01).
 * Handles the `uniride://activate?token=...` deep link and guards the single-use token
 * via [SavedStateHandle] so process recreation does not re-submit an already consumed code.
 */
@HiltViewModel
class ActivateAccountViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val activateAccountUseCase: ActivateAccountUseCase,
) : ViewModel() {

    private val activationCode: String = savedStateHandle.get<String>(ACTIVATION_CODE_KEY).orEmpty()

    private val _uiState = MutableStateFlow(
        ActivateAccountUiState(
            content = when {
                savedStateHandle.get<Boolean>(ACTIVATED_KEY) == true -> ContentState.Success(Unit)
                savedStateHandle.get<Boolean>(ATTEMPTED_KEY) == true -> ContentState.Error(
                    messageRes = R.string.auth_activate_interrupted,
                    canRetry = true,
                )
                else -> ContentState.Loading
            },
        ),
    )
    val uiState: StateFlow<ActivateAccountUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ActivateAccountEffect>(Channel.BUFFERED)
    val effects: Flow<ActivateAccountEffect> = _effects.receiveAsFlow()

    init {
        if (savedStateHandle.get<Boolean>(ACTIVATED_KEY) != true &&
            savedStateHandle.get<Boolean>(ATTEMPTED_KEY) != true
        ) {
            savedStateHandle[ATTEMPTED_KEY] = true
            activate()
        }
    }

    fun onAction(action: ActivateAccountAction) {
        when (action) {
            ActivateAccountAction.Retry -> activate()
        }
    }

    private fun activate() {
        _uiState.update { it.copy(content = ContentState.Loading) }
        viewModelScope.launch {
            when (val result = activateAccountUseCase(activationCode)) {
                is ActivateAccountResult.Activated -> {
                    savedStateHandle[ACTIVATED_KEY] = true
                    _uiState.update { it.copy(content = ContentState.Success(Unit)) }
                    _effects.send(ActivateAccountEffect.NavigateToMain)
                }
                ActivateAccountResult.InvalidCode -> {
                    _uiState.update {
                        it.copy(
                            content = ContentState.Error(
                                messageRes = R.string.auth_activate_invalid_token,
                                canRetry = false,
                            ),
                        )
                    }
                }
                is ActivateAccountResult.Failed -> {
                    _uiState.update {
                        it.copy(
                            content = ContentState.Error(
                                messageRes = result.error.messageRes(),
                                canRetry = true,
                            ),
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val ACTIVATION_CODE_KEY = "activationCode"
        internal const val ATTEMPTED_KEY = "activationAttempted"
        internal const val ACTIVATED_KEY = "activationSucceeded"
    }
}
