package com.example.uniride.presentation.detail

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
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
import com.example.uniride.domain.usecase.reservation.TripDetail
import com.example.uniride.domain.validation.SeatRequestDecision
import com.example.uniride.presentation.common.ContentState
import com.example.uniride.presentation.common.ContentStateHost
import com.example.uniride.presentation.common.LocalAppSnackbar
import com.example.uniride.presentation.common.OfflineBanner
import com.example.uniride.presentation.common.PreviewData
import com.example.uniride.presentation.common.PrimaryButton
import com.example.uniride.presentation.common.StatusChip
import com.example.uniride.presentation.common.UniRideTopBar
import com.example.uniride.presentation.common.dateLabel
import com.example.uniride.presentation.common.formatMoney
import com.example.uniride.presentation.common.formatRating
import com.example.uniride.presentation.common.formatTime
import com.example.uniride.presentation.common.labelRes
import com.example.uniride.presentation.common.tone
import com.example.uniride.presentation.reservations.components.DetailRow
import com.example.uniride.presentation.reservations.components.RejectionNotice
import com.example.uniride.presentation.reservations.components.ReservationStatusRow
import com.example.uniride.presentation.reservations.messageRes
import com.example.uniride.presentation.reservations.passengerMessageRes
import com.example.uniride.presentation.theme.Dimens
import com.example.uniride.presentation.theme.UniRideTheme
import java.time.Instant

/**
 * Gets the [TripDetailViewModel] and turns its effects into navigation. [titleRes] tells B3
 * ("Detalle del viaje") from B6 ("Mi viaje"): same data, same actions.
 */
