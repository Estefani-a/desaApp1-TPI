package com.example.uniride.presentation.passenger

import androidx.annotation.StringRes
import com.example.uniride.domain.usecase.reservation.MyTrip
import com.example.uniride.presentation.common.ContentState

/** State of the start of the passenger (RF04): the next trip, or `Empty` if there is none. */
data class PassengerHomeUiState(
    val content: ContentState<PassengerHomeContent> = ContentState.Loading,
    val isOffline: Boolean = false,
)

data class PassengerHomeContent(
    val myTrip: MyTrip,
    /** Name of the campus of the trip, or `null` while the campuses are not loaded. */
    val campusName: String?,
)

sealed interface PassengerHomeAction {
    data object Refresh : PassengerHomeAction
    data object OpenMyTrip : PassengerHomeAction
    data object ExploreTrips : PassengerHomeAction
    data object DismissRejection : PassengerHomeAction
}

sealed interface PassengerHomeEffect {
    data class NavigateToMyTrip(val tripId: String) : PassengerHomeEffect
    data object NavigateToFeed : PassengerHomeEffect
    data class ShowMessage(@param:StringRes val messageRes: Int) : PassengerHomeEffect
}
