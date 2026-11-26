package com.example.uniride.presentation.detail

import androidx.annotation.StringRes
import com.example.uniride.domain.usecase.reservation.TripDetail
import com.example.uniride.presentation.common.ContentState

/** State of B3 Detalle del viaje, also used by B6 Mi viaje (RF04). */
data class TripDetailUiState(
    val content: ContentState<TripDetail> = ContentState.Loading,
    val isOffline: Boolean = false,
    /** A request or a cancel is being saved: the button shows a spinner. */
    val isSubmitting: Boolean = false,
    /** "¿Cancelar la reserva?" is open. */
    val isConfirmingCancel: Boolean = false,
)

sealed interface TripDetailAction {
    data object Refresh : TripDetailAction
    data object RequestSeat : TripDetailAction
    data object AskCancel : TripDetailAction
    data object ConfirmCancel : TripDetailAction
    data object DismissCancel : TripDetailAction
    data object DismissRejection : TripDetailAction
    data object OpenDriverPanel : TripDetailAction
}

sealed interface TripDetailEffect {
    data class NavigateToSeatRequested(val tripId: String) : TripDetailEffect
    data class NavigateToDriverPanel(val tripId: String) : TripDetailEffect
    data class ShowMessage(@param:StringRes val messageRes: Int) : TripDetailEffect
}
