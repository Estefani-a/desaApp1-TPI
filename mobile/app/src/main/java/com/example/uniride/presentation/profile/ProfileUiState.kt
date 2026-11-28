package com.example.uniride.presentation.profile

import androidx.annotation.StringRes
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.model.Rating
import com.example.uniride.presentation.common.ContentState
import com.example.uniride.presentation.common.ProfileForm

/** Loaded profile content shown inside [ProfileUiState.content] (RF01). */
data class ProfileData(
    val email: String = "",
    val rating: Rating? = null,
    val form: ProfileForm = ProfileForm(),
)

data class ProfileUiState(
    val content: ContentState<ProfileData> = ContentState.Loading,
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
