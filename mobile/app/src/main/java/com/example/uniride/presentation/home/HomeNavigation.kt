package com.example.uniride.presentation.home

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.uniride.domain.model.AppMode
import com.example.uniride.presentation.navigation.PLACEHOLDER_TRIP_ID
import com.example.uniride.presentation.publish.PublishTripRoute
import com.example.uniride.presentation.reservations.DriverPanelRoute
import com.example.uniride.presentation.reservations.MyTripRoute
import kotlinx.serialization.Serializable

/** Dynamic start (RF04): what it shows depends on the mode and on the state of the user. */
@Serializable
data object HomeRoute

fun NavGraphBuilder.homeGraph(navController: NavController, appMode: () -> AppMode) {
    composable<HomeRoute> {
        HomeScreen(
            state = HomeUiState(appMode = appMode()),
            onAction = { action ->
                when (action) {
                    HomeAction.OpenMyTrip -> navController.navigate(MyTripRoute(PLACEHOLDER_TRIP_ID))
                    HomeAction.PublishTrip -> navController.navigate(PublishTripRoute)
                    HomeAction.OpenDriverPanel -> navController.navigate(DriverPanelRoute(PLACEHOLDER_TRIP_ID))
                }
            },
        )
    }
}
