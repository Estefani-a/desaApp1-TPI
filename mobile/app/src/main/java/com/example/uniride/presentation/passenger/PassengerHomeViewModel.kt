package com.example.uniride.presentation.passenger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.usecase.campus.ObserveCampusesUseCase
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.domain.usecase.reservation.DismissReservationRejectionUseCase
import com.example.uniride.domain.usecase.reservation.MyTrip
import com.example.uniride.domain.usecase.reservation.ObserveMyActiveTripUseCase
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
 * Start of the passenger (RF04): the card of the next trip, read from the phone so it works
 * offline (12.1), or an invitation to explore. The server is only asked for news, on open and
 * when the connection comes back.
 */
@HiltViewModel
class PassengerHomeViewModel @Inject constructor(
    observeMyActiveTrip: ObserveMyActiveTripUseCase,
    observeCampuses: ObserveCampusesUseCase,
    private val observeConnectivity: ObserveConnectivityUseCase,
    private val refreshMyTrips: RefreshMyTripsUseCase,
    private val dismissRejection: DismissReservationRejectionUseCase,
) : ViewModel() {

    private val refresh = MutableStateFlow(RefreshStatus())

    private val _effects = Channel<PassengerHomeEffect>(Channel.BUFFERED)
    val effects: Flow<PassengerHomeEffect> = _effects.receiveAsFlow()

    val uiState: StateFlow<PassengerHomeUiState> = combine(
        observeMyActiveTrip(),
        observeCampuses(),
        observeConnectivity(),
        refresh,
    ) { myTrip, campuses, isOnline, refreshStatus ->
        val content = myTrip?.let { mine ->
            PassengerHomeContent(mine, campuses.firstOrNull { it.code == mine.trip.campus }?.name)
        }
        PassengerHomeUiState(content = contentFor(content, refreshStatus), isOffline = !isOnline)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PassengerHomeUiState())

    init {
        refresh()
        refreshWhenConnectionReturns()
    }

    fun onAction(action: PassengerHomeAction) {
        when (action) {
            PassengerHomeAction.Refresh -> refresh()
            PassengerHomeAction.ExploreTrips -> send(PassengerHomeEffect.NavigateToFeed)
            PassengerHomeAction.OpenMyTrip -> currentTrip()?.let {
                send(PassengerHomeEffect.NavigateToMyTrip(it.trip.id.value))
            }
            PassengerHomeAction.DismissRejection -> viewModelScope.launch {
                currentTrip()?.let { dismissRejection(it.reservation.id) }
            }
        }
    }

    private fun currentTrip(): MyTrip? = (uiState.value.content as? ContentState.Success)?.data?.myTrip

    private fun send(effect: PassengerHomeEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private fun refresh() {
        if (refresh.value.isRefreshing) return
        refresh.value = refresh.value.copy(isRefreshing = true)
        viewModelScope.launch {
            val error = (refreshMyTrips() as? AppResult.Failure)?.error
            refresh.value = RefreshStatus(isRefreshing = false, lastError = error)
            // With a trip on screen the error does not replace it: it is a Snackbar. Without
            // connection the banner already says it, and an expired session belongs to RF01.
            if (error != null && error != DataError.NO_CONNECTION && error != DataError.UNAUTHORIZED &&
                uiState.value.content is ContentState.Success
            ) {
                send(PassengerHomeEffect.ShowMessage(error.messageRes()))
            }
        }
    }

    /** 12.1: when the phone gets connection again, the start brings the news by itself. */
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
     * A saved trip is always shown. Without it: empty, unless the refresh failed with an error
     * the user can retry. `UNAUTHORIZED` belongs to RF01, as in C1.
     */
    private fun contentFor(content: PassengerHomeContent?, status: RefreshStatus): ContentState<PassengerHomeContent> {
        val error = status.lastError
        return when {
            content != null -> ContentState.Success(content)
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
