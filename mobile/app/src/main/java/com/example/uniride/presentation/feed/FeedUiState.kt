package com.example.uniride.presentation.feed

import androidx.annotation.StringRes
import com.example.uniride.domain.model.Campus
import com.example.uniride.domain.model.CampusCode
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.TripFilters
import com.example.uniride.presentation.common.ContentState
import java.time.LocalDate
import java.time.LocalTime

/** State of B1 Explorar (RF03). */
data class FeedUiState(
    val content: ContentState<List<Trip>> = ContentState.Loading,
    val filters: TripFilters = TripFilters(),
    val campuses: List<Campus> = emptyList(),
    /** Days the date filter offers: today and the next ones. */
    val dateOptions: List<LocalDate> = emptyList(),
    val isOffline: Boolean = false,
    val isRefreshing: Boolean = false,
    /** Time of the last successful refresh, for "Actualizado HH:MM". */
    val lastUpdatedAt: LocalTime? = null,
) {
    val campusNames: Map<CampusCode, String> get() = campuses.associate { it.code to it.name }
}

sealed interface FeedAction {
    data class ChangeFilters(val filters: TripFilters) : FeedAction
    data object ClearFilters : FeedAction
    data object Refresh : FeedAction
    data class OpenTrip(val tripId: String) : FeedAction
}

sealed interface FeedEffect {
    data class NavigateToDetail(val tripId: String) : FeedEffect
    data class ShowMessage(@param:StringRes val messageRes: Int) : FeedEffect
}
