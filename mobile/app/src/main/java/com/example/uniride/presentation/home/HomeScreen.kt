package com.example.uniride.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.uniride.domain.model.AppMode
import com.example.uniride.presentation.driver.DriverHomeRoute
import com.example.uniride.presentation.passenger.PassengerHomeRoute

/**
 * What the dynamic start shows in passenger mode: the next trip or an invitation to explore
 * (RF04), with the callbacks to open the trip (B6) and the feed.
 *
 * Like [DriverHome], it exists so instrumented tests, which have no Hilt, can swap the screen
 * (its ViewModel comes from Hilt) for a plain composable; the app always uses [DefaultPassengerHome].
 */
typealias PassengerHome = @Composable (onOpenMyTrip: (tripId: String) -> Unit, onExplore: () -> Unit) -> Unit

val DefaultPassengerHome: PassengerHome = { onOpenMyTrip, onExplore ->
    PassengerHomeRoute(onOpenMyTrip = onOpenMyTrip, onExplore = onExplore)
}

/**
 * What the dynamic start shows in driver mode: C1 (RF02) with the callbacks to publish a trip
 * and to open the panel of one. It exists so instrumented tests, which have no Hilt, can swap
 * C1 (its ViewModel comes from Hilt) for a plain composable; the app always uses [DefaultDriverHome].
 */
typealias DriverHome = @Composable (onPublishTrip: () -> Unit, onOpenTrip: (tripId: String) -> Unit) -> Unit

val DefaultDriverHome: DriverHome = { onPublishTrip, onOpenTrip ->
    DriverHomeRoute(onPublishTrip = onPublishTrip, onOpenTrip = onOpenTrip)
}

/** Dynamic start: the passenger start (RF04) or C1 Inicio del conductor (RF02), by mode. */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
    passengerHome: PassengerHome = DefaultPassengerHome,
    driverHome: DriverHome = DefaultDriverHome,
) {
    Box(modifier) {
        when (state.appMode) {
            AppMode.PASAJERO -> passengerHome(
                { tripId -> onAction(HomeAction.OpenMyTrip(tripId)) },
                { onAction(HomeAction.ExploreTrips) },
            )

            AppMode.CONDUCTOR -> driverHome(
                { onAction(HomeAction.PublishTrip) },
                { tripId -> onAction(HomeAction.OpenDriverPanel(tripId)) },
            )
        }
    }
}
