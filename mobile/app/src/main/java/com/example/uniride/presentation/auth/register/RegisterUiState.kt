package com.example.uniride.presentation.auth.register

import androidx.annotation.StringRes
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.presentation.common.ProfileForm

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val profile: ProfileForm = ProfileForm(),
    @param:StringRes val emailErrorRes: Int? = null,
    @param:StringRes val passwordErrorRes: Int? = null,
    @param:StringRes val confirmPasswordErrorRes: Int? = null,
    @param:StringRes val generalErrorRes: Int? = null,
    val isSubmitting: Boolean = false,
    val isOffline: Boolean = false,
)

sealed interface RegisterAction {
    data class EmailChanged(val value: String) : RegisterAction
    data class PasswordChanged(val value: String) : RegisterAction
    data class ConfirmPasswordChanged(val value: String) : RegisterAction
    data class FullNameChanged(val value: String) : RegisterAction
    data class CareerChanged(val value: String) : RegisterAction
    data class PhoneChanged(val value: String) : RegisterAction
    data class PreferredModeChanged(val mode: PreferredMode) : RegisterAction
    data class HasVehicleChanged(val enabled: Boolean) : RegisterAction
    data class VehicleBrandChanged(val value: String) : RegisterAction
    data class VehicleModelChanged(val value: String) : RegisterAction
    data class VehicleColorChanged(val value: String) : RegisterAction
    data class VehiclePlateChanged(val value: String) : RegisterAction
    data object Submit : RegisterAction
}

sealed interface RegisterEffect {
    data class NavigateToCheckEmail(val email: String) : RegisterEffect
}
