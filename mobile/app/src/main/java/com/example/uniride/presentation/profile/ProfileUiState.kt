package com.example.uniride.presentation.profile

import androidx.annotation.StringRes
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.model.Rating

data class ProfileUiState(
    val hasLoadedUser: Boolean = false,
    val isLoading: Boolean = true,
    val email: String = "",
    val fullName: String = "",
    val career: String = "",
    val phone: String = "",
    val preferredMode: PreferredMode = PreferredMode.PASAJERO,
    val hasVehicle: Boolean = false,
    val vehicleBrand: String = "",
    val vehicleModel: String = "",
    val vehicleColor: String = "",
    val vehiclePlate: String = "",
    val rating: Rating? = null,
    @param:StringRes val fullNameErrorRes: Int? = null,
    @param:StringRes val careerErrorRes: Int? = null,
    @param:StringRes val phoneErrorRes: Int? = null,
    @param:StringRes val vehicleBrandErrorRes: Int? = null,
    @param:StringRes val vehicleModelErrorRes: Int? = null,
    @param:StringRes val vehicleColorErrorRes: Int? = null,
    @param:StringRes val vehiclePlateErrorRes: Int? = null,
    @param:StringRes val loadErrorRes: Int? = null,
    @param:StringRes val feedbackMessageRes: Int? = null,
    val isFeedbackError: Boolean = false,
    val isSaving: Boolean = false,
    val isLoggingOut: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val isOffline: Boolean = false,
)

sealed interface ProfileAction {
    data class FullNameChanged(val value: String) : ProfileAction
    data class CareerChanged(val value: String) : ProfileAction
    data class PhoneChanged(val value: String) : ProfileAction
    data class PreferredModeChanged(val mode: PreferredMode) : ProfileAction
    data class HasVehicleChanged(val enabled: Boolean) : ProfileAction
    data class VehicleBrandChanged(val value: String) : ProfileAction
    data class VehicleModelChanged(val value: String) : ProfileAction
    data class VehicleColorChanged(val value: String) : ProfileAction
    data class VehiclePlateChanged(val value: String) : ProfileAction
    data object Save : ProfileAction
    data object RetryLoad : ProfileAction
    data object RequestLogout : ProfileAction
    data object DismissLogoutDialog : ProfileAction
    data object ConfirmLogout : ProfileAction
}

sealed interface ProfileEffect {
    data object NavigateToAuth : ProfileEffect
}
