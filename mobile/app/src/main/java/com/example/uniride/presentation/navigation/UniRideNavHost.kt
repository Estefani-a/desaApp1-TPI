package com.example.uniride.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
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
 * Provisional until E0 (Hilt) and RF01: it starts without session in [AuthGraphRoute], and the
 * mode of the selector lives here with `rememberSaveable`. It moves to a ViewModel backed by
 * `UserPreferencesRepository.observeAppMode()` and `AuthRepository.observeIsLoggedIn()`.
 */
@Composable
fun UniRideNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    var appMode by rememberSaveable { mutableStateOf(AppMode.PASAJERO) }

    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val currentTab = currentDestination?.topLevelDestination(appMode)

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (currentTab != null) {
                UniRideBottomBar(
                    appMode = appMode,
                    selected = currentTab,
                    onModeChange = { mode ->
                        if (mode != appMode) {
                            appMode = mode
                            navController.navigateToTab(TopLevelDestination.HOME)
                        }
                    },
                    onTabClick = { tab -> navController.navigateToTab(tab) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AuthGraphRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
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
}

/** Tab of the bottom bar that [this] destination belongs to, or `null` to hide the bar. */
private fun NavDestination.topLevelDestination(mode: AppMode): TopLevelDestination? =
    TopLevelDestination.forMode(mode).firstOrNull { tab ->
        hierarchy.any { it.hasRoute(tab.routeClass) }
    }

/** Standard tab navigation: one copy of each tab, keeping the state of the others. */
private fun NavController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo<HomeRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
