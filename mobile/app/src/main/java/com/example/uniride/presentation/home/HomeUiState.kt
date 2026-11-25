package com.example.uniride.presentation.home

import com.example.uniride.domain.model.AppMode

/**
 * State of the dynamic start (RF04). For now it only knows the mode; RF04 adds the active trip
 * of the passenger and the requests of the driver, read by a `HomeViewModel`.
 */
data class HomeUiState(
    val appMode: AppMode = AppMode.PASAJERO,
)

sealed interface HomeAction {
    data object OpenMyTrip : HomeAction
    data object PublishTrip : HomeAction
    data class OpenDriverPanel(val tripId: String) : HomeAction
}
