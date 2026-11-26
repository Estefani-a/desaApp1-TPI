package com.example.uniride.presentation.reservations

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.uniride.R
import com.example.uniride.presentation.detail.SeatRequestedRoute
import com.example.uniride.presentation.detail.TripDetailRoute
import kotlinx.serialization.Serializable

/** B6 Mi viaje: trip of the passenger, readable offline (RF04). Opened by the push of an answer. */
@Serializable
data class MyTripRoute(val tripId: String)

/** C4 Panel del conductor: requests of a trip (RF04). Opened by the push of a new request. */
@Serializable
data class DriverPanelRoute(val tripId: String)

fun NavGraphBuilder.reservationsGraph(navController: NavController) {
    composable<MyTripRoute> {
        // Same screen as B3: the trip with the reservation of the passenger on it
        TripDetailRoute(
            titleRes = R.string.my_trip_title,
            onBack = { navController.popBackStack() },
            onSeatRequested = { tripId ->
                navController.navigate(SeatRequestedRoute(tripId)) {
                    popUpTo<MyTripRoute> { inclusive = true }
                }
            },
            onOpenDriverPanel = { tripId -> navController.navigate(DriverPanelRoute(tripId)) },
        )
    }
    composable<DriverPanelRoute> {
        DriverPanelRoute(
            onBack = { navController.popBackStack() },
            onOpenTrip = { tripId -> navController.navigate(TripDetailRoute(tripId)) },
        )
    }
}
