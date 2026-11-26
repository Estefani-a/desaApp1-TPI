package com.example.uniride.presentation.reservations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniride.R
import com.example.uniride.domain.model.Participant
import com.example.uniride.domain.model.Reservation
import com.example.uniride.domain.model.ReservationId
import com.example.uniride.domain.model.ReservationRejection
import com.example.uniride.domain.model.ReservationStatus
import com.example.uniride.domain.model.UserId
import com.example.uniride.domain.usecase.reservation.DriverPanel
import com.example.uniride.domain.usecase.reservation.DriverRequest
import com.example.uniride.presentation.common.ContentState
import com.example.uniride.presentation.common.ContentStateHost
import com.example.uniride.presentation.common.EmptyState
import com.example.uniride.presentation.common.LocalAppSnackbar
import com.example.uniride.presentation.common.OfflineBanner
import com.example.uniride.presentation.common.PreviewData
import com.example.uniride.presentation.common.PrimaryButton
import com.example.uniride.presentation.common.TripCard
import com.example.uniride.presentation.common.UniRideTopBar
import com.example.uniride.presentation.common.formatRating
import com.example.uniride.presentation.reservations.components.DetailRow
import com.example.uniride.presentation.reservations.components.RejectionNotice
import com.example.uniride.presentation.reservations.components.ReservationStatusRow
import com.example.uniride.presentation.theme.Dimens
import com.example.uniride.presentation.theme.UniRideTheme
import java.time.Instant

@Composable
fun DriverPanelRoute(
    onBack: () -> Unit,
    onOpenTrip: (tripId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DriverPanelViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val appSnackbar = LocalAppSnackbar.current
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DriverPanelEffect.NavigateToTrip -> onOpenTrip(effect.tripId)
                is DriverPanelEffect.ShowMessage -> {
                    val message = resources.getString(effect.messageRes)
                    if (appSnackbar != null) appSnackbar.show(message) else snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    DriverPanelScreen(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

/**
 * C4 Panel del conductor (RF04): the trip on top and its requests, pending first. A pending
 * request has "Aceptar" and "Rechazar" until the trip leaves; the passenger's phone shows up
 * once the request is accepted (RNF07). Offline the answers are saved and sent later (12.1).
 */
@Composable
fun DriverPanelScreen(
    state: DriverPanelUiState,
    onAction: (DriverPanelAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            UniRideTopBar(
                title = stringResource(R.string.driver_panel_title),
                onBack = onBack,
                windowInsets = WindowInsets(0),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isOffline) {
                OfflineBanner(lastUpdatedAt = null)
            }
            ContentStateHost(
                state = state.content,
                onRetry = { onAction(DriverPanelAction.Refresh) },
                modifier = Modifier.weight(1f),
            ) { panel ->
                RequestList(panel = panel, answeringId = state.answeringId, onAction = onAction)
            }
        }
    }
}

@Composable
private fun RequestList(panel: DriverPanel, answeringId: String?, onAction: (DriverPanelAction) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
    ) {
        item(key = "trip") {
            TripCard(trip = panel.trip, onClick = { onAction(DriverPanelAction.OpenTrip) })
        }
        item(key = "title") {
            Text(
                text = stringResource(R.string.driver_panel_requests_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(top = Dimens.SpaceS)
                    .semantics { heading() },
            )
        }
        if (panel.requests.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    title = stringResource(R.string.driver_panel_empty_title),
                    message = stringResource(R.string.driver_panel_empty_message),
                )
            }
        }
        items(panel.requests, key = { it.reservation.id.value }) { request ->
            RequestCard(
                request = request,
                canAnswer = request.canBeAnswered && !panel.hasStarted,
                isAnswering = answeringId == request.reservation.id.value,
                isBusy = answeringId != null,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun RequestCard(
    request: DriverRequest,
    canAnswer: Boolean,
    isAnswering: Boolean,
    isBusy: Boolean,
    onAction: (DriverPanelAction) -> Unit,
) {
    val passenger = request.reservation.passenger
    val id = request.reservation.id.value
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.SpaceL),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
        ) {
            request.rejection?.let { rejection ->
                RejectionNotice(
                    messageRes = rejection.driverMessageRes(),
                    onDismiss = { onAction(DriverPanelAction.DismissRejection(id)) },
                )
            }
            Text(text = passenger.displayName, style = MaterialTheme.typography.titleMedium)
            passenger.rating?.let { rating ->
                Text(
                    text = stringResource(R.string.driver_panel_rating, formatRating(rating)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ReservationStatusRow(status = request.reservation.status, isPendingSync = request.isPendingSync)
            passenger.phone?.let { DetailRow(label = stringResource(R.string.detail_phone), value = it) }
            if (canAnswer) {
                AnswerButtons(
                    passengerName = passenger.displayName,
                    isAnswering = isAnswering,
                    enabled = !isBusy,
                    onAccept = { onAction(DriverPanelAction.Accept(id)) },
                    onReject = { onAction(DriverPanelAction.Reject(id)) },
                )
            }
        }
    }
}

@Composable
private fun AnswerButtons(
    passengerName: String,
    isAnswering: Boolean,
    enabled: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    val rejectDescription = stringResource(R.string.driver_panel_reject_description, passengerName)
    val acceptDescription = stringResource(R.string.driver_panel_accept_description, passengerName)
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
        OutlinedButton(
            onClick = onReject,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = Dimens.MinTouchTarget)
                .semantics { contentDescription = rejectDescription },
        ) {
            Text(stringResource(R.string.driver_panel_reject))
        }
        PrimaryButton(
            text = stringResource(R.string.driver_panel_accept),
            onClick = onAccept,
            enabled = enabled,
            isLoading = isAnswering,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = acceptDescription },
        )
    }
}

private fun previewRequest(
    id: String,
    name: String,
    status: ReservationStatus,
    phone: String? = null,
    isPendingSync: Boolean = false,
    rejection: ReservationRejection? = null,
) = DriverRequest(
    reservation = Reservation(
        id = ReservationId(id),
        tripId = PreviewData.trip.id,
        passenger = Participant(UserId("user-$id"), name, 4.6, phone = phone),
        status = status,
        reservedAt = Instant.parse("2026-11-20T21:41:03-03:00"),
        updatedAt = Instant.parse("2026-11-20T21:41:03-03:00"),
    ),
    isPendingSync = isPendingSync,
    rejection = rejection,
)

@Preview(showBackground = true)
@Composable
private fun DriverPanelPreview() {
    val panel = DriverPanel(
        trip = PreviewData.trip,
        requests = listOf(
            previewRequest("1", "Martín G.", ReservationStatus.PENDIENTE),
            previewRequest("2", "Ana R.", ReservationStatus.ACEPTADA, phone = "+5491155550000", isPendingSync = true),
            previewRequest(
                "3",
                "Juan P.",
                ReservationStatus.PENDIENTE,
                rejection = ReservationRejection.NO_SEATS,
            ),
        ),
        hasStarted = false,
    )
    UniRideTheme {
        DriverPanelScreen(
            state = DriverPanelUiState(content = ContentState.Success(panel)),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DriverPanelEmptyOfflinePreview() {
    UniRideTheme {
        DriverPanelScreen(
            state = DriverPanelUiState(
                content = ContentState.Success(DriverPanel(PreviewData.trip, emptyList(), hasStarted = false)),
                isOffline = true,
            ),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DriverPanelErrorPreview() {
    UniRideTheme {
        DriverPanelScreen(
            state = DriverPanelUiState(content = ContentState.Error(R.string.common_error_server)),
            onAction = {},
            onBack = {},
        )
    }
}
