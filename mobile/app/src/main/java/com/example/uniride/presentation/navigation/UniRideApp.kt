package com.example.uniride.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
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
import com.example.uniride.presentation.common.AppSnackbar
import com.example.uniride.presentation.common.LocalAppSnackbar
import com.example.uniride.presentation.home.DefaultDriverHome
import com.example.uniride.presentation.home.DriverHome
import com.example.uniride.presentation.home.HomeRoute
import com.example.uniride.presentation.home.DefaultPassengerHome
import com.example.uniride.presentation.home.PassengerHome

/**
 * Shell of the app: the bottom bar with the mode selector around [UniRideNavHost].
 *
 * Provisional until E0 (Hilt) and RF01: the chosen mode lives here with `rememberSaveable`
 * and [profile] is `null`. Then an `AppViewModel` gives both, from `AppModeRepository` and
 * `ProfileRepository`, and this function stays the same.
 *
 * @param passengerHome start of the passenger mode and [driverHome] C1 of the driver mode. They
 * only exist so instrumented tests, which have no Hilt, can replace them with plain composables;
 * the app uses the defaults.
 */
@Composable
fun UniRideApp(
    modifier: Modifier = Modifier,
    profile: Profile? = null,
    navController: NavHostController = rememberNavController(),
    passengerHome: PassengerHome = DefaultPassengerHome,
    driverHome: DriverHome = DefaultDriverHome,
) {
    var selectedMode by rememberSaveable { mutableStateOf(AppMode.PASAJERO) }
    val modes = availableModes(profile)
    val appMode = effectiveMode(selectedMode, modes)

    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val currentTab = currentDestination?.topLevelDestination(appMode)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val appSnackbar = remember(snackbarHostState, scope) { AppSnackbar(snackbarHostState, scope) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
        CompositionLocalProvider(LocalAppSnackbar provides appSnackbar) {
            UniRideNavHost(
                navController = navController,
                appMode = appMode,
                modifier = Modifier.padding(innerPadding),
                passengerHome = passengerHome,
                driverHome = driverHome,
            )
        }
    }
}

/** Tab of the bottom bar that [this] destination belongs to, or `null` to hide the bar. */
private fun NavDestination.topLevelDestination(mode: AppMode): TopLevelDestination? =
    TopLevelDestination.forMode(mode).firstOrNull { tab ->
        hierarchy.any { it.hasRoute(tab.routeClass) }
    }

/** Standard tab navigation: one copy of each tab, keeping the state of the others. */
internal fun NavController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo<HomeRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
