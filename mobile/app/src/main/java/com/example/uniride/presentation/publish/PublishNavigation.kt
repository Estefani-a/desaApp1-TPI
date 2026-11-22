package com.example.uniride.presentation.publish

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.uniride.R
import com.example.uniride.presentation.navigation.PlaceholderScreen
import kotlinx.serialization.Serializable

/** C2 Formulario de publicación (RF02). */
@Serializable
data object PublishTripRoute

@Suppress("UNUSED_PARAMETER") // RF02 uses it to go back after publishing
fun NavGraphBuilder.publishGraph(navController: NavController) {
    composable<PublishTripRoute> {
        PlaceholderScreen(titleRes = R.string.publish_title, screenCode = "C2")
    }
}
