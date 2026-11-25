package com.example.uniride.presentation.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniride.R
import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.Vehicle
import com.example.uniride.domain.usecase.auth.RegisterUseCase
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.domain.validation.EmailValidationResult
import com.example.uniride.domain.validation.EmailValidator
import com.example.uniride.domain.validation.PasswordPolicy
import com.example.uniride.domain.validation.PasswordValidationResult
import com.example.uniride.domain.validation.ProfileValidationError
import com.example.uniride.domain.validation.ProfileValidationResult
import com.example.uniride.domain.validation.ProfileValidator
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
                    generalErrorRes = null,
                )
            }
            is RegisterAction.HasVehicleChanged -> formState.update {
                it.copy(
                    hasVehicle = action.enabled,
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

        val emailError = when (val res = EmailValidator.validate(current.email)) {
            EmailValidationResult.Valid -> null
            is EmailValidationResult.Invalid -> res.error.messageRes()
        }
        val passwordError = when (val res = PasswordPolicy.validate(current.password)) {
            PasswordValidationResult.Valid -> null
            is PasswordValidationResult.Invalid -> res.primaryError.messageRes()
        }
        val confirmError = when {
            current.confirmPassword.isBlank() -> R.string.auth_error_password_blank
            current.confirmPassword != current.password -> R.string.auth_error_password_mismatch
            else -> null
        }

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

        val profileErrors = when (val res = ProfileValidator.validate(profile)) {
            ProfileValidationResult.Valid -> emptySet()
            is ProfileValidationResult.Invalid -> res.errors
        }

        val hasAnyError = emailError != null ||
            passwordError != null ||
            confirmError != null ||
            profileErrors.isNotEmpty()

        if (hasAnyError) {
            formState.update {
                it.copy(
                    emailErrorRes = emailError,
                    passwordErrorRes = passwordError,
                    confirmPasswordErrorRes = confirmError,
                    fullNameErrorRes = profileErrors.errorFor(ProfileValidationError.FULL_NAME_BLANK),
                    careerErrorRes = profileErrors.errorFor(ProfileValidationError.CAREER_BLANK),
                    phoneErrorRes = profileErrors.errorFor(ProfileValidationError.PHONE_BLANK),
                    vehicleBrandErrorRes = profileErrors.errorFor(ProfileValidationError.VEHICLE_BRAND_BLANK),
                    vehicleModelErrorRes = profileErrors.errorFor(ProfileValidationError.VEHICLE_MODEL_BLANK),
                    vehicleColorErrorRes = profileErrors.errorFor(ProfileValidationError.VEHICLE_COLOR_BLANK),
                    vehiclePlateErrorRes = profileErrors.errorFor(ProfileValidationError.VEHICLE_PLATE_BLANK),
                )
            }
            return
        }

        val normalizedEmail = EmailValidator.normalize(current.email)
        formState.update { it.copy(isSubmitting = true, generalErrorRes = null) }
        viewModelScope.launch {
            when (val result = registerUseCase(normalizedEmail, current.password, profile)) {
                is AppResult.Success -> {
                    formState.update { it.copy(isSubmitting = false) }
                    _effects.send(RegisterEffect.NavigateToCheckEmail(normalizedEmail))
                }
                is AppResult.Failure -> {
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

    private fun Set<ProfileValidationError>.errorFor(error: ProfileValidationError): Int? =
        if (error in this) error.messageRes() else null
}