@Composable
fun TripDetailRoute(
    @StringRes titleRes: Int,
    onBack: () -> Unit,
    onSeatRequested: (tripId: String) -> Unit,
    onOpenDriverPanel: (tripId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TripDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val appSnackbar = LocalAppSnackbar.current
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TripDetailEffect.NavigateToSeatRequested -> onSeatRequested(effect.tripId)
                is TripDetailEffect.NavigateToDriverPanel -> onOpenDriverPanel(effect.tripId)
                is TripDetailEffect.ShowMessage -> {
                    val message = resources.getString(effect.messageRes)
                    if (appSnackbar != null) appSnackbar.show(message) else snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    TripDetailScreen(
        state = state,
        titleRes = titleRes,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

/**
 * B3 Detalle del viaje / B6 Mi viaje (RF04). The plate and the phone of the driver only show up
 * when the server sends them (accepted reservation, RNF07). The main action depends on who looks:
 * the driver goes to the requests (C4), a passenger asks for a seat or cancels; when it is not
 * allowed the screen says why. Offline it keeps the saved trip under the banner (12.1).
 */
@Composable
fun TripDetailScreen(
    state: TripDetailUiState,
    @StringRes titleRes: Int,
    onAction: (TripDetailAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            UniRideTopBar(title = stringResource(titleRes), onBack = onBack, windowInsets = WindowInsets(0))
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isOffline) {
                OfflineBanner(lastUpdatedAt = null)
            }
            ContentStateHost(
                state = state.content,
                onRetry = { onAction(TripDetailAction.Refresh) },
                modifier = Modifier.weight(1f),
            ) { detail ->
                Column(modifier = Modifier.fillMaxSize()) {
                    TripDetailContent(detail = detail, onAction = onAction, modifier = Modifier.weight(1f))
                    MainAction(detail = detail, isSubmitting = state.isSubmitting, onAction = onAction)
                }
            }
        }
    }

    if (state.isConfirmingCancel) {
        AlertDialog(
            onDismissRequest = { onAction(TripDetailAction.DismissCancel) },
            title = { Text(stringResource(R.string.detail_cancel_confirm_title)) },
            text = { Text(stringResource(R.string.detail_cancel_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { onAction(TripDetailAction.ConfirmCancel) }) {
                    Text(stringResource(R.string.detail_cancel_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(TripDetailAction.DismissCancel) }) {
                    Text(stringResource(R.string.detail_cancel_keep))
                }
            },
        )
    }
}

@Composable
private fun TripDetailContent(detail: TripDetail, onAction: (TripDetailAction) -> Unit, modifier: Modifier = Modifier) {
    val trip = detail.trip
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL),
    ) {
        detail.rejection?.let { rejection ->
            RejectionNotice(
                messageRes = rejection.passengerMessageRes(),
                onDismiss = { onAction(TripDetailAction.DismissRejection) },
            )
        }
        StatusChip(text = stringResource(trip.status.labelRes), tone = trip.status.tone)
        DetailRow(
            label = stringResource(R.string.detail_departure),
            value = stringResource(
                R.string.detail_departure_value,
                dateLabel(trip.schedule.departureDate),
                formatTime(trip.schedule.departureTime),
            ),
        )
        DetailRow(label = stringResource(R.string.detail_meeting_point), value = trip.route.meetingPoint)
        DetailRow(
            label = stringResource(R.string.detail_destination),
            value = listOfNotNull(trip.route.destinationNeighborhood, trip.route.destinationReference).joinToString(" · "),
        )
        DetailRow(
            label = stringResource(R.string.detail_contribution),
            value = formatMoney(trip.contributionPerSeat),
        )
        Text(
            text = stringResource(R.string.detail_seats_free, trip.availableSeats, trip.totalSeats),
            style = MaterialTheme.typography.bodyMedium,
        )
        HorizontalDivider()
        DetailRow(
            label = stringResource(R.string.detail_driver),
            value = listOfNotNull(trip.driver.displayName, trip.driver.rating?.let { "${formatRating(it)} ★" }).joinToString(" · "),
        )
        DetailRow(
            label = stringResource(R.string.detail_vehicle),
            value = stringResource(R.string.detail_vehicle_value, trip.vehicle.model, trip.vehicle.color),
        )
        trip.vehicle.plate?.let { DetailRow(label = stringResource(R.string.detail_plate), value = it) }
        trip.driver.phone?.let { DetailRow(label = stringResource(R.string.detail_phone), value = it) }
        if (!detail.isDriver && trip.vehicle.plate == null) {
            Text(
                text = stringResource(R.string.detail_contact_hidden),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        detail.myReservation?.let { mine ->
            HorizontalDivider()
            Text(
                text = stringResource(R.string.detail_my_reservation),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            ReservationStatusRow(status = mine.status, isPendingSync = detail.isPendingSync)
        }
    }
}

/**
 * The action at the bottom (RNF01): "Ver solicitudes" for the driver, "Cancelar reserva" for a
 * passenger with an active reservation, "Solicitar asiento" when allowed; otherwise the reason.
 */
@Composable
private fun MainAction(detail: TripDetail, isSubmitting: Boolean, onAction: (TripDetailAction) -> Unit) {
    Column(
        modifier = Modifier.padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
    ) {
        when {
            detail.isDriver -> PrimaryButton(
                text = stringResource(R.string.detail_open_driver_panel),
                onClick = { onAction(TripDetailAction.OpenDriverPanel) },
            )

            detail.canCancel -> OutlinedButton(
                onClick = { onAction(TripDetailAction.AskCancel) },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(stringResource(R.string.detail_cancel_reservation))
            }

            detail.seatRequest == SeatRequestDecision.ALLOWED -> PrimaryButton(
                text = stringResource(R.string.detail_request_seat),
                onClick = { onAction(TripDetailAction.RequestSeat) },
                isLoading = isSubmitting,
            )

            else -> detail.seatRequest.messageRes?.let { reason ->
                // A started trip with an accepted reservation has nothing to say here
                if (detail.myReservation?.isActive != true) {
                    Text(
                        text = stringResource(reason),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private val previewReservation = Reservation(
    id = ReservationId("res-1"),
    tripId = PreviewData.trip.id,
    passenger = Participant(UserId("passenger-1"), "Martín G.", 4.5, phone = null),
    status = ReservationStatus.PENDIENTE,
    reservedAt = Instant.parse("2026-11-20T21:41:03-03:00"),
    updatedAt = Instant.parse("2026-11-20T21:41:03-03:00"),
)

private fun previewDetail(
    myReservation: Reservation? = null,
    seatRequest: SeatRequestDecision = SeatRequestDecision.ALLOWED,
    rejection: ReservationRejection? = null,
    isPendingSync: Boolean = false,
    trip: com.example.uniride.domain.model.Trip = PreviewData.trip,
) = TripDetail(
    trip = trip,
    myReservation = myReservation,
    isDriver = false,
    seatRequest = seatRequest,
    hasStarted = false,
    isPendingSync = isPendingSync,
    rejection = rejection,
)

@Preview(showBackground = true)
@Composable
private fun TripDetailRequestPreview() {
    UniRideTheme {
        TripDetailScreen(
            state = TripDetailUiState(content = ContentState.Success(previewDetail())),
            titleRes = R.string.detail_title,
            onAction = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TripDetailPendingOfflinePreview() {
    UniRideTheme {
        TripDetailScreen(
            state = TripDetailUiState(
                content = ContentState.Success(
                    previewDetail(
                        myReservation = previewReservation,
                        seatRequest = SeatRequestDecision.ALREADY_RESERVED,
                        isPendingSync = true,
                    ),
                ),
                isOffline = true,
            ),
            titleRes = R.string.my_trip_title,
            onAction = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TripDetailAcceptedWithRejectionPreview() {
    val accepted = previewReservation.copy(status = ReservationStatus.ACEPTADA)
    val trip = PreviewData.trip.copy(
        vehicle = PreviewData.trip.vehicle.copy(plate = "AB123CD"),
        driver = PreviewData.trip.driver.copy(phone = "+5491155550000"),
    )
    UniRideTheme {
        TripDetailScreen(
            state = TripDetailUiState(
                content = ContentState.Success(
                    previewDetail(
                        myReservation = accepted,
                        seatRequest = SeatRequestDecision.ALREADY_RESERVED,
                        rejection = ReservationRejection.TRIP_ALREADY_DEPARTED,
                        trip = trip,
                    ),
                ),
            ),
            titleRes = R.string.my_trip_title,
            onAction = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TripDetailLoadingPreview() {
    UniRideTheme {
        TripDetailScreen(state = TripDetailUiState(), titleRes = R.string.detail_title, onAction = {}, onBack = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun TripDetailNotFoundPreview() {
    UniRideTheme {
        TripDetailScreen(
            state = TripDetailUiState(content = ContentState.Error(R.string.detail_not_found, canRetry = false)),
            titleRes = R.string.detail_title,
            onAction = {},
            onBack = {},
        )
    }
}
