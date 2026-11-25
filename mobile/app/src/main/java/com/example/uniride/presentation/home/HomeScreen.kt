package com.example.uniride.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.uniride.R
import com.example.uniride.domain.model.AppMode
import com.example.uniride.presentation.driver.DriverHomeRoute
import com.example.uniride.presentation.navigation.PlaceholderAction
import com.example.uniride.presentation.navigation.PlaceholderScreen
import com.example.uniride.presentation.theme.UniRideTheme

/**
 * What the dynamic start shows in driver mode: C1 (RF02) with the callbacks to publish a trip
 * and to open the panel of one. It exists so instrumented tests, which have no Hilt, can swap
 * C1 (its ViewModel comes from Hilt) for a plain composable; the app always uses [DefaultDriverHome].
 */
typealias DriverHome = @Composable (onPublishTrip: () -> Unit, onOpenTrip: (tripId: String) -> Unit) -> Unit

val DefaultDriverHome: DriverHome = { onPublishTrip, onOpenTrip ->
    DriverHomeRoute(onPublishTrip = onPublishTrip, onOpenTrip = onOpenTrip)
}

/**
 * Dynamic start. Passenger: B6 card of the active trip or an invitation to explore (placeholder
 * until RF04). Driver: C1 Inicio del conductor (RF02).
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
    driverHome: DriverHome = DefaultDriverHome,
) {
    when (state.appMode) {
        AppMode.PASAJERO -> PlaceholderScreen(
            titleRes = R.string.home_passenger_title,
            modifier = modifier,
            actions = listOf(
                PlaceholderAction(R.string.placeholder_action_my_trip) { onAction(HomeAction.OpenMyTrip) },
            ),
        )

        AppMode.CONDUCTOR -> Box(modifier) {
            driverHome(
                { onAction(HomeAction.PublishTrip) },
                { tripId -> onAction(HomeAction.OpenDriverPanel(tripId)) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPassengerPreview() {
    UniRideTheme {
        HomeScreen(state = HomeUiState(AppMode.PASAJERO), onAction = {})
    }
}
