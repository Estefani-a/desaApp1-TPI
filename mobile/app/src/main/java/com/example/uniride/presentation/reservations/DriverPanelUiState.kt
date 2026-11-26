package com.example.uniride.presentation.reservations

import androidx.annotation.StringRes
import com.example.uniride.domain.usecase.reservation.DriverPanel
import com.example.uniride.presentation.common.ContentState

/** State of C4 Panel del conductor (RF04). */
data class DriverPanelUiState(
    val content: ContentState<DriverPanel> = ContentState.Loading,
    val isOffline: Boolean = false,
    /** Request being answered: its buttons show a spinner and the others wait. */
    val answeringId: String? = null,
)

sealed interface DriverPanelAction {
    data object Refresh : DriverPanelAction
    data object OpenTrip : DriverPanelAction
    data class Accept(val reservationId: String) : DriverPanelAction
    data class Reject(val reservationId: String) : DriverPanelAction
    data class DismissRejection(val reservationId: String) : DriverPanelAction
}

sealed interface DriverPanelEffect {
    data class NavigateToTrip(val tripId: String) : DriverPanelEffect
    data class ShowMessage(@param:StringRes val messageRes: Int) : DriverPanelEffect
}
