package com.example.uniride.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniride.domain.usecase.mode.ObserveAppModeUseCase
import com.example.uniride.domain.usecase.mode.SetAppModeUseCase
import com.example.uniride.domain.usecase.auth.ObserveIsLoggedInUseCase
import com.example.uniride.domain.usecase.profile.ObserveCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shell of the app (G3): the session, the mode chosen with the selector (kept between launches)
 * and the modes the profile allows.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val observeIsLoggedIn: ObserveIsLoggedInUseCase,
    observeCurrentUser: ObserveCurrentUserUseCase,
    observeAppMode: ObserveAppModeUseCase,
    private val setAppMode: SetAppModeUseCase,
) : ViewModel() {

    private val _effects = Channel<AppEffect>(Channel.BUFFERED)
    val effects: Flow<AppEffect> = _effects.receiveAsFlow()

    val uiState: StateFlow<AppUiState> = combine(
        observeIsLoggedIn(),
        observeCurrentUser(),
        observeAppMode(),
    ) { isLoggedIn, user, chosenMode ->
        val modes = availableModes(user?.profile)
        AppUiState(
            session = if (isLoggedIn) SessionStatus.LOGGED_IN else SessionStatus.LOGGED_OUT,
            appMode = effectiveMode(chosenMode, modes),
            availableModes = modes,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    init {
        notifyWhenSessionEnds()
    }

    fun onAction(action: AppAction) {
        when (action) {
            is AppAction.ChangeMode -> viewModelScope.launch { setAppMode(action.mode) }
            is AppAction.OpenFromPush -> openFromPush(action.destination)
        }
    }

    /**
     * Waits until the session is read (a tapped push can start the app). Without a session the
     * push is ignored: after logging in, the user sees the same state on the start screen.
     */
    private fun openFromPush(destination: PushDestination) {
        viewModelScope.launch {
            val state = uiState.first { it.session != SessionStatus.LOADING }
            if (state.session != SessionStatus.LOGGED_IN) return@launch
            if (destination.mode != state.appMode && destination.mode in state.availableModes) {
                setAppMode(destination.mode)
            }
            _effects.send(AppEffect.OpenPush(destination))
        }
    }

    /** Only a session that was open and closed: opening the app without one is not an end. */
    private fun notifyWhenSessionEnds() {
        viewModelScope.launch {
            var wasLoggedIn = false
            observeIsLoggedIn().distinctUntilChanged().collect { isLoggedIn ->
                if (wasLoggedIn && !isLoggedIn) _effects.send(AppEffect.SessionEnded)
                wasLoggedIn = isLoggedIn
            }
        }
    }
}
