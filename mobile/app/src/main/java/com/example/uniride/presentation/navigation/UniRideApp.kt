package com.example.uniride.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.uniride.domain.model.AppMode
import com.example.uniride.presentation.auth.AuthGraphRoute
import com.example.uniride.presentation.common.AppSnackbar
import com.example.uniride.presentation.common.LocalAppSnackbar
import com.example.uniride.presentation.home.DefaultDriverHome
import com.example.uniride.presentation.home.DefaultPassengerHome
import com.example.uniride.presentation.home.DriverHome
import com.example.uniride.presentation.home.HomeRoute
import com.example.uniride.presentation.home.PassengerHome

/**
 * Entry of the app: reads the session and the mode from [AppViewModel] and draws [UniRideApp].
 * While the session is being read it only draws the background, so the app does not flash the
 * welcome screen to someone who is logged in.
 */
@Composable
fun UniRideAppRoute(
    modifier: Modifier = Modifier,
    viewModel: AppViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    LaunchedEffect(viewModel, navController) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AppEffect.SessionEnded -> navController.navigateToAuth()
            }
        }
    }

    if (state.session == SessionStatus.LOADING) {
        Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }
    // The start is decided once: logging in or out later navigates, it does not rebuild the graph
    val startsLoggedIn = rememberSaveable { state.session == SessionStatus.LOGGED_IN }

    UniRideApp(
        appMode = state.appMode,
        availableModes = state.availableModes,
        onModeChange = { viewModel.onAction(AppAction.ChangeMode(it)) },
        modifier = modifier,
        startDestination = if (startsLoggedIn) MainGraphRoute else AuthGraphRoute,
        navController = navController,
    )

    // Here, after the NavHost: a push read while the session loaded waits until the graph exists
    LaunchedEffect(state.pendingPush) {
        state.pendingPush?.let { destination ->
            navController.openFromPush(destination)
            viewModel.onAction(AppAction.PushOpened)
        }
    }
}

/**
 * Shell of the app: the bottom bar with the mode selector around [UniRideNavHost]. Choosing a
 * mode goes back to its Home tab.
 *
 * @param passengerHome start of the passenger mode and [driverHome] C1 of the driver mode. They
 * only exist so instrumented tests, which have no Hilt, can replace them with plain composables;
 * the app uses the defaults.
 */
@Composable
fun UniRideApp(
    appMode: AppMode,
    availableModes: Set<AppMode>,
    onModeChange: (AppMode) -> Unit,
    modifier: Modifier = Modifier,
    startDestination: Any = AuthGraphRoute,
    navController: NavHostController = rememberNavController(),
    passengerHome: PassengerHome = DefaultPassengerHome,
    driverHome: DriverHome = DefaultDriverHome,
) {
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
                    availableModes = availableModes,
                    selected = currentTab,
                    onModeChange = { mode ->
                        if (mode != appMode) {
                            onModeChange(mode)
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
                startDestination = startDestination,
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

/**
 * Opens the screen of a push on top of the Home tab, so going back leaves the user on the start
 * of the mode instead of wherever they were before the push. What was above Home is saved, like
 * when changing tabs: a half-filled form comes back when the user returns to its tab.
 */
internal fun NavController.openFromPush(destination: PushDestination) {
    navigate(destination.route) {
        popUpTo<HomeRoute> { saveState = true }
        launchSingleTop = true
    }
}

/**
 * Back to the start of the auth graph, forgetting the main area. Does nothing if the user is
 * already out of it (for example, the session expires while on the login screen).
 */
internal fun NavController.navigateToAuth() {
    val inMainArea = currentDestination?.hierarchy?.any { it.hasRoute(MainGraphRoute::class) } == true
    if (!inMainArea) return
    navigate(AuthGraphRoute) {
        popUpTo<MainGraphRoute> { inclusive = true }
    }
}
