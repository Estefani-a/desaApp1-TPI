package com.example.uniride.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.usecase.auth.ObserveIsLoggedInUseCase
import com.example.uniride.domain.usecase.profile.ObserveCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface AppSessionUiState {
    /** Waiting for the encrypted session token check before choosing the start graph. */
    data object Loading : AppSessionUiState

    data class Ready(
        val isLoggedIn: Boolean,
        val profile: Profile?,
    ) : AppSessionUiState
}

/**
 * Root session ViewModel (RF01). Awaits the encrypted token check before rendering [UniRideApp]
 * so `SessionTokenStore.currentToken()` is warmed before any authenticated screen refreshes,
 * and exposes session loss (e.g. HTTP 401) so the app returns to [AuthGraphRoute].
 */
@HiltViewModel
class AppSessionViewModel @Inject constructor(
    observeIsLoggedIn: ObserveIsLoggedInUseCase,
    observeCurrentUser: ObserveCurrentUserUseCase,
) : ViewModel() {

    val uiState: StateFlow<AppSessionUiState> = combine(
        observeIsLoggedIn(),
        observeCurrentUser(),
    ) { isLoggedIn, user ->
        AppSessionUiState.Ready(
            isLoggedIn = isLoggedIn,
            profile = user?.profile,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSessionUiState.Loading,
    )
}
