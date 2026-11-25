package com.example.uniride.presentation.feed

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.uniride.presentation.detail.TripDetailRoute
import kotlinx.serialization.Serializable

/** B1 Explorar: feed and filters (RF03). */
@Serializable
data object FeedRoute

fun NavGraphBuilder.feedGraph(navController: NavController) {
    composable<FeedRoute> {
        FeedRoute(onOpenTrip = { tripId -> navController.navigate(TripDetailRoute(tripId)) })
    }
}
