package com.example.uniride.presentation.driver

import androidx.annotation.StringRes
import com.example.uniride.domain.model.CampusCode
import com.example.uniride.domain.model.RejectedTrip
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.TripId
import com.example.uniride.presentation.common.ContentState

/** State of C1 Inicio del conductor (RF02). */
data class DriverHomeUiState(
    val content: ContentState<DriverHomeContent> = ContentState.Loading,
    val isOffline: Boolean = false,
)

/** What C1 lists: the notices of rejected trips on top and the upcoming trips under them. */
data class DriverHomeContent(
    val rejections: List<RejectedTrip> = emptyList(),
    val trips: List<Trip> = emptyList(),
    /** Trips created on this phone that the server did not confirm yet. */
    val pendingTripIds: Set<TripId> = emptySet(),
    val campusNames: Map<CampusCode, String> = emptyMap(),
)

sealed interface DriverHomeAction {
    data object Refresh : DriverHomeAction
    data object PublishTrip : DriverHomeAction
    data class OpenTrip(val tripId: String) : DriverHomeAction
    data class DismissRejection(val tripId: String) : DriverHomeAction
}

sealed interface DriverHomeEffect {
    data object NavigateToPublish : DriverHomeEffect
    data class NavigateToPanel(val tripId: String) : DriverHomeEffect
    data class ShowMessage(@param:StringRes val messageRes: Int) : DriverHomeEffect
}
