package com.example.uniride.presentation.home

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.uniride.domain.model.AppMode
import com.example.uniride.presentation.navigation.TopLevelDestination
import com.example.uniride.presentation.navigation.navigateToTab
import com.example.uniride.presentation.publish.PublishTripRoute
import com.example.uniride.presentation.reservations.DriverPanelRoute
import com.example.uniride.presentation.reservations.MyTripRoute
import kotlinx.serialization.Serializable

/** Dynamic start (RF04): what it shows depends on the mode and on the state of the user. */
@Serializable
data object HomeRoute

/**
 * @param passengerHome start of the passenger mode and [driverHome] C1 of the driver mode. Only
 * instrumented tests, which have no Hilt, pass other ones.
 */
fun NavGraphBuilder.homeGraph(
    navController: NavController,
    appMode: () -> AppMode,
    passengerHome: PassengerHome = DefaultPassengerHome,
    driverHome: DriverHome = DefaultDriverHome,
) {
    composable<HomeRoute> {
        HomeScreen(
            state = HomeUiState(appMode = appMode()),
            passengerHome = passengerHome,
            driverHome = driverHome,
            onAction = { action ->
                when (action) {
                    is HomeAction.OpenMyTrip -> navController.navigate(MyTripRoute(action.tripId))
                    HomeAction.ExploreTrips -> navController.navigateToTab(TopLevelDestination.FEED)
                    HomeAction.PublishTrip -> navController.navigate(PublishTripRoute)
                    is HomeAction.OpenDriverPanel -> navController.navigate(DriverPanelRoute(action.tripId))
                }
            },
        )
    }
}
