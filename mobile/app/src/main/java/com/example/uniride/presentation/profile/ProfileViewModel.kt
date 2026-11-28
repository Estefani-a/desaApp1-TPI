package com.example.uniride.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniride.R
import com.example.uniride.core.result.AppResult
import com.example.uniride.domain.model.User
import com.example.uniride.domain.usecase.auth.LogoutUseCase
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.domain.usecase.profile.ObserveCurrentUserUseCase
import com.example.uniride.domain.usecase.profile.RefreshProfileUseCase
import com.example.uniride.domain.usecase.profile.UpdateProfileResult
import com.example.uniride.domain.usecase.profile.UpdateProfileUseCase
import com.example.uniride.presentation.common.ContentState
import com.example.uniride.presentation.common.ProfileForm
import com.example.uniride.presentation.common.messageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** D1 Perfil ViewModel (RF01). Reads cached user from Room and refreshes in background. */
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
            is ProfileAction.FullNameChanged -> editForm {
                it.copy(fullName = action.value, fullNameErrorRes = null)
            }
            is ProfileAction.CareerChanged -> editForm {
                it.copy(career = action.value, careerErrorRes = null)
            }
            is ProfileAction.PhoneChanged -> editForm {
                it.copy(phone = action.value, phoneErrorRes = null)
            }
            is ProfileAction.PreferredModeChanged -> editForm {
                it.withPreferredMode(action.mode)
            }
            is ProfileAction.HasVehicleChanged -> editForm {
                it.withHasVehicle(action.enabled)
            }
            is ProfileAction.VehicleBrandChanged -> editForm {
                it.copy(vehicleBrand = action.value, vehicleBrandErrorRes = null)
            }
            is ProfileAction.VehicleModelChanged -> editForm {
                it.copy(vehicleModel = action.value, vehicleModelErrorRes = null)
            }
            is ProfileAction.VehicleColorChanged -> editForm {
                it.copy(vehicleColor = action.value, vehicleColorErrorRes = null)
            }
            is ProfileAction.VehiclePlateChanged -> editForm {
                it.copy(vehiclePlate = action.value, vehiclePlateErrorRes = null)
            }
            ProfileAction.Save -> save()
            ProfileAction.RetryLoad -> refresh()
            ProfileAction.RequestLogout -> formState.update { it.copy(showLogoutDialog = true) }
            ProfileAction.DismissLogoutDialog -> formState.update { it.copy(showLogoutDialog = false) }
            ProfileAction.ConfirmLogout -> logout()
        }
    }

    private fun editForm(transform: (ProfileForm) -> ProfileForm) {
        hasUnsavedEdits = true
        formState.update { state ->
            val current = state.content as? ContentState.Success ?: return@update state
            state.copy(
                content = ContentState.Success(current.data.copy(form = transform(current.data.form))),
                feedbackMessageRes = null,
            )
        }
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
        formState.update { state ->
            val existing = state.content as? ContentState.Success
            val nextData = if (hasUnsavedEdits && existing != null) {
                // Keep user's in-progress edits while refreshing read-only fields.
                existing.data.copy(
                    email = user.email,
                    rating = user.rating,
                )
            } else {
                ProfileData(
                    email = user.email,
                    rating = user.rating,
                    form = ProfileForm.fromProfile(user.profile),
                )
            }
            state.copy(content = ContentState.Success(nextData))
        }
    }

    private fun refresh() {
        formState.update { state ->
            if (state.content is ContentState.Success) state else state.copy(content = ContentState.Loading)
        }
        viewModelScope.launch {
            when (val result = refreshProfileUseCase()) {
                is AppResult.Success -> formState.update { state ->
                    if (state.content is ContentState.Success) state else state.copy(content = ContentState.Empty)
                }
                is AppResult.Failure -> formState.update { state ->
                    if (state.content is ContentState.Success) {
                        state
                    } else {
                        state.copy(
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

    private fun save() {
        val currentState = formState.value
        if (currentState.isSaving || currentState.isLoggingOut) return
        val currentData = (currentState.content as? ContentState.Success)?.data ?: return

        val profile = currentData.form.toProfile()
        formState.update { state ->
            val data = (state.content as? ContentState.Success)?.data ?: return@update state
            state.copy(
                content = ContentState.Success(data.copy(form = data.form.clearErrors())),
                isSaving = true,
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
                is UpdateProfileResult.Invalid -> formState.update { state ->
                    val data = (state.content as? ContentState.Success)?.data
                    state.copy(
                        content = data?.let {
                            ContentState.Success(it.copy(form = it.form.withErrors(result.errors)))
                        } ?: state.content,
                        isSaving = false,
                    )
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
        }
    }
}
