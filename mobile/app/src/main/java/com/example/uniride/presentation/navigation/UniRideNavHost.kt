package com.example.uniride.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.navigation
import com.example.uniride.domain.model.AppMode
import com.example.uniride.presentation.auth.AuthGraphRoute
import com.example.uniride.presentation.auth.authGraph
import com.example.uniride.presentation.detail.detailGraph
import com.example.uniride.presentation.feed.feedGraph
import com.example.uniride.presentation.home.HomeRoute
import com.example.uniride.presentation.home.homeGraph
import com.example.uniride.presentation.profile.profileGraph
import com.example.uniride.presentation.publish.publishGraph
import com.example.uniride.presentation.reservations.reservationsGraph
import kotlinx.serialization.Serializable

/** Subgraph of everything that needs a session. */
@Serializable
data object MainGraphRoute

/**
 * Root of the navigation (CONVENCIONES §7.6). It only calls the subgraph of each area: every
 * area adds its screens in its own `xxxGraph` file, not here.
 *
 * @param startDestination [AuthGraphRoute] without session, [MainGraphRoute] with one
 */
@Composable
fun UniRideNavHost(
    navController: NavHostController,
    appMode: AppMode,
    modifier: Modifier = Modifier,
    startDestination: Any = AuthGraphRoute,
) {
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        authGraph(navController)
        navigation<MainGraphRoute>(startDestination = HomeRoute) {
            homeGraph(navController, appMode = { appMode })
            feedGraph(navController)
            detailGraph(navController)
            publishGraph(navController)
            reservationsGraph(navController)
            profileGraph(navController)
        }
    }
}
