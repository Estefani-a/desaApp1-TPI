package com.example.uniride.presentation.detail

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.uniride.R
import com.example.uniride.presentation.navigation.PlaceholderAction
import com.example.uniride.presentation.navigation.PlaceholderScreen
import kotlinx.serialization.Serializable

/** B3 Detalle del viaje, with "Solicitar asiento" (RF04). Reads the trip from Room. */
@Serializable
data class TripDetailRoute(val tripId: String)

/** B5 Asiento confirmado: the request was saved (maybe still pending to sync). */
@Serializable
data class SeatRequestedRoute(val reservationId: String)

fun NavGraphBuilder.detailGraph(navController: NavController) {
    composable<TripDetailRoute> { entry ->
        val route = entry.toRoute<TripDetailRoute>()
        PlaceholderScreen(
            titleRes = R.string.detail_title,
            screenCode = "B3",
            actions = listOf(
                PlaceholderAction(R.string.placeholder_action_request_seat) {
                    navController.navigate(SeatRequestedRoute(reservationId = "placeholder-${route.tripId}")) {
                        popUpTo<TripDetailRoute> { inclusive = true }
                    }
                },
            ),
        )
    }
    composable<SeatRequestedRoute> {
        PlaceholderScreen(
            titleRes = R.string.seat_requested_title,
            screenCode = "B5",
            actions = listOf(
                PlaceholderAction(R.string.placeholder_action_back) { navController.popBackStack() },
            ),
        )
    }
}
