package com.example.uniride.presentation.common

import androidx.annotation.StringRes

/**
 * Main content of a screen (CONVENCIONES §7.1). The indicators that can live together with it
 * (offline, refreshing, last update) go in separate fields of each `XxxUiState`, so a screen can
 * show saved data and the offline banner at the same time (12.1).
 */
sealed interface ContentState<out T> {
    data object Loading : ContentState<Nothing>
    data class Success<T>(val data: T) : ContentState<T>
    data object Empty : ContentState<Nothing>
    data class Error(@param:StringRes val messageRes: Int, val canRetry: Boolean = true) : ContentState<Nothing>
}
