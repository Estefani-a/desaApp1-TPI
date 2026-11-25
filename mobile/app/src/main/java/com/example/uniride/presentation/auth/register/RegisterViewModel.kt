package com.example.uniride.presentation.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniride.R
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.Vehicle
import com.example.uniride.domain.usecase.auth.RegisterResult
import com.example.uniride.domain.usecase.auth.RegisterUseCase
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.domain.validation.EmailValidationError
import com.example.uniride.domain.validation.EmailValidationResult
import com.example.uniride.domain.validation.EmailValidator
import com.example.uniride.domain.validation.PasswordPolicy
import com.example.uniride.domain.validation.PasswordValidationError
import com.example.uniride.domain.validation.PasswordValidationResult
import com.example.uniride.domain.validation.ProfileValidationError
import com.example.uniride.domain.validation.ProfileValidationResult
import com.example.uniride.domain.validation.ProfileValidator
import com.example.uniride.presentation.auth.components.firstMessageRes
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

/** Institutional registration ViewModel (RF01). */
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
                it.copy(fullName = action.value, fullNameErrorRes = null, generalErrorRes = null)
            }
            is RegisterAction.CareerChanged -> formState.update {
                it.copy(career = action.value, careerErrorRes = null, generalErrorRes = null)
            }
            is RegisterAction.PhoneChanged -> formState.update {
                it.copy(phone = action.value, phoneErrorRes = null, generalErrorRes = null)
            }
            is RegisterAction.PreferredModeChanged -> formState.update {
                val shouldSuggestVehicle = action.mode != PreferredMode.PASAJERO
                it.copy(
                    preferredMode = action.mode,
                    hasVehicle = if (shouldSuggestVehicle) true else it.hasVehicle,
                    vehicleRequiredErrorRes = null,
                    generalErrorRes = null,
                )
            }
            is RegisterAction.HasVehicleChanged -> formState.update {
                it.copy(
                    hasVehicle = action.enabled,
                    vehicleRequiredErrorRes = null,
                    vehicleBrandErrorRes = null,
                    vehicleModelErrorRes = null,
                    vehicleColorErrorRes = null,
                    vehiclePlateErrorRes = null,
                    generalErrorRes = null,
                )
            }
            is RegisterAction.VehicleBrandChanged -> formState.update {
                it.copy(vehicleBrand = action.value, vehicleBrandErrorRes = null, generalErrorRes = null)
            }
            is RegisterAction.VehicleModelChanged -> formState.update {
                it.copy(vehicleModel = action.value, vehicleModelErrorRes = null, generalErrorRes = null)
            }
            is RegisterAction.VehicleColorChanged -> formState.update {
                it.copy(vehicleColor = action.value, vehicleColorErrorRes = null, generalErrorRes = null)
            }
            is RegisterAction.VehiclePlateChanged -> formState.update {
                it.copy(vehiclePlate = action.value, vehiclePlateErrorRes = null, generalErrorRes = null)
            }
            RegisterAction.Submit -> submit()
        }
    }

    private fun submit() {
        val current = formState.value
        if (current.isSubmitting) return

        val profile = Profile(
            fullName = current.fullName,
            career = current.career,
            phone = current.phone,
            preferredMode = current.preferredMode,
            vehicle = if (current.hasVehicle) {
                Vehicle(
                    brand = current.vehicleBrand,
                    model = current.vehicleModel,
                    color = current.vehicleColor,
                    plate = current.vehiclePlate,
                )
            } else {
                null
            },
        )

        val confirmError = when {
            current.confirmPassword.isBlank() -> R.string.auth_error_password_blank
            current.confirmPassword != current.password -> R.string.auth_error_password_mismatch
            else -> null
        }

        if (confirmError != null) {
            applyValidationErrors(
                emailError = (EmailValidator.validate(current.email) as? EmailValidationResult.Invalid)?.error,
                passwordErrors = (PasswordPolicy.validate(current.password) as? PasswordValidationResult.Invalid)?.errors.orEmpty(),
                profileErrors = (ProfileValidator.validate(profile) as? ProfileValidationResult.Invalid)?.errors.orEmpty(),
                confirmErrorRes = confirmError,
            )
            return
        }

        val normalizedEmail = EmailValidator.normalize(current.email)
        formState.update {
            it.copy(
                isSubmitting = true,
                emailErrorRes = null,
                passwordErrorRes = null,
                confirmPasswordErrorRes = null,
                fullNameErrorRes = null,
                careerErrorRes = null,
                phoneErrorRes = null,
                vehicleRequiredErrorRes = null,
                vehicleBrandErrorRes = null,
                vehicleModelErrorRes = null,
                vehicleColorErrorRes = null,
                vehiclePlateErrorRes = null,
                generalErrorRes = null,
            )
        }
        viewModelScope.launch {
            when (val result = registerUseCase(current.email, current.password, profile)) {
                RegisterResult.Registered -> {
                    formState.update { it.copy(isSubmitting = false) }
                    _effects.send(RegisterEffect.NavigateToCheckEmail(normalizedEmail))
                }
                is RegisterResult.Invalid -> {
                    applyValidationErrors(
                        emailError = result.emailError,
                        passwordErrors = result.passwordErrors,
                        profileErrors = result.profileErrors,
                        confirmErrorRes = null,
                    )
                }
                is RegisterResult.Failed -> {
                    val errorRes = if (result.error == DataError.CONFLICT) {
                        R.string.auth_register_email_exists
                    } else {
                        result.error.messageRes()
                    }
                    formState.update { it.copy(isSubmitting = false, generalErrorRes = errorRes) }
                }
            }
        }
    }

    private fun applyValidationErrors(
        emailError: EmailValidationError?,
        passwordErrors: Set<PasswordValidationError>,
        profileErrors: Set<ProfileValidationError>,
        confirmErrorRes: Int?,
    ) {
        formState.update {
            it.copy(
                isSubmitting = false,
                emailErrorRes = emailError?.messageRes(),
                passwordErrorRes = passwordErrors.firstOrNull()?.messageRes(),
                confirmPasswordErrorRes = confirmErrorRes,
                fullNameErrorRes = profileErrors.firstMessageRes(ProfileValidationError.FULL_NAME_BLANK),
                careerErrorRes = profileErrors.firstMessageRes(ProfileValidationError.CAREER_BLANK),
                phoneErrorRes = profileErrors.firstMessageRes(
                    ProfileValidationError.PHONE_BLANK,
                    ProfileValidationError.PHONE_INVALID,
                ),
                vehicleRequiredErrorRes = profileErrors.firstMessageRes(ProfileValidationError.VEHICLE_REQUIRED),
                vehicleBrandErrorRes = profileErrors.firstMessageRes(ProfileValidationError.VEHICLE_BRAND_BLANK),
                vehicleModelErrorRes = profileErrors.firstMessageRes(ProfileValidationError.VEHICLE_MODEL_BLANK),
                vehicleColorErrorRes = profileErrors.firstMessageRes(ProfileValidationError.VEHICLE_COLOR_BLANK),
                vehiclePlateErrorRes = profileErrors.firstMessageRes(
                    ProfileValidationError.VEHICLE_PLATE_BLANK,
                    ProfileValidationError.VEHICLE_PLATE_INVALID,
                ),
            )
        }
    }
}
