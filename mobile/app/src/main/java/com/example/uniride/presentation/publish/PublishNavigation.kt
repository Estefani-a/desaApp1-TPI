package com.example.uniride.presentation.publish

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.uniride.R
import com.example.uniride.presentation.navigation.PlaceholderAction
import com.example.uniride.presentation.navigation.PlaceholderScreen
import kotlinx.serialization.Serializable

/** C2 Formulario de publicación (RF02). */
@Serializable
data object PublishTripRoute

fun NavGraphBuilder.publishGraph(navController: NavController) {
    composable<PublishTripRoute> {
        PlaceholderScreen(
            titleRes = R.string.publish_title,
            actions = listOf(
                PlaceholderAction(R.string.placeholder_action_back) { navController.popBackStack() },
            ),
        )
    }
}
