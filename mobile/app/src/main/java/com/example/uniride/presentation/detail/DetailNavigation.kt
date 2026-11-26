package com.example.uniride.presentation.detail

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.uniride.R
import com.example.uniride.presentation.navigation.TopLevelDestination
import com.example.uniride.presentation.navigation.navigateToTab
import com.example.uniride.presentation.reservations.DriverPanelRoute
import com.example.uniride.presentation.reservations.MyTripRoute
import kotlinx.serialization.Serializable

/** B3 Detalle del viaje, with "Solicitar asiento" (RF04). Reads the trip from Room. */
@Serializable
data class TripDetailRoute(val tripId: String)

/** B5 Solicitud enviada: the request was saved (maybe still pending to sync). */
@Serializable
data class SeatRequestedRoute(val tripId: String)

fun NavGraphBuilder.detailGraph(navController: NavController) {
    composable<TripDetailRoute> {
        TripDetailRoute(
            titleRes = R.string.detail_title,
            onBack = { navController.popBackStack() },
            onSeatRequested = { tripId ->
                navController.navigate(SeatRequestedRoute(tripId)) {
                    popUpTo<TripDetailRoute> { inclusive = true }
                }
            },
            onOpenDriverPanel = { tripId -> navController.navigate(DriverPanelRoute(tripId)) },
        )
    }
    composable<SeatRequestedRoute> { entry ->
        val route = entry.toRoute<SeatRequestedRoute>()
        SeatRequestedRoute(
            onOpenMyTrip = {
                navController.navigate(MyTripRoute(route.tripId)) {
                    popUpTo<SeatRequestedRoute> { inclusive = true }
                }
            },
            onGoHome = { navController.navigateToTab(TopLevelDestination.HOME) },
        )
    }
}
