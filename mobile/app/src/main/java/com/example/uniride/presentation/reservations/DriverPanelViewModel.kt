package com.example.uniride.presentation.reservations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniride.R
import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.ReservationId
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.domain.usecase.reservation.DismissReservationRejectionUseCase
import com.example.uniride.domain.usecase.reservation.DriverPanel
import com.example.uniride.domain.usecase.reservation.ObserveDriverPanelUseCase
import com.example.uniride.domain.usecase.reservation.RefreshDriverPanelUseCase
import com.example.uniride.domain.usecase.reservation.ReservationChangeResult
import com.example.uniride.domain.usecase.reservation.RespondToRequestUseCase
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * C4 Panel del conductor (RF04): the requests of a trip, answered from the phone. An answer is
 * saved right away (offline too) and sent by the sync; if the server does not take it, the
 * request shows why.
 */
@HiltViewModel
class DriverPanelViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeDriverPanel: ObserveDriverPanelUseCase,
    private val observeConnectivity: ObserveConnectivityUseCase,
    private val refreshDriverPanel: RefreshDriverPanelUseCase,
    private val respondToRequest: RespondToRequestUseCase,
    private val dismissRejection: DismissReservationRejectionUseCase,
) : ViewModel() {

    private val tripId = TripId(checkNotNull(savedStateHandle.get<String>(TRIP_ID_KEY)) { "Falta tripId en la ruta" })

    private val refresh = MutableStateFlow(RefreshStatus())
    private val answering = MutableStateFlow<String?>(null)

    private val _effects = Channel<DriverPanelEffect>(Channel.BUFFERED)
    val effects: Flow<DriverPanelEffect> = _effects.receiveAsFlow()

    val uiState: StateFlow<DriverPanelUiState> = combine(
        observeDriverPanel(tripId),
        observeConnectivity(),
        refresh,
        answering,
    ) { panel, isOnline, refreshStatus, answeringId ->
        DriverPanelUiState(
            content = contentFor(panel, refreshStatus),
            isOffline = !isOnline,
            answeringId = answeringId,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DriverPanelUiState())

    init {
        refresh()
        refreshWhenConnectionReturns()
    }

    fun onAction(action: DriverPanelAction) {
        when (action) {
            DriverPanelAction.Refresh -> refresh()
            DriverPanelAction.OpenTrip -> send(DriverPanelEffect.NavigateToTrip(tripId.value))
            is DriverPanelAction.Accept -> respond(action.reservationId, accept = true)
            is DriverPanelAction.Reject -> respond(action.reservationId, accept = false)
            is DriverPanelAction.DismissRejection -> viewModelScope.launch {
                dismissRejection(ReservationId(action.reservationId))
            }
        }
    }

    /** One answer at a time: a double tap does not answer twice. */
    private fun respond(reservationId: String, accept: Boolean) {
        if (answering.value != null) return
        answering.value = reservationId
        viewModelScope.launch {
            try {
                val message = when (val result = respondToRequest(tripId, ReservationId(reservationId), accept)) {
                    ReservationChangeResult.Done ->
                        if (accept) R.string.driver_panel_accepted else R.string.driver_panel_rejected
                    is ReservationChangeResult.NotAllowed -> result.reason.driverMessageRes()
                    is ReservationChangeResult.Failed -> result.error.messageRes()
                }
                send(DriverPanelEffect.ShowMessage(message))
            } finally {
                answering.update { null }
            }
        }
    }

    private fun send(effect: DriverPanelEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private fun refresh() {
        if (refresh.value.isRefreshing) return
        refresh.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            val error = (refreshDriverPanel(tripId) as? AppResult.Failure)?.error
            refresh.value = RefreshStatus(isRefreshing = false, hasRefreshed = true, lastError = error)
        }
    }

    /** 12.1: when the connection comes back, the panel brings the new requests by itself. */
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
     * The saved trip is always shown, with or without requests (the screen says when there are
     * none). Without it: loading until the first refresh answers, then its error.
     */
    private fun contentFor(panel: DriverPanel?, status: RefreshStatus): ContentState<DriverPanel> {
        val error = status.lastError
        return when {
            panel != null -> ContentState.Success(panel)
            !status.hasRefreshed || status.isRefreshing -> ContentState.Loading
            error == null || error == DataError.NOT_FOUND -> ContentState.Error(R.string.detail_not_found, canRetry = false)
            else -> error.toContentError()
        }
    }

    private data class RefreshStatus(
        val isRefreshing: Boolean = false,
        val hasRefreshed: Boolean = false,
        val lastError: DataError? = null,
    )

    companion object {
        const val TRIP_ID_KEY = "tripId"
    }
}
