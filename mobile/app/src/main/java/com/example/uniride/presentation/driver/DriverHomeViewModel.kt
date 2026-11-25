package com.example.uniride.presentation.driver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.usecase.campus.ObserveCampusesUseCase
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.domain.usecase.trip.DismissTripRejectionUseCase
import com.example.uniride.domain.usecase.trip.ObservePendingTripIdsUseCase
import com.example.uniride.domain.usecase.trip.ObserveRejectedTripsUseCase
import com.example.uniride.domain.usecase.trip.ObserveUpcomingDriverTripsUseCase
import com.example.uniride.domain.usecase.trip.RefreshMyTripsUseCase
import com.example.uniride.presentation.common.ContentState
import com.example.uniride.presentation.common.messageRes
import com.example.uniride.presentation.common.toContentError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * C1 Inicio del conductor (RF02). The list always comes from the data saved on the phone, so a
 * trip published offline shows up right away, marked as pending. The server is only asked for
 * news (on open and when the connection comes back); if that fails the saved data stays.
 */
@HiltViewModel
class DriverHomeViewModel @Inject constructor(
    observeUpcomingTrips: ObserveUpcomingDriverTripsUseCase,
    observeRejectedTrips: ObserveRejectedTripsUseCase,
    observePendingTripIds: ObservePendingTripIdsUseCase,
    observeCampuses: ObserveCampusesUseCase,
    private val observeConnectivity: ObserveConnectivityUseCase,
    private val refreshMyTrips: RefreshMyTripsUseCase,
    private val dismissTripRejection: DismissTripRejectionUseCase,
) : ViewModel() {

    private val refresh = MutableStateFlow(RefreshStatus())

    private val _effects = Channel<DriverHomeEffect>(Channel.BUFFERED)
    val effects: Flow<DriverHomeEffect> = _effects.receiveAsFlow()

    private val saved: Flow<DriverHomeContent> = combine(
        observeUpcomingTrips(),
        observeRejectedTrips(),
        observePendingTripIds(),
        observeCampuses(),
    ) { trips, rejections, pendingIds, campuses ->
        DriverHomeContent(
            rejections = rejections,
            trips = trips,
            pendingTripIds = pendingIds,
            campusNames = campuses.associate { it.code to it.name },
        )
    }

    /** `Loading` lasts until Room emits for the first time. */
    val uiState: StateFlow<DriverHomeUiState> = combine(
        saved,
        observeConnectivity(),
        refresh,
    ) { content, isOnline, refreshStatus ->
        DriverHomeUiState(content = contentFor(content, refreshStatus), isOffline = !isOnline)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DriverHomeUiState())

    init {
        refresh()
        refreshWhenConnectionReturns()
    }

    fun onAction(action: DriverHomeAction) {
        when (action) {
            DriverHomeAction.Refresh -> refresh()
            DriverHomeAction.PublishTrip -> send(DriverHomeEffect.NavigateToPublish)
            is DriverHomeAction.OpenTrip -> send(DriverHomeEffect.NavigateToPanel(action.tripId))
            is DriverHomeAction.DismissRejection -> viewModelScope.launch {
                dismissTripRejection(TripId(action.tripId))
            }
        }
    }

    private fun send(effect: DriverHomeEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private fun refresh() {
        if (refresh.value.isRefreshing) return
        refresh.value = refresh.value.copy(isRefreshing = true)
        viewModelScope.launch {
            val error = (refreshMyTrips() as? AppResult.Failure)?.error
            refresh.value = RefreshStatus(isRefreshing = false, lastError = error)
            // With trips on screen the error does not replace them: it is a Snackbar. Without
            // connection the banner already says it, and an expired session belongs to RF01.
            if (error != null && error != DataError.NO_CONNECTION && error != DataError.UNAUTHORIZED &&
                uiState.value.content is ContentState.Success
            ) {
                send(DriverHomeEffect.ShowMessage(error.messageRes()))
            }
        }
    }

    /** 12.1: when the phone gets connection again, C1 brings the news by itself. */
    private fun refreshWhenConnectionReturns() {
        viewModelScope.launch {
            var wasOffline = false
            observeConnectivity().distinctUntilChanged().collect { isOnline ->
                if (isOnline && wasOffline) refresh()
                wasOffline = !isOnline
            }
        }
    }

    /**
     * Something saved (trips or notices) is always shown. Without it: empty, unless the refresh
     * failed with an error the user can retry. `UNAUTHORIZED` is not shown: the session belongs
     * to RF01, and until it exists every refresh answers 401.
     */
    private fun contentFor(content: DriverHomeContent, status: RefreshStatus): ContentState<DriverHomeContent> {
        val error = status.lastError
        return when {
            content.trips.isNotEmpty() || content.rejections.isNotEmpty() -> ContentState.Success(content)
            error == null || error == DataError.UNAUTHORIZED -> ContentState.Empty
            status.isRefreshing -> ContentState.Loading
            else -> error.toContentError()
        }
    }

    private data class RefreshStatus(
        val isRefreshing: Boolean = false,
        val lastError: DataError? = null,
    )
}
