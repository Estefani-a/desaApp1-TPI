package com.example.uniride.presentation.publish

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** C2 Formulario de publicación (RF02). */
@Serializable
data object PublishTripRoute

fun NavGraphBuilder.publishGraph(navController: NavController) {
    composable<PublishTripRoute> {
        PublishTripRoute(onBack = { navController.popBackStack() })
    }
}
