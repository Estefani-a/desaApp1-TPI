package com.example.uniride.presentation.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniride.R
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.usecase.auth.LoginResult
import com.example.uniride.domain.usecase.auth.LoginUseCase
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.presentation.auth.components.messageRes
import com.example.uniride.presentation.common.messageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A2 Login ViewModel (RF01). */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    observeConnectivity: ObserveConnectivityUseCase,
) : ViewModel() {

    private val formState = MutableStateFlow(LoginUiState())

    private val _effects = Channel<LoginEffect>(Channel.BUFFERED)
    val effects: Flow<LoginEffect> = _effects.receiveAsFlow()

    val uiState: StateFlow<LoginUiState> = combine(
        formState,
        observeConnectivity(),
    ) { state, isOnline ->
        state.copy(isOffline = !isOnline)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LoginUiState(),
    )

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.EmailChanged -> formState.update {
                it.copy(email = action.value, emailErrorRes = null, generalErrorRes = null)
            }
            is LoginAction.PasswordChanged -> formState.update {
                it.copy(password = action.value, passwordErrorRes = null, generalErrorRes = null)
            }
            LoginAction.Submit -> submit()
        }
    }

    private fun submit() {
        val current = formState.value
        if (current.isSubmitting) return

        formState.update {
            it.copy(
                isSubmitting = true,
                emailErrorRes = null,
                passwordErrorRes = null,
                generalErrorRes = null,
            )
        }
        viewModelScope.launch {
            when (val result = loginUseCase(current.email, current.password)) {
                is LoginResult.LoggedIn -> {
                    formState.update { it.copy(isSubmitting = false) }
                    _effects.send(LoginEffect.NavigateToMain)
                }
                is LoginResult.Invalid -> {
                    formState.update {
                        it.copy(
                            isSubmitting = false,
                            emailErrorRes = result.emailError?.messageRes(),
                            passwordErrorRes = if (result.passwordBlank) R.string.auth_error_password_blank else null,
                        )
                    }
                }
                is LoginResult.Failed -> {
                    val errorRes = if (result.error == DataError.UNAUTHORIZED) {
                        R.string.auth_login_invalid_credentials
                    } else {
                        result.error.messageRes()
                    }
                    formState.update { it.copy(isSubmitting = false, generalErrorRes = errorRes) }
                }
            }
        }
    }
}
