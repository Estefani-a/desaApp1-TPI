package com.example.uniride.presentation.feed

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.uniride.R
import com.example.uniride.presentation.detail.TripDetailRoute
import com.example.uniride.presentation.navigation.PlaceholderAction
import com.example.uniride.presentation.navigation.PLACEHOLDER_TRIP_ID
import com.example.uniride.presentation.navigation.PlaceholderScreen
import kotlinx.serialization.Serializable

/** B1 Explorar: feed and filters (RF03). */
@Serializable
data object FeedRoute

fun NavGraphBuilder.feedGraph(navController: NavController) {
    composable<FeedRoute> {
        PlaceholderScreen(
            titleRes = R.string.feed_title,
            actions = listOf(
                PlaceholderAction(R.string.placeholder_action_trip_detail) {
                    navController.navigate(TripDetailRoute(tripId = PLACEHOLDER_TRIP_ID))
                },
            ),
        )
    }
}
