package com.example.uniride.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniride.R
import com.example.uniride.core.result.AppResult
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.User
import com.example.uniride.domain.model.Vehicle
import com.example.uniride.domain.usecase.auth.LogoutUseCase
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.domain.usecase.profile.ObserveCurrentUserUseCase
import com.example.uniride.domain.usecase.profile.RefreshProfileUseCase
import com.example.uniride.domain.usecase.profile.UpdateProfileResult
import com.example.uniride.domain.usecase.profile.UpdateProfileUseCase
import com.example.uniride.domain.validation.ProfileValidationError
import com.example.uniride.presentation.auth.components.firstMessageRes
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

/** Profile and logout ViewModel (RF01). Reads cached user from Room and refreshes in background. */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val refreshProfileUseCase: RefreshProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val logoutUseCase: LogoutUseCase,
    observeConnectivity: ObserveConnectivityUseCase,
) : ViewModel() {

    private var hasUnsavedEdits = false
    private val formState = MutableStateFlow(ProfileUiState())

    private val _effects = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects: Flow<ProfileEffect> = _effects.receiveAsFlow()

    val uiState: StateFlow<ProfileUiState> = combine(
        formState,
        observeConnectivity(),
    ) { state, isOnline ->
        state.copy(isOffline = !isOnline)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(),
    )

    init {
        observeUser()
        refresh()
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.FullNameChanged -> edit {
                it.copy(fullName = action.value, fullNameErrorRes = null, feedbackMessageRes = null)
            }
            is ProfileAction.CareerChanged -> edit {
                it.copy(career = action.value, careerErrorRes = null, feedbackMessageRes = null)
            }
            is ProfileAction.PhoneChanged -> edit {
                it.copy(phone = action.value, phoneErrorRes = null, feedbackMessageRes = null)
            }
            is ProfileAction.PreferredModeChanged -> edit {
                val shouldSuggestVehicle = action.mode != PreferredMode.PASAJERO
                it.copy(
                    preferredMode = action.mode,
                    hasVehicle = if (shouldSuggestVehicle) true else it.hasVehicle,
                    vehicleRequiredErrorRes = null,
                    feedbackMessageRes = null,
                )
            }
            is ProfileAction.HasVehicleChanged -> edit {
                it.copy(
                    hasVehicle = action.enabled,
                    vehicleRequiredErrorRes = null,
                    vehicleBrandErrorRes = null,
                    vehicleModelErrorRes = null,
                    vehicleColorErrorRes = null,
                    vehiclePlateErrorRes = null,
                    feedbackMessageRes = null,
                )
            }
            is ProfileAction.VehicleBrandChanged -> edit {
                it.copy(vehicleBrand = action.value, vehicleBrandErrorRes = null, feedbackMessageRes = null)
            }
            is ProfileAction.VehicleModelChanged -> edit {
                it.copy(vehicleModel = action.value, vehicleModelErrorRes = null, feedbackMessageRes = null)
            }
            is ProfileAction.VehicleColorChanged -> edit {
                it.copy(vehicleColor = action.value, vehicleColorErrorRes = null, feedbackMessageRes = null)
            }
            is ProfileAction.VehiclePlateChanged -> edit {
                it.copy(vehiclePlate = action.value, vehiclePlateErrorRes = null, feedbackMessageRes = null)
            }
            ProfileAction.Save -> save()
            ProfileAction.RetryLoad -> refresh()
            ProfileAction.RequestLogout -> formState.update { it.copy(showLogoutDialog = true) }
            ProfileAction.DismissLogoutDialog -> formState.update { it.copy(showLogoutDialog = false) }
            ProfileAction.ConfirmLogout -> logout()
        }
    }

    private fun edit(transform: (ProfileUiState) -> ProfileUiState) {
        hasUnsavedEdits = true
        formState.update(transform)
    }

    private fun observeUser() {
        viewModelScope.launch {
            observeCurrentUserUseCase().collect { user ->
                if (user != null) {
                    applyUser(user)
                }
            }
        }
    }

    private fun applyUser(user: User) {
        formState.update { current ->
            if (hasUnsavedEdits && current.hasLoadedUser) {
                // Keep user's in-progress edits while refreshing read-only fields.
                current.copy(
                    hasLoadedUser = true,
                    isLoading = false,
                    loadErrorRes = null,
                    email = user.email,
                    rating = user.rating,
                )
            } else {
                current.copy(
                    hasLoadedUser = true,
                    isLoading = false,
                    loadErrorRes = null,
                    email = user.email,
                    fullName = user.profile.fullName,
                    career = user.profile.career,
                    phone = user.profile.phone,
                    preferredMode = user.profile.preferredMode,
                    hasVehicle = user.profile.vehicle != null,
                    vehicleBrand = user.profile.vehicle?.brand.orEmpty(),
                    vehicleModel = user.profile.vehicle?.model.orEmpty(),
                    vehicleColor = user.profile.vehicle?.color.orEmpty(),
                    vehiclePlate = user.profile.vehicle?.plate.orEmpty(),
                    rating = user.rating,
                )
            }
        }
    }

    private fun refresh() {
        formState.update { it.copy(isLoading = !it.hasLoadedUser, loadErrorRes = null) }
        viewModelScope.launch {
            when (val result = refreshProfileUseCase()) {
                is AppResult.Success -> formState.update { it.copy(isLoading = false, loadErrorRes = null) }
                is AppResult.Failure -> formState.update { current ->
                    current.copy(
                        isLoading = false,
                        loadErrorRes = if (!current.hasLoadedUser) result.error.messageRes() else null,
                    )
                }
            }
        }
    }

    private fun save() {
        val current = formState.value
        if (current.isSaving || current.isLoggingOut) return

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

        formState.update {
            it.copy(
                isSaving = true,
                fullNameErrorRes = null,
                careerErrorRes = null,
                phoneErrorRes = null,
                vehicleRequiredErrorRes = null,
                vehicleBrandErrorRes = null,
                vehicleModelErrorRes = null,
                vehicleColorErrorRes = null,
                vehiclePlateErrorRes = null,
                feedbackMessageRes = null,
            )
        }
        viewModelScope.launch {
            when (val result = updateProfileUseCase(profile)) {
                is UpdateProfileResult.Updated -> {
                    hasUnsavedEdits = false
                    applyUser(result.user)
                    formState.update {
                        it.copy(
                            isSaving = false,
                            feedbackMessageRes = R.string.profile_saved_message,
                            isFeedbackError = false,
                        )
                    }
                }
                is UpdateProfileResult.Invalid -> {
                    val errors = result.errors
                    formState.update {
                        it.copy(
                            isSaving = false,
                            fullNameErrorRes = errors.firstMessageRes(ProfileValidationError.FULL_NAME_BLANK),
                            careerErrorRes = errors.firstMessageRes(ProfileValidationError.CAREER_BLANK),
                            phoneErrorRes = errors.firstMessageRes(
                                ProfileValidationError.PHONE_BLANK,
                                ProfileValidationError.PHONE_INVALID,
                            ),
                            vehicleRequiredErrorRes = errors.firstMessageRes(ProfileValidationError.VEHICLE_REQUIRED),
                            vehicleBrandErrorRes = errors.firstMessageRes(ProfileValidationError.VEHICLE_BRAND_BLANK),
                            vehicleModelErrorRes = errors.firstMessageRes(ProfileValidationError.VEHICLE_MODEL_BLANK),
                            vehicleColorErrorRes = errors.firstMessageRes(ProfileValidationError.VEHICLE_COLOR_BLANK),
                            vehiclePlateErrorRes = errors.firstMessageRes(
                                ProfileValidationError.VEHICLE_PLATE_BLANK,
                                ProfileValidationError.VEHICLE_PLATE_INVALID,
                            ),
                        )
                    }
                }
                is UpdateProfileResult.Failed -> formState.update {
                    it.copy(
                        isSaving = false,
                        feedbackMessageRes = result.error.messageRes(),
                        isFeedbackError = true,
                    )
                }
            }
        }
    }

    private fun logout() {
        if (formState.value.isLoggingOut) return
        formState.update { it.copy(showLogoutDialog = false, isLoggingOut = true) }
        viewModelScope.launch {
            logoutUseCase()
            formState.update { it.copy(isLoggingOut = false) }
            _effects.send(ProfileEffect.NavigateToAuth)
        }
    }
}
