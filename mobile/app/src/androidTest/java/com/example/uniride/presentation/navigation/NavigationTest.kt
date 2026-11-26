package com.example.uniride.presentation.navigation

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.uniride.R
import com.example.uniride.domain.model.AppMode
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.model.Profile
import com.example.uniride.presentation.auth.AuthGraphRoute
import com.example.uniride.presentation.auth.WelcomeRoute
import com.example.uniride.presentation.home.HomeRoute
import com.example.uniride.presentation.theme.UniRideTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.reflect.KClass

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navController: TestNavHostController

    private fun start(profile: Profile? = null) {
        rule.setContent {
            val context = LocalContext.current
            // remember: changing the mode recomposes this block and must keep the same controller
            navController = remember {
                TestNavHostController(context).apply { navigatorProvider.addNavigator(ComposeNavigator()) }
            }
            // The mode lives here instead of in AppViewModel, which needs Hilt
            var chosenMode by remember { mutableStateOf(AppMode.PASAJERO) }
            val modes = availableModes(profile)
            UniRideTheme {
                UniRideApp(
                    appMode = effectiveMode(chosenMode, modes),
                    availableModes = modes,
                    onModeChange = { chosenMode = it },
                    navController = navController,
                    // Screens with Hilt ViewModels are swapped because this test has no Hilt
                    passengerHome = { _, _ -> },
                    driverHome = { _, _ -> },
                    loginContent = { _, _, _, onLoginSuccess ->
                        Button(onClick = onLoginSuccess) {
                            Text(stringResource(R.string.placeholder_action_enter))
                        }
                    },
                    profileContent = {
                        Button(onClick = { navController.navigateToAuth() }) {
                            Text(stringResource(R.string.profile_logout_action))
                        }
                    },
                )
            }
        }
    }

    private fun click(@StringRes text: Int) {
        rule.onNodeWithText(rule.activity.getString(text)).performClick()
        rule.waitForIdle()
    }

    private fun isAt(route: KClass<*>) = navController.currentDestination?.hasRoute(route) == true

    private fun inBackStack(route: KClass<*>) =
        navController.currentBackStack.value.any { it.destination.hasRoute(route) }

    private fun login() {
        click(R.string.auth_welcome_login_action)
        click(R.string.placeholder_action_enter)
    }

    @Test
    fun entrarLlevaAlInicioYSacaElLoginDelBackStack() {
        start()

        login()

        assertTrue(isAt(HomeRoute::class))
        assertFalse(inBackStack(AuthGraphRoute::class))
    }

    @Test
    fun cambiarDeModoVuelveAlInicioConLasPestanasDelModo() {
        start()
        login()
        // Any tab other than Home works to leave it. Not the feed: it is a real screen with a
        // Hilt ViewModel, and this test has no Hilt
        click(R.string.nav_profile)

        click(R.string.nav_mode_driver)

        assertTrue(isAt(HomeRoute::class))
        rule.onNodeWithText(rule.activity.getString(R.string.nav_publish)).assertExists()
        rule.onNodeWithText(rule.activity.getString(R.string.nav_feed)).assertDoesNotExist()
    }

    @Test
    fun cerrarSesionVuelveALaBienvenidaYLimpiaElAreaPrincipal() {
        start()
        login()
        click(R.string.nav_profile)

        click(R.string.profile_logout_action)

        assertTrue(isAt(WelcomeRoute::class))
        assertFalse(inBackStack(MainGraphRoute::class))
    }

    @Test
    fun unPasajeroSinVehiculoNoVeElSelectorDeModo() {
        start(Profile("Martín Gómez", "Administración", "+5491155550001", PreferredMode.PASAJERO, vehicle = null))
        login()

        rule.onNodeWithText(rule.activity.getString(R.string.nav_mode_driver)).assertDoesNotExist()
        rule.onNodeWithText(rule.activity.getString(R.string.nav_publish)).assertDoesNotExist()
    }
}
