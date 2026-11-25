package com.example.uniride.presentation.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Snackbar of the shell of the app. It lives outside any screen, so a message survives the
 * screen that sent it: "Viaje publicado" keeps showing after the form went back.
 */
@Stable
class AppSnackbar(
    private val hostState: SnackbarHostState,
    private val scope: CoroutineScope,
) {
    /** Shows [message] for a short time, replacing the one on screen. Does not wait. */
    fun show(message: String) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            hostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }
}

/** `null` outside the shell (previews, tests): screens then use their own host. */
val LocalAppSnackbar = staticCompositionLocalOf<AppSnackbar?> { null }
