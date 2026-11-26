package com.example.uniride.presentation.home

import com.example.uniride.domain.model.AppMode

/**
 * State of the dynamic start (RF04). It only knows the mode: each mode has its own screen with
 * its own ViewModel (passenger start and C1).
 */
data class HomeUiState(
    val appMode: AppMode = AppMode.PASAJERO,
)

sealed interface HomeAction {
    data class OpenMyTrip(val tripId: String) : HomeAction
    data object ExploreTrips : HomeAction
    data object PublishTrip : HomeAction
    data class OpenDriverPanel(val tripId: String) : HomeAction
}
