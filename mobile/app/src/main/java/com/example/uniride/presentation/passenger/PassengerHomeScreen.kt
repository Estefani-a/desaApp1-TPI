package com.example.uniride.presentation.passenger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniride.R
import com.example.uniride.domain.model.Participant
import com.example.uniride.domain.model.Reservation
import com.example.uniride.domain.model.ReservationId
import com.example.uniride.domain.model.ReservationRejection
import com.example.uniride.domain.model.ReservationStatus
import com.example.uniride.domain.model.UserId
import com.example.uniride.domain.usecase.reservation.MyTrip
import com.example.uniride.presentation.common.ContentState
import com.example.uniride.presentation.common.ContentStateHost
import com.example.uniride.presentation.common.EmptyState
import com.example.uniride.presentation.common.LocalAppSnackbar
import com.example.uniride.presentation.common.OfflineBanner
import com.example.uniride.presentation.common.PreviewData
import com.example.uniride.presentation.common.TripCard
import com.example.uniride.presentation.reservations.components.RejectionNotice
import com.example.uniride.presentation.reservations.components.ReservationStatusRow
import com.example.uniride.presentation.reservations.passengerMessageRes
import com.example.uniride.presentation.theme.Dimens
import com.example.uniride.presentation.theme.UniRideTheme
import java.time.Instant

@Composable
fun PassengerHomeRoute(
    onOpenMyTrip: (tripId: String) -> Unit,
    onExplore: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PassengerHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val appSnackbar = LocalAppSnackbar.current
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PassengerHomeEffect.NavigateToMyTrip -> onOpenMyTrip(effect.tripId)
                PassengerHomeEffect.NavigateToFeed -> onExplore()
                is PassengerHomeEffect.ShowMessage -> {
                    val message = resources.getString(effect.messageRes)
                    if (appSnackbar != null) appSnackbar.show(message) else snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    PassengerHomeScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

/**
 * Start of the passenger (RF04): the card of the next trip with the status of the request, or
 * an invitation to explore. Offline it keeps the saved trip under the banner (12.1).
 */
@Composable
fun PassengerHomeScreen(
    state: PassengerHomeUiState,
    onAction: (PassengerHomeAction) -> Unit,
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
                onRetry = { onAction(PassengerHomeAction.Refresh) },
                modifier = Modifier.weight(1f),
                empty = {
                    EmptyState(
                        title = stringResource(R.string.passenger_home_empty_title),
                        message = stringResource(R.string.passenger_home_empty_message),
                        actionLabel = stringResource(R.string.passenger_home_explore),
                        onAction = { onAction(PassengerHomeAction.ExploreTrips) },
                    )
                },
            ) { content ->
                NextTrip(content = content, onAction = onAction)
            }
        }
    }
}

@Composable
private fun NextTrip(content: PassengerHomeContent, onAction: (PassengerHomeAction) -> Unit) {
    val myTrip = content.myTrip
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
    ) {
        myTrip.rejection?.let { rejection ->
            RejectionNotice(
                messageRes = rejection.passengerMessageRes(),
                onDismiss = { onAction(PassengerHomeAction.DismissRejection) },
            )
        }
        Text(
            text = stringResource(R.string.passenger_home_next_trip),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        ReservationStatusRow(status = myTrip.reservation.status, isPendingSync = myTrip.isPendingSync)
        TripCard(
            trip = myTrip.trip,
            campusName = content.campusName,
            onClick = { onAction(PassengerHomeAction.OpenMyTrip) },
        )
    }
}

private val previewMyTrip = MyTrip(
    trip = PreviewData.trip,
    reservation = Reservation(
        id = ReservationId("res-1"),
        tripId = PreviewData.trip.id,
        passenger = Participant(UserId("passenger-1"), "Martín G.", 4.5, phone = null),
        status = ReservationStatus.ACEPTADA,
        reservedAt = Instant.parse("2026-11-20T21:41:03-03:00"),
        updatedAt = Instant.parse("2026-11-20T21:41:03-03:00"),
    ),
    isPendingSync = false,
    rejection = null,
)

@Preview(showBackground = true)
@Composable
private fun PassengerHomeTripPreview() {
    UniRideTheme {
        PassengerHomeScreen(
            state = PassengerHomeUiState(
                content = ContentState.Success(PassengerHomeContent(previewMyTrip, "Sede Monserrat")),
            ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PassengerHomeOfflineRejectionPreview() {
    val myTrip = previewMyTrip.copy(isPendingSync = true, rejection = ReservationRejection.TRIP_ALREADY_DEPARTED)
    UniRideTheme {
        PassengerHomeScreen(
            state = PassengerHomeUiState(
                content = ContentState.Success(PassengerHomeContent(myTrip, campusName = null)),
                isOffline = true,
            ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PassengerHomeEmptyPreview() {
    UniRideTheme {
        PassengerHomeScreen(state = PassengerHomeUiState(content = ContentState.Empty), onAction = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PassengerHomeErrorPreview() {
    UniRideTheme {
        PassengerHomeScreen(
            state = PassengerHomeUiState(content = ContentState.Error(R.string.common_error_no_connection)),
            onAction = {},
        )
    }
}
