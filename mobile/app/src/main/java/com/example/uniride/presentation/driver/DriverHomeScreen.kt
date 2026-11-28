package com.example.uniride.presentation.driver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniride.R
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.RejectedTrip
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.model.TripStatus
import com.example.uniride.presentation.common.ContentState
import com.example.uniride.presentation.common.ContentStateHost
import com.example.uniride.presentation.common.EmptyState
import com.example.uniride.presentation.common.LocalAppSnackbar
import com.example.uniride.presentation.common.OfflineBanner
import com.example.uniride.presentation.common.PreviewData
import com.example.uniride.presentation.common.PrimaryButton
import com.example.uniride.presentation.common.TripCard
import com.example.uniride.presentation.common.dateLabel
import com.example.uniride.presentation.common.formatTime
import com.example.uniride.presentation.common.messageRes
import com.example.uniride.presentation.push.NotificationPermissionPrompt
import com.example.uniride.presentation.theme.Dimens
import com.example.uniride.presentation.theme.UniRideTheme

/**
 * Gets the [DriverHomeViewModel], collects its state and turns its effects into the callbacks:
 * [onPublishTrip] opens C2 and [onOpenTrip] opens the panel of a trip (C4).
 */
@Composable
fun DriverHomeRoute(
    onPublishTrip: () -> Unit,
    onOpenTrip: (tripId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DriverHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val appSnackbar = LocalAppSnackbar.current
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                DriverHomeEffect.NavigateToPublish -> onPublishTrip()
                is DriverHomeEffect.NavigateToPanel -> onOpenTrip(effect.tripId)
                is DriverHomeEffect.ShowMessage -> {
                    val message = resources.getString(effect.messageRes)
                    // The shell's snackbar outlives the screen; previews and tests use their own
                    if (appSnackbar != null) appSnackbar.show(message) else snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    NotificationPermissionPrompt(
        rationaleRes = R.string.push_permission_driver_rationale,
        shouldPrompt = state.shouldPromptNotifications,
    )

    DriverHomeScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

/**
 * C1 Inicio del conductor (RF02): notices of rejected trips, the upcoming trips and the main
 * button to publish. Offline it keeps showing what is saved under the "Sin conexión" banner,
 * and a trip not synced yet carries its "Pendiente de sincronizar" mark (12.1).
 */
@Composable
fun DriverHomeScreen(
    state: DriverHomeUiState,
    onAction: (DriverHomeAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    // The shell with the bottom bar already leaves the space of the system bars
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isOffline) {
                OfflineBanner(lastUpdatedAt = null)
            }
            ContentStateHost(
                state = state.content,
                onRetry = { onAction(DriverHomeAction.Refresh) },
                modifier = Modifier.weight(1f),
                empty = {
                    EmptyState(
                        title = stringResource(R.string.driver_home_empty_title),
                        message = stringResource(R.string.driver_home_empty_message),
                        actionLabel = stringResource(R.string.driver_home_publish),
                        onAction = { onAction(DriverHomeAction.PublishTrip) },
                    )
                },
            ) { content ->
                DriverTripList(content = content, onAction = onAction)
            }
            // Empty and loading have no list to leave room for: the empty state has its own button
            if (state.content is ContentState.Success || state.content is ContentState.Error) {
                PrimaryButton(
                    text = stringResource(R.string.driver_home_publish),
                    onClick = { onAction(DriverHomeAction.PublishTrip) },
                    modifier = Modifier.padding(Dimens.ScreenPadding),
                )
            }
        }
    }
}

@Composable
private fun DriverTripList(content: DriverHomeContent, onAction: (DriverHomeAction) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.ScreenPadding,
            end = Dimens.ScreenPadding,
            top = Dimens.SpaceL,
            bottom = Dimens.SpaceL,
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
    ) {
        items(content.rejections, key = { "rejected-" + it.tripId.value }) { rejected ->
            RejectionCard(
                rejected = rejected,
                onDismiss = { onAction(DriverHomeAction.DismissRejection(rejected.tripId.value)) },
            )
        }
        if (content.trips.isNotEmpty()) {
            item(key = "title") {
                Text(
                    text = stringResource(R.string.driver_home_trips_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
            }
        }
        items(content.trips, key = { it.id.value }) { trip ->
            TripCard(
                trip = trip,
                campusName = content.campusNames[trip.campus],
                isPendingSync = trip.id in content.pendingTripIds,
                onClick = { onAction(DriverHomeAction.OpenTrip(trip.id.value)) },
            )
        }
    }
}

/** Notice of a trip the server refused: the reason and a button to forget it. */
@Composable
private fun RejectionCard(rejected: RejectedTrip, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.SpaceL),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
        ) {
            Text(
                text = stringResource(R.string.driver_home_rejected_title),
                style = MaterialTheme.typography.titleMedium,
            )
            rejected.trip?.let { trip ->
                Text(
                    text = stringResource(
                        R.string.driver_home_rejected_trip,
                        dateLabel(trip.schedule.departureDate),
                        formatTime(trip.schedule.departureTime),
                        trip.route.destinationNeighborhood,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = stringResource(rejected.error.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.End)
                    .heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(stringResource(R.string.driver_home_rejected_dismiss))
            }
        }
    }
}

private val previewCampusNames = mapOf(
    PreviewData.trip.campus to "Sede Monserrat",
    PreviewData.fullTrip.campus to "Sede Belgrano",
)

private val previewContent = DriverHomeContent(
    trips = PreviewData.trips,
    pendingTripIds = setOf(PreviewData.fullTrip.id),
    campusNames = previewCampusNames,
)

private val previewRejection = RejectedTrip(
    tripId = TripId("rejected"),
    trip = PreviewData.trip.copy(id = TripId("rejected"), status = TripStatus.CANCELADO),
    error = DataError.VALIDATION,
)

@Preview(showBackground = true, name = "Cargando")
@Composable
private fun DriverHomeLoadingPreview() {
    UniRideTheme { DriverHomeScreen(state = DriverHomeUiState(ContentState.Loading), onAction = {}) }
}

@Preview(showBackground = true, name = "Vacío")
@Composable
private fun DriverHomeEmptyPreview() {
    UniRideTheme { DriverHomeScreen(state = DriverHomeUiState(ContentState.Empty), onAction = {}) }
}

@Preview(showBackground = true, name = "Con viajes y uno pendiente")
@Composable
private fun DriverHomeContentPreview() {
    UniRideTheme {
        DriverHomeScreen(state = DriverHomeUiState(ContentState.Success(previewContent)), onAction = {})
    }
}

@Preview(showBackground = true, name = "Con un rechazo")
@Composable
private fun DriverHomeRejectionPreview() {
    UniRideTheme {
        DriverHomeScreen(
            state = DriverHomeUiState(
                ContentState.Success(previewContent.copy(rejections = listOf(previewRejection))),
            ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true, name = "Sin conexión")
@Composable
private fun DriverHomeOfflinePreview() {
    UniRideTheme {
        DriverHomeScreen(
            state = DriverHomeUiState(ContentState.Success(previewContent), isOffline = true),
            onAction = {},
        )
    }
}

@Preview(showBackground = true, name = "Error sin viajes")
@Composable
private fun DriverHomeErrorPreview() {
    UniRideTheme {
        DriverHomeScreen(
            state = DriverHomeUiState(ContentState.Error(R.string.common_error_server)),
            onAction = {},
        )
    }
}
