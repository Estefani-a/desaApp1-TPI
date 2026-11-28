package com.example.uniride.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.uniride.R
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.presentation.common.PrimaryButton
import com.example.uniride.presentation.common.UniRideTopBar
import com.example.uniride.presentation.push.NotificationPermissionPrompt
import com.example.uniride.presentation.theme.Dimens
import com.example.uniride.presentation.theme.UniRideTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** State of B5: the request is saved on the phone; offline it is sent when the connection returns. */
data class SeatRequestedUiState(val isOffline: Boolean = false)

sealed interface SeatRequestedAction {
    data object OpenMyTrip : SeatRequestedAction
    data object GoHome : SeatRequestedAction
}

/** B5 only needs to know if the request already left the phone or waits for the connection. */
@HiltViewModel
class SeatRequestedViewModel @Inject constructor(
    observeConnectivity: ObserveConnectivityUseCase,
) : ViewModel() {
    val uiState: StateFlow<SeatRequestedUiState> = observeConnectivity()
        .map { isOnline -> SeatRequestedUiState(isOffline = !isOnline) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SeatRequestedUiState())
}

@Composable
fun SeatRequestedRoute(
    onOpenMyTrip: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SeatRequestedViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    NotificationPermissionPrompt(rationaleRes = R.string.push_permission_passenger_rationale)
    SeatRequestedScreen(
        state = state,
        onAction = { action ->
            when (action) {
                SeatRequestedAction.OpenMyTrip -> onOpenMyTrip()
                SeatRequestedAction.GoHome -> onGoHome()
            }
        },
        modifier = modifier,
    )
}

/** B5 Solicitud enviada (RF04): the request is saved and the driver will answer. */
@Composable
fun SeatRequestedScreen(
    state: SeatRequestedUiState,
    onAction: (SeatRequestedAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = { UniRideTopBar(title = stringResource(R.string.seat_requested_title), windowInsets = WindowInsets(0)) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(Dimens.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL),
        ) {
            Spacer(Modifier.weight(1f))
            Icon(
                painter = painterResource(R.drawable.ic_schedule),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimens.IconLarge),
            )
            Text(
                text = stringResource(R.string.seat_requested_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(
                    if (state.isOffline) R.string.seat_requested_offline else R.string.seat_requested_message,
                ),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                text = stringResource(R.string.seat_requested_my_trip),
                onClick = { onAction(SeatRequestedAction.OpenMyTrip) },
            )
            OutlinedButton(
                onClick = { onAction(SeatRequestedAction.GoHome) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(stringResource(R.string.seat_requested_home))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SeatRequestedPreview() {
    UniRideTheme {
        SeatRequestedScreen(state = SeatRequestedUiState(), onAction = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun SeatRequestedOfflinePreview() {
    UniRideTheme {
        SeatRequestedScreen(state = SeatRequestedUiState(isOffline = true), onAction = {})
    }
}
