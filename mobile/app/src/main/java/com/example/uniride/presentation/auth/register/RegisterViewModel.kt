package com.example.uniride.presentation.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniride.R
import com.example.uniride.domain.usecase.auth.RegisterResult
import com.example.uniride.domain.usecase.auth.RegisterUseCase
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
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

/** A3 Registro ViewModel (RF01). Delegates all validation rules to [RegisterUseCase]. */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase,
    observeConnectivity: ObserveConnectivityUseCase,
) : ViewModel() {

    private val formState = MutableStateFlow(RegisterUiState())

    private val _effects = Channel<RegisterEffect>(Channel.BUFFERED)
    val effects: Flow<RegisterEffect> = _effects.receiveAsFlow()

    val uiState: StateFlow<RegisterUiState> = combine(
        formState,
        observeConnectivity(),
    ) { state, isOnline ->
        state.copy(isOffline = !isOnline)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RegisterUiState(),
    )

    fun onAction(action: RegisterAction) {
        when (action) {
            is RegisterAction.EmailChanged -> formState.update {
                it.copy(email = action.value, emailErrorRes = null, generalErrorRes = null)
            }
            is RegisterAction.PasswordChanged -> formState.update {
                it.copy(password = action.value, passwordErrorRes = null, generalErrorRes = null)
            }
            is RegisterAction.ConfirmPasswordChanged -> formState.update {
                it.copy(confirmPassword = action.value, confirmPasswordErrorRes = null, generalErrorRes = null)
            }
            is RegisterAction.FullNameChanged -> formState.update {
                it.copy(
                    profile = it.profile.copy(fullName = action.value, fullNameErrorRes = null),
                    generalErrorRes = null,
                )
            }
            is RegisterAction.CareerChanged -> formState.update {
                it.copy(
                    profile = it.profile.copy(career = action.value, careerErrorRes = null),
                    generalErrorRes = null,
                )
            }
            is RegisterAction.PhoneChanged -> formState.update {
                it.copy(
                    profile = it.profile.copy(phone = action.value, phoneErrorRes = null),
                    generalErrorRes = null,
                )
            }
            is RegisterAction.PreferredModeChanged -> formState.update {
                it.copy(profile = it.profile.withPreferredMode(action.mode), generalErrorRes = null)
            }
            is RegisterAction.HasVehicleChanged -> formState.update {
                it.copy(profile = it.profile.withHasVehicle(action.enabled), generalErrorRes = null)
            }
            is RegisterAction.VehicleBrandChanged -> formState.update {
                it.copy(
                    profile = it.profile.copy(vehicleBrand = action.value, vehicleBrandErrorRes = null),
                    generalErrorRes = null,
                )
            }
            is RegisterAction.VehicleModelChanged -> formState.update {
                it.copy(
                    profile = it.profile.copy(vehicleModel = action.value, vehicleModelErrorRes = null),
                    generalErrorRes = null,
                )
            }
            is RegisterAction.VehicleColorChanged -> formState.update {
                it.copy(
                    profile = it.profile.copy(vehicleColor = action.value, vehicleColorErrorRes = null),
                    generalErrorRes = null,
                )
            }
            is RegisterAction.VehiclePlateChanged -> formState.update {
                it.copy(
                    profile = it.profile.copy(vehiclePlate = action.value, vehiclePlateErrorRes = null),
                    generalErrorRes = null,
                )
            }
            RegisterAction.Submit -> submit()
        }
    }

    private fun submit() {
        val current = formState.value
        if (current.isSubmitting) return

        val normalizedEmail = EmailValidator.normalize(current.email)
        formState.update {
            it.copy(
                isSubmitting = true,
                emailErrorRes = null,
                passwordErrorRes = null,
                confirmPasswordErrorRes = null,
                profile = it.profile.clearErrors(),
                generalErrorRes = null,
            )
        }
        viewModelScope.launch {
            when (
                val result = registerUseCase(
                    email = current.email,
                    password = current.password,
                    profile = current.profile.toProfile(),
                    confirmPassword = current.confirmPassword,
                )
            ) {
                RegisterResult.Registered -> {
                    formState.update { it.copy(isSubmitting = false) }
                    _effects.send(RegisterEffect.NavigateToCheckEmail(normalizedEmail))
                }
                is RegisterResult.Invalid -> {
                    val confirmErrorRes = when {
                        result.confirmPasswordBlank -> R.string.auth_error_password_blank
                        result.passwordMismatch -> R.string.auth_error_password_mismatch
                        else -> null
                    }
                    formState.update {
                        it.copy(
                            isSubmitting = false,
                            emailErrorRes = result.emailError?.messageRes(),
                            passwordErrorRes = result.passwordErrors.firstOrNull()?.messageRes(),
                            confirmPasswordErrorRes = confirmErrorRes,
                            profile = it.profile.withErrors(result.profileErrors),
                        )
                    }
                }
                RegisterResult.EmailAlreadyRegistered -> formState.update {
                    it.copy(
                        isSubmitting = false,
                        generalErrorRes = R.string.auth_register_email_exists,
                    )
                }
                is RegisterResult.Failed -> formState.update {
                    it.copy(
                        isSubmitting = false,
                        generalErrorRes = result.error.messageRes(),
                    )
                }
            }
        }
    }
}
