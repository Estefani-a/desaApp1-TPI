package com.example.uniride.presentation.feed

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniride.R
import com.example.uniride.domain.model.Campus
import com.example.uniride.domain.model.CampusCode
import com.example.uniride.domain.model.Direction
import com.example.uniride.domain.model.Shift
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.TripFilters
import com.example.uniride.presentation.common.BusinessZone
import com.example.uniride.presentation.common.ContentState
import com.example.uniride.presentation.common.ContentStateHost
import com.example.uniride.presentation.common.EmptyState
import com.example.uniride.presentation.common.OfflineBanner
import com.example.uniride.presentation.common.PreviewData
import com.example.uniride.presentation.common.TripCard
import com.example.uniride.presentation.common.formatTime
import com.example.uniride.presentation.feed.components.FeedFilters
import com.example.uniride.presentation.theme.Dimens
import com.example.uniride.presentation.theme.UniRideTheme
import java.time.LocalDate
import java.time.LocalTime

/** Gets the [FeedViewModel], collects its state and turns its effects into navigation. */
@Composable
fun FeedRoute(
    onOpenTrip: (tripId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is FeedEffect.NavigateToDetail -> onOpenTrip(effect.tripId)
                is FeedEffect.ShowMessage -> snackbarHostState.showSnackbar(resources.getString(effect.messageRes))
            }
        }
    }

    FeedScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

/**
 * B1 Explorar (RF03), as in the guide: the question of the direction, the filters and the list
 * of trips with pull to refresh. Offline it keeps showing the saved trips under the "Sin
 * conexión" banner (12.1).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    state: FeedUiState,
    onAction: (FeedAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    // The shell with the bottom bar already leaves the space of the system bars
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
        ) {
            if (state.isOffline) {
                OfflineBanner(lastUpdatedAt = state.lastUpdatedAt)
            }
            Text(
                text = stringResource(headerRes(state.filters.direction)),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .padding(horizontal = Dimens.ScreenPadding)
                    .padding(top = Dimens.SpaceL)
                    .semantics { heading() },
            )
            FeedFilters(
                filters = state.filters,
                campuses = state.campuses,
                dateOptions = state.dateOptions,
                onChange = { onAction(FeedAction.ChangeFilters(it)) },
            )
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onAction(FeedAction.Refresh) },
                modifier = Modifier.weight(1f),
            ) {
                ContentStateHost(
                    state = state.content,
                    onRetry = { onAction(FeedAction.Refresh) },
                    empty = { FeedEmptyState(state.filters, onClear = { onAction(FeedAction.ClearFilters) }) },
                ) { trips ->
                    TripList(
                        trips = trips,
                        campusNames = state.campusNames,
                        lastUpdatedAt = state.lastUpdatedAt,
                        onOpenTrip = { onAction(FeedAction.OpenTrip(it)) },
                    )
                }
            }
        }
    }
}

/** The question of the guide follows the direction chosen; with both, the generic title. */
@StringRes
private fun headerRes(direction: Direction?): Int = when (direction) {
    Direction.VUELTA -> R.string.feed_question_vuelta
    Direction.IDA -> R.string.feed_question_ida
    null -> R.string.feed_title
}

@Composable
private fun TripList(
    trips: List<Trip>,
    campusNames: Map<CampusCode, String>,
    lastUpdatedAt: LocalTime?,
    onOpenTrip: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.ScreenPadding,
            end = Dimens.ScreenPadding,
            bottom = Dimens.SpaceL,
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
    ) {
        item(key = "summary") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = pluralStringResource(R.plurals.feed_trips_available, trips.size, trips.size),
                    style = MaterialTheme.typography.titleSmall,
                )
                if (lastUpdatedAt != null) {
                    Text(
                        text = stringResource(R.string.common_last_updated_at, formatTime(lastUpdatedAt)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(trips, key = { it.id.value }) { trip ->
            TripCard(
                trip = trip,
                campusName = campusNames[trip.campus],
                onClick = { onOpenTrip(trip.id.value) },
            )
        }
    }
}

/** Empty because of the filters (with a way out) or because nobody published yet. */
@Composable
private fun FeedEmptyState(filters: TripFilters, onClear: () -> Unit) {
    if (filters.isEmpty) {
        EmptyState(
            title = stringResource(R.string.feed_empty_title),
            message = stringResource(R.string.feed_empty_message),
        )
    } else {
        EmptyState(
            title = stringResource(R.string.feed_empty_filtered_title),
            message = stringResource(R.string.feed_empty_filtered_message),
            actionLabel = stringResource(R.string.feed_clear_filters),
            onAction = onClear,
        )
    }
}

private val previewCampuses = listOf(
    Campus(CampusCode("MONSERRAT"), "Sede Monserrat", emptyList()),
    Campus(CampusCode("BELGRANO"), "Sede Belgrano", emptyList()),
)

private fun previewState(
    content: ContentState<List<Trip>>,
    filters: TripFilters = TripFilters(),
    isOffline: Boolean = false,
) = FeedUiState(
    content = content,
    filters = filters,
    campuses = previewCampuses,
    dateOptions = listOf(LocalDate.now(BusinessZone)),
    isOffline = isOffline,
    lastUpdatedAt = LocalTime.of(21, 40),
)

@Preview(showBackground = true, name = "Con viajes")
@Composable
private fun FeedScreenContentPreview() {
    UniRideTheme { FeedScreen(
            state = previewState(ContentState.Success(PreviewData.trips), filters = TripFilters(direction = Direction.VUELTA)),
            onAction = {},
        ) }
}

@Preview(showBackground = true, name = "Cargando")
@Composable
private fun FeedScreenLoadingPreview() {
    UniRideTheme { FeedScreen(state = previewState(ContentState.Loading), onAction = {}) }
}

@Preview(showBackground = true, name = "Vacío por filtros")
@Composable
private fun FeedScreenEmptyPreview() {
    UniRideTheme {
        FeedScreen(
            state = previewState(ContentState.Empty, filters = TripFilters(shift = Shift.MANANA)),
            onAction = {},
        )
    }
}

@Preview(showBackground = true, name = "Primer inicio sin conexión")
@Composable
private fun FeedScreenFirstStartOfflinePreview() {
    UniRideTheme {
        FeedScreen(
            state = previewState(ContentState.Error(R.string.feed_first_start_offline), isOffline = true)
                .copy(lastUpdatedAt = null),
            onAction = {},
        )
    }
}

@Preview(showBackground = true, name = "Sin conexión con datos guardados")
@Composable
private fun FeedScreenOfflinePreview() {
    UniRideTheme {
        FeedScreen(state = previewState(ContentState.Success(PreviewData.trips), isOffline = true), onAction = {})
    }
}
