package com.example.uniride.presentation.navigation

import com.example.uniride.domain.model.AppMode

/** Whether there is a session. [LOADING] only while the session is read when the app opens. */
enum class SessionStatus { LOADING, LOGGED_IN, LOGGED_OUT }

/**
 * State of the shell: the session decides where the app starts, and the profile which modes the
 * selector offers.
 *
 * @param appMode mode in use: the one chosen last if the profile allows it
 * @param availableModes modes the selector offers; with one, the selector is hidden
 */
data class AppUiState(
    val session: SessionStatus = SessionStatus.LOADING,
    val appMode: AppMode = AppMode.PASAJERO,
    val availableModes: Set<AppMode> = AppMode.entries.toSet(),
)

sealed interface AppAction {
    data class ChangeMode(val mode: AppMode) : AppAction
}

sealed interface AppEffect {
    /** The session ended (logout, or a 401 anywhere): back to the start of the auth graph. */
    data object SessionEnded : AppEffect
}
