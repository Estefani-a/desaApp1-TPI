package com.example.uniride.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniride.R
import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.TripFilters
import com.example.uniride.domain.usecase.campus.ObserveCampusesUseCase
import com.example.uniride.domain.usecase.campus.RefreshCampusesUseCase
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.domain.usecase.trip.ObserveFeedFiltersUseCase
import com.example.uniride.domain.usecase.trip.ObserveLastTripsRefreshUseCase
import com.example.uniride.domain.usecase.trip.ObserveTripsUseCase
import com.example.uniride.domain.usecase.trip.RefreshTripsUseCase
import com.example.uniride.domain.usecase.trip.SaveFeedFiltersUseCase
import com.example.uniride.presentation.common.ContentState
import com.example.uniride.presentation.common.messageRes
import com.example.uniride.presentation.common.toContentError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/**
 * B1 Explorar (RF03). The list always comes from the data saved on the phone, filtered by the
 * last filters the user chose; the server is only asked for news (on open, pull to refresh and
 * when the connection comes back). Offline the saved trips stay on screen under the banner.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FeedViewModel @Inject constructor(
    observeTrips: ObserveTripsUseCase,
    private val refreshTrips: RefreshTripsUseCase,
    observeFeedFilters: ObserveFeedFiltersUseCase,
    private val saveFeedFilters: SaveFeedFiltersUseCase,
    observeCampuses: ObserveCampusesUseCase,
    private val refreshCampuses: RefreshCampusesUseCase,
    observeLastTripsRefresh: ObserveLastTripsRefreshUseCase,
    private val observeConnectivity: ObserveConnectivityUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val refresh = MutableStateFlow(RefreshStatus())

    private val _effects = Channel<FeedEffect>(Channel.BUFFERED)
    val effects: Flow<FeedEffect> = _effects.receiveAsFlow()

    /** The trips of the current filters, together with those filters. */
    private val filteredTrips: Flow<Pair<TripFilters, List<Trip>>> =
        observeFeedFilters().distinctUntilChanged().flatMapLatest { filters ->
            observeTrips(filters).map { trips -> filters to trips }
        }

    val uiState: StateFlow<FeedUiState> = combine(
        filteredTrips,
        observeCampuses(),
        observeConnectivity(),
        observeLastTripsRefresh(),
        refresh,
    ) { (filters, trips), campuses, isOnline, lastRefreshedAt, refreshStatus ->
        FeedUiState(
            content = contentFor(trips, lastRefreshedAt, refreshStatus),
            filters = filters,
            campuses = campuses,
            dateOptions = dateOptions(filters.date),
            isOffline = !isOnline,
            isRefreshing = refreshStatus.isRefreshing && trips.isNotEmpty(),
            lastUpdatedAt = lastRefreshedAt?.atZone(clock.zone)?.toLocalTime(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedUiState())

    init {
        refresh(userInitiated = false)
        viewModelScope.launch { refreshCampuses() }
        refreshWhenConnectionReturns()
    }

    fun onAction(action: FeedAction) {
        when (action) {
            is FeedAction.ChangeFilters -> viewModelScope.launch { saveFeedFilters(action.filters) }
            FeedAction.ClearFilters -> viewModelScope.launch { saveFeedFilters(TripFilters()) }
            FeedAction.Refresh -> refresh(userInitiated = true)
            is FeedAction.OpenTrip -> viewModelScope.launch {
                _effects.send(FeedEffect.NavigateToDetail(action.tripId))
            }
        }
    }

    private fun refresh(userInitiated: Boolean) {
        if (refresh.value.isRefreshing) return
        refresh.value = refresh.value.copy(isRefreshing = true)
        viewModelScope.launch {
            val result = refreshTrips()
            val error = (result as? AppResult.Failure)?.error
            refresh.value = RefreshStatus(isRefreshing = false, lastError = error)
            // With trips on screen the error does not replace them: it is a Snackbar
            if (error != null && userInitiated && uiState.value.content is ContentState.Success) {
                _effects.send(FeedEffect.ShowMessage(error.messageRes()))
            }
            if (result is AppResult.Success && uiState.value.campuses.isEmpty()) refreshCampuses()
        }
    }

    /** 12.1: when the phone gets connection again, the feed brings the news by itself. */
    private fun refreshWhenConnectionReturns() {
        viewModelScope.launch {
            var wasOffline = false
            observeConnectivity().distinctUntilChanged().collect { isOnline ->
                if (isOnline && wasOffline) refresh(userInitiated = false)
                wasOffline = !isOnline
            }
        }
    }

    /**
     * Saved trips are shown whenever there are some. Without trips, it depends on whether the
     * feed was ever refreshed: if it was, the list is just empty; if not (first start), it is
     * loading, or an error if the first refresh failed.
     */
    private fun contentFor(trips: List<Trip>, lastRefreshedAt: Instant?, status: RefreshStatus): ContentState<List<Trip>> =
        when {
            trips.isNotEmpty() -> ContentState.Success(trips)
            lastRefreshedAt != null -> ContentState.Empty
            status.isRefreshing -> ContentState.Loading
            else -> when (val error = status.lastError) {
                null -> ContentState.Loading
                DataError.NO_CONNECTION -> ContentState.Error(R.string.feed_first_start_offline)
                else -> error.toContentError()
            }
        }

    /** Today and the next six days; a chosen date further away stays as an option. */
    private fun dateOptions(selected: LocalDate?): List<LocalDate> {
        val today = LocalDate.now(clock)
        val week = (0L until DAYS_TO_CHOOSE).map { today.plusDays(it) }
        return if (selected != null && selected !in week) week + selected else week
    }

    private data class RefreshStatus(
        val isRefreshing: Boolean = false,
        val lastError: DataError? = null,
    )

    private companion object {
        const val DAYS_TO_CHOOSE = 7L
    }
}
