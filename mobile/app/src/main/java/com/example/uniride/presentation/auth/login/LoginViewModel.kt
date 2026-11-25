package com.example.uniride.presentation.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniride.R
import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.usecase.auth.LoginUseCase
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.domain.validation.EmailValidationResult
import com.example.uniride.domain.validation.EmailValidator
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

        val emailError = when (val res = EmailValidator.validate(current.email)) {
            EmailValidationResult.Valid -> null
            is EmailValidationResult.Invalid -> res.error.messageRes()
        }
        val passwordError = if (current.password.isBlank()) R.string.auth_error_password_blank else null

        if (emailError != null || passwordError != null) {
            formState.update {
                it.copy(emailErrorRes = emailError, passwordErrorRes = passwordError)
            }
            return
        }

        formState.update { it.copy(isSubmitting = true, generalErrorRes = null) }
        viewModelScope.launch {
            when (val result = loginUseCase(current.email, current.password)) {
                is AppResult.Success -> {
                    formState.update { it.copy(isSubmitting = false) }
                    _effects.send(LoginEffect.NavigateToMain)
                }
                is AppResult.Failure -> {
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
