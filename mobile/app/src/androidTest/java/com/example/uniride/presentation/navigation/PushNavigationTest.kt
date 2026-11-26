package com.example.uniride.presentation.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.uniride.domain.model.TripId
import com.example.uniride.presentation.feed.FeedRoute
import com.example.uniride.presentation.home.HomeRoute
import com.example.uniride.presentation.reservations.DriverPanelRoute
import com.example.uniride.presentation.reservations.MyTripRoute
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.reflect.KClass

/**
 * Where a tapped push leaves the user. The graph has the real routes with empty screens: the
 * real panel and trip screens have Hilt ViewModels and this test has no Hilt.
 */
@RunWith(AndroidJUnit4::class)
class PushNavigationTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navController: TestNavHostController

    private fun start() {
        rule.setContent {
            val context = LocalContext.current
            navController = remember {
                TestNavHostController(context).apply { navigatorProvider.addNavigator(ComposeNavigator()) }
            }
            NavHost(navController, startDestination = MainGraphRoute) {
                navigation<MainGraphRoute>(startDestination = HomeRoute) {
                    composable<HomeRoute> {}
                    composable<FeedRoute> {}
                    composable<DriverPanelRoute> {}
                    composable<MyTripRoute> {}
                }
            }
        }
    }

    private fun backStack(): List<KClass<*>> =
        navController.currentBackStack.value.mapNotNull { entry ->
            listOf(HomeRoute::class, FeedRoute::class, DriverPanelRoute::class, MyTripRoute::class)
                .firstOrNull { entry.destination.hasRoute(it) }
        }

    private fun openFromPush(destination: PushDestination) {
        rule.runOnIdle { navController.openFromPush(destination) }
        rule.waitForIdle()
    }

    @Test
    fun unaSolicitudNuevaAbreElPanelArribaDelInicio() {
        start()
        rule.runOnIdle { navController.navigate(FeedRoute) }

        openFromPush(PushDestination.DriverPanel(TRIP_ID))

        assertEquals(listOf(HomeRoute::class, DriverPanelRoute::class), backStack())
    }

    @Test
    fun unaRespuestaAbreMiViajeConElIdDelViaje() {
        start()

        openFromPush(PushDestination.MyTrip(TRIP_ID))

        assertEquals(TRIP_ID.value, navController.currentBackStackEntry?.arguments?.getString("tripId"))
    }

    @Test
    fun dosPushDelMismoViajeNoApilanDosPaneles() {
        start()

        openFromPush(PushDestination.DriverPanel(TRIP_ID))
        openFromPush(PushDestination.DriverPanel(TRIP_ID))

        assertEquals(listOf(HomeRoute::class, DriverPanelRoute::class), backStack())
    }

    private companion object {
        val TRIP_ID = TripId("0b7c4f0e-5d1a-4f5a-8a52-7c9e5e0d2b31")
    }
}
