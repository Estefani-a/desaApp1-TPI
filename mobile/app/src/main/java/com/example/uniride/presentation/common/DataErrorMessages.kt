package com.example.uniride.presentation.common

import androidx.annotation.StringRes
import com.example.uniride.R
import com.example.uniride.core.result.DataError

/**
 * Text shown for each [DataError] (CONVENCIONES §6.3: the ViewModel turns the error into a
 * string resource; domain and data have no texts). A screen that needs a more specific message,
 * like "sin plazas" of RF04, maps that case itself and uses this one for the rest.
 */
@StringRes
fun DataError.messageRes(): Int = when (this) {
    DataError.NO_CONNECTION -> R.string.common_error_no_connection
    DataError.TIMEOUT -> R.string.common_error_timeout
    DataError.UNAUTHORIZED -> R.string.common_error_unauthorized
    DataError.NOT_FOUND -> R.string.common_error_not_found
    DataError.CONFLICT -> R.string.common_error_conflict
    DataError.VALIDATION -> R.string.common_error_validation
    DataError.SERVER -> R.string.common_error_server
    DataError.UNKNOWN -> R.string.common_error_unknown
}

/** Retrying does not help with an expired session: the app goes back to the login (11.3). */
fun DataError.toContentError(): ContentState.Error =
    ContentState.Error(messageRes = messageRes(), canRetry = this != DataError.UNAUTHORIZED)
