package com.example.uniride.presentation.profile

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.uniride.R
import com.example.uniride.presentation.navigation.PlaceholderAction
import com.example.uniride.presentation.navigation.PlaceholderScreen
import com.example.uniride.presentation.navigation.navigateToAuth
import kotlinx.serialization.Serializable

/** Perfil y cierre de sesión (RF01). */
@Serializable
data object ProfileRoute

fun NavGraphBuilder.profileGraph(navController: NavController) {
    composable<ProfileRoute> {
        PlaceholderScreen(
            titleRes = R.string.profile_title,
            actions = listOf(
                PlaceholderAction(R.string.placeholder_action_logout) {
                    navController.navigateToAuth()
                },
            ),
        )
    }
}
