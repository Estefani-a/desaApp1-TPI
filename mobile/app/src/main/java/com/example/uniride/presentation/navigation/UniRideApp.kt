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
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.uniride.domain.model.AppMode
import com.example.uniride.domain.model.Profile
import com.example.uniride.presentation.auth.AuthGraphRoute
import com.example.uniride.presentation.home.HomeRoute

/**
 * Shell of the app: the bottom bar with the mode selector around [UniRideNavHost].
 */
@Composable
fun UniRideApp(
    modifier: Modifier = Modifier,
    profile: Profile? = null,
    navController: NavHostController = rememberNavController(),
    startDestination: Any = AuthGraphRoute,
) {
    var selectedMode by rememberSaveable { mutableStateOf(AppMode.PASAJERO) }
    val modes = availableModes(profile)
    val appMode = effectiveMode(selectedMode, modes)

    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val currentTab = currentDestination?.topLevelDestination(appMode)

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (currentTab != null) {
                UniRideBottomBar(
                    appMode = appMode,
                    availableModes = modes,
                    selected = currentTab,
                    onModeChange = { mode ->
                        if (mode != appMode) {
                            selectedMode = mode
                            navController.navigateToTab(TopLevelDestination.HOME)
                        }
                    },
                    onTabClick = { tab -> navController.navigateToTab(tab) },
                )
            }
        },
    ) { innerPadding ->
        UniRideNavHost(
            navController = navController,
            appMode = appMode,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        )
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
