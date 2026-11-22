package com.example.uniride.presentation.home

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.uniride.R
import com.example.uniride.domain.model.AppMode
import com.example.uniride.presentation.navigation.PlaceholderAction
import com.example.uniride.presentation.navigation.PlaceholderScreen
import com.example.uniride.presentation.publish.PublishTripRoute
import com.example.uniride.presentation.reservations.DriverPanelRoute
import com.example.uniride.presentation.reservations.MyTripRoute
import kotlinx.serialization.Serializable

/**
 * Dynamic start (RF04): depends on the mode and on the state of the user. Passenger: card of
 * the active trip (B6) or invitation to explore. Driver: C1 without a trip, or C4 with requests.
 */
@Serializable
data object HomeRoute

/** Ids used by the placeholders until RF04 reads the real trip from Room. */
private const val PLACEHOLDER_TRIP_ID = "placeholder-trip"

fun NavGraphBuilder.homeGraph(navController: NavController, appMode: () -> AppMode) {
    composable<HomeRoute> {
        when (appMode()) {
            AppMode.PASAJERO -> PlaceholderScreen(
                titleRes = R.string.home_passenger_title,
                screenCode = "B6",
                actions = listOf(
                    PlaceholderAction(R.string.placeholder_action_my_trip) {
                        navController.navigate(MyTripRoute(PLACEHOLDER_TRIP_ID))
                    },
                ),
            )

            AppMode.CONDUCTOR -> PlaceholderScreen(
                titleRes = R.string.home_driver_title,
                screenCode = "C1",
                actions = listOf(
                    PlaceholderAction(R.string.placeholder_action_publish) {
                        navController.navigate(PublishTripRoute)
                    },
                    PlaceholderAction(R.string.placeholder_action_driver_panel) {
                        navController.navigate(DriverPanelRoute(PLACEHOLDER_TRIP_ID))
                    },
                ),
            )
        }
    }
}
