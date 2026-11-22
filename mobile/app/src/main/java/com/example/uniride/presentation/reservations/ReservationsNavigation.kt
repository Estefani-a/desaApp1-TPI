package com.example.uniride.presentation.reservations

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.uniride.R
import com.example.uniride.presentation.detail.TripDetailRoute
import com.example.uniride.presentation.navigation.PlaceholderAction
import com.example.uniride.presentation.navigation.PlaceholderScreen
import kotlinx.serialization.Serializable

/** B6 Mi viaje: trip of the passenger, readable offline (RF04). Opened by the push of an answer. */
@Serializable
data class MyTripRoute(val tripId: String)

/** C4 Panel del conductor: requests of a trip (RF04). Opened by the push of a new request. */
@Serializable
data class DriverPanelRoute(val tripId: String)

fun NavGraphBuilder.reservationsGraph(navController: NavController) {
    composable<MyTripRoute> {
        PlaceholderScreen(
            titleRes = R.string.my_trip_title,
            screenCode = "B6",
            actions = listOf(
                PlaceholderAction(R.string.placeholder_action_trip_detail) {
                    navController.navigate(TripDetailRoute(tripId = "placeholder-trip"))
                },
            ),
        )
    }
    composable<DriverPanelRoute> {
        PlaceholderScreen(titleRes = R.string.driver_panel_title, screenCode = "C4")
    }
}
