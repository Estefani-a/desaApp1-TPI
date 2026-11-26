package com.example.uniride.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniride.R
import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.domain.usecase.reservation.CancelReservationUseCase
import com.example.uniride.domain.usecase.reservation.DismissReservationRejectionUseCase
import com.example.uniride.domain.usecase.reservation.ObserveTripDetailUseCase
import com.example.uniride.domain.usecase.reservation.RefreshTripDetailUseCase
import com.example.uniride.domain.usecase.reservation.RequestSeatResult
import com.example.uniride.domain.usecase.reservation.RequestSeatUseCase
import com.example.uniride.domain.usecase.reservation.ReservationChangeResult
import com.example.uniride.domain.usecase.reservation.TripDetail
import com.example.uniride.presentation.common.ContentState
import com.example.uniride.presentation.common.messageRes
import com.example.uniride.presentation.common.toContentError
import com.example.uniride.presentation.reservations.messageRes
import com.example.uniride.presentation.reservations.passengerMessageRes
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
 * B3 Detalle del viaje and B6 Mi viaje (RF04). Both show a trip with the user's reservation on
 * it, so they share this ViewModel; the route only changes the title. Everything is read from the
 * phone, so a confirmed trip is readable offline (12.1); the server is only asked for news.
 */
@HiltViewModel
class TripDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeTripDetail: ObserveTripDetailUseCase,
    private val observeConnectivity: ObserveConnectivityUseCase,
    private val refreshTripDetail: RefreshTripDetailUseCase,
    private val requestSeat: RequestSeatUseCase,
    private val cancelReservation: CancelReservationUseCase,
    private val dismissRejection: DismissReservationRejectionUseCase,
) : ViewModel() {

    /** Both routes ([TripDetailRoute] and `MyTripRoute`) carry the trip as `tripId`. */
    private val tripId = TripId(checkNotNull(savedStateHandle.get<String>(TRIP_ID_KEY)) { "Falta tripId en la ruta" })

    private val refresh = MutableStateFlow(RefreshStatus())
    private val form = MutableStateFlow(FormStatus())

    private val _effects = Channel<TripDetailEffect>(Channel.BUFFERED)
    val effects: Flow<TripDetailEffect> = _effects.receiveAsFlow()

    private val detail = observeTripDetail(tripId)

    val uiState: StateFlow<TripDetailUiState> = combine(
        detail,
        observeConnectivity(),
        refresh,
        form,
    ) { detail, isOnline, refreshStatus, formStatus ->
        TripDetailUiState(
            content = contentFor(detail, refreshStatus),
            isOffline = !isOnline,
            isSubmitting = formStatus.isSubmitting,
            isConfirmingCancel = formStatus.isConfirmingCancel,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripDetailUiState())

    init {
        refresh()
        refreshWhenConnectionReturns()
    }

    fun onAction(action: TripDetailAction) {
        when (action) {
            TripDetailAction.Refresh -> refresh()
            TripDetailAction.RequestSeat -> submit { requestSeat() }
            TripDetailAction.AskCancel -> form.update { it.copy(isConfirmingCancel = true) }
            TripDetailAction.DismissCancel -> form.update { it.copy(isConfirmingCancel = false) }
            TripDetailAction.ConfirmCancel -> {
                form.update { it.copy(isConfirmingCancel = false) }
                submit { cancel() }
            }
            TripDetailAction.DismissRejection -> viewModelScope.launch {
                currentDetail()?.myReservation?.let { dismissRejection(it.id) }
            }
            TripDetailAction.OpenDriverPanel -> send(TripDetailEffect.NavigateToDriverPanel(tripId.value))
        }
    }

    private suspend fun requestSeat() {
        when (val result = requestSeat(tripId)) {
            is RequestSeatResult.Requested -> send(TripDetailEffect.NavigateToSeatRequested(tripId.value))
            is RequestSeatResult.NotAllowed -> result.reason.messageRes?.let { send(TripDetailEffect.ShowMessage(it)) }
            is RequestSeatResult.Failed -> send(TripDetailEffect.ShowMessage(result.error.messageRes()))
        }
    }

    private suspend fun cancel() {
        val reservation = currentDetail()?.myReservation ?: return
        when (val result = cancelReservation(tripId, reservation.id)) {
            ReservationChangeResult.Done -> send(TripDetailEffect.ShowMessage(R.string.detail_cancel_done))
            is ReservationChangeResult.NotAllowed -> send(TripDetailEffect.ShowMessage(result.reason.passengerMessageRes()))
            is ReservationChangeResult.Failed -> send(TripDetailEffect.ShowMessage(result.error.messageRes()))
        }
    }

    /** One write at a time: a double tap does not send two requests. */
    private fun submit(block: suspend () -> Unit) {
        if (form.value.isSubmitting) return
        form.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            try {
                block()
            } finally {
                form.update { it.copy(isSubmitting = false) }
            }
        }
    }

    private fun currentDetail(): TripDetail? = (uiState.value.content as? ContentState.Success)?.data

    private fun send(effect: TripDetailEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private fun refresh() {
        if (refresh.value.isRefreshing) return
        refresh.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            val error = (refreshTripDetail(tripId) as? AppResult.Failure)?.error
            refresh.value = RefreshStatus(isRefreshing = false, hasRefreshed = true, lastError = error)
        }
    }

    /** 12.1: when the connection comes back, the detail brings the news by itself. */
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
     * The saved trip is always shown. Without it: loading until the first refresh answers, then
     * the error of the refresh, or "no encontramos el viaje" if the server does not have it either.
     */
    private fun contentFor(detail: TripDetail?, status: RefreshStatus): ContentState<TripDetail> {
        val error = status.lastError
        return when {
            detail != null -> ContentState.Success(detail)
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

    private data class FormStatus(
        val isSubmitting: Boolean = false,
        val isConfirmingCancel: Boolean = false,
    )

    companion object {
        const val TRIP_ID_KEY = "tripId"
    }
}
