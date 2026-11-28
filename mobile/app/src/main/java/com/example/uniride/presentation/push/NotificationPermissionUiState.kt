package com.example.uniride.presentation.push

import androidx.annotation.StringRes

/** UI state of the contextual `POST_NOTIFICATIONS` prompt (RNF08). */
data class NotificationPermissionUiState(
    /** `null` while DataStore loads so the rationale dialog never flashes unnecessarily. */
    val hasPrompted: Boolean? = null,
)

sealed interface NotificationPermissionAction {
    data object Confirm : NotificationPermissionAction
    data object Dismiss : NotificationPermissionAction
    data class SystemResult(val granted: Boolean) : NotificationPermissionAction
}

sealed interface NotificationPermissionEffect {
    data object LaunchSystemRequest : NotificationPermissionEffect
    data class ShowMessage(@param:StringRes val messageRes: Int) : NotificationPermissionEffect
}
