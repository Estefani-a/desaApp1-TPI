package com.example.uniride.presentation.auth

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.uniride.R
import com.example.uniride.presentation.navigation.MainGraphRoute
import com.example.uniride.presentation.navigation.PlaceholderAction
import com.example.uniride.presentation.navigation.PlaceholderScreen
import kotlinx.serialization.Serializable

/** Subgraph of RF01: everything that happens without a session. */
@Serializable
data object AuthGraphRoute

/** A1 Bienvenida. */
@Serializable
data object WelcomeRoute

/** A2 Login. */
@Serializable
data object LoginRoute

@Serializable
data object RegisterRoute

/** "Revisá tu correo" after registering. */
@Serializable
data object CheckEmailRoute

/** Asks for the email to send the reset link. */
@Serializable
data object ForgotPasswordRoute

/** New password. Opened from the reset link of the email, which brings [resetCode] (docs/API.md §8). */
@Serializable
data class ResetPasswordRoute(val resetCode: String)

/** Opened from the activation link of the email, which brings [activationCode] (docs/API.md §8). */
@Serializable
data class ActivateAccountRoute(val activationCode: String)

fun NavGraphBuilder.authGraph(navController: NavController) {
    navigation<AuthGraphRoute>(startDestination = WelcomeRoute) {
        composable<WelcomeRoute> {
            PlaceholderScreen(
                titleRes = R.string.auth_welcome_title,
                actions = listOf(
                    PlaceholderAction(R.string.placeholder_action_login) { navController.navigate(LoginRoute) },
                    PlaceholderAction(R.string.placeholder_action_register) { navController.navigate(RegisterRoute) },
                ),
            )
        }
        composable<LoginRoute> {
            PlaceholderScreen(
                titleRes = R.string.auth_login_title,
                actions = listOf(
                    PlaceholderAction(R.string.placeholder_action_enter) { navController.navigateToMain() },
                    PlaceholderAction(R.string.placeholder_action_forgot_password) {
                        navController.navigate(ForgotPasswordRoute)
                    },
                ),
            )
        }
        composable<RegisterRoute> {
            PlaceholderScreen(
                titleRes = R.string.auth_register_title,
                actions = listOf(
                    PlaceholderAction(R.string.placeholder_action_create_account) {
                        navController.navigate(CheckEmailRoute)
                    },
                ),
            )
        }
        composable<CheckEmailRoute> {
            PlaceholderScreen(titleRes = R.string.auth_check_email_title)
        }
        composable<ForgotPasswordRoute> {
            PlaceholderScreen(titleRes = R.string.auth_forgot_password_title)
        }
        composable<ResetPasswordRoute> {
            PlaceholderScreen(titleRes = R.string.auth_reset_password_title)
        }
        composable<ActivateAccountRoute> {
            PlaceholderScreen(
                titleRes = R.string.auth_activate_title,
                actions = listOf(
                    PlaceholderAction(R.string.placeholder_action_enter) { navController.navigateToMain() },
                ),
            )
        }
    }
}

/** After login or activation: goes to the main area and removes the auth screens. */
private fun NavController.navigateToMain() {
    navigate(MainGraphRoute) {
        popUpTo<AuthGraphRoute> { inclusive = true }
    }
}
