package com.example.uniride.presentation.auth

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navDeepLink
import com.example.uniride.presentation.auth.activate.ActivateAccountRoute as ActivateAccountScreenRoute
import com.example.uniride.presentation.auth.checkemail.CheckEmailRoute as CheckEmailScreenRoute
import com.example.uniride.presentation.auth.login.LoginRoute as LoginScreenRoute
import com.example.uniride.presentation.auth.register.RegisterRoute as RegisterScreenRoute
import com.example.uniride.presentation.auth.resetpassword.ForgotPasswordRoute as ForgotPasswordScreenRoute
import com.example.uniride.presentation.auth.resetpassword.ResetPasswordRoute as ResetPasswordScreenRoute
import com.example.uniride.presentation.auth.welcome.WelcomeScreen
import com.example.uniride.presentation.navigation.MainGraphRoute
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
data class CheckEmailRoute(val email: String = "")

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
            WelcomeScreen(
                onLoginClick = { navController.navigate(LoginRoute) },
                onRegisterClick = { navController.navigate(RegisterRoute) },
            )
        }
        composable<LoginRoute> {
            LoginScreenRoute(
                onBack = { navController.popBackStack() },
                onForgotPasswordClick = { navController.navigate(ForgotPasswordRoute) },
                onRegisterClick = { navController.navigate(RegisterRoute) },
                onLoginSuccess = { navController.navigateToMain() },
            )
        }
        composable<RegisterRoute> {
            RegisterScreenRoute(
                onBack = { navController.popBackStack() },
                onLoginClick = {
                    navController.navigate(LoginRoute) {
                        popUpTo<RegisterRoute> { inclusive = true }
                    }
                },
                onRegistered = { email ->
                    navController.navigate(CheckEmailRoute(email = email)) {
                        popUpTo<RegisterRoute> { inclusive = true }
                    }
                },
            )
        }
        composable<CheckEmailRoute> {
            CheckEmailScreenRoute(
                onBack = { navController.popBackStack() },
                onGoToLogin = {
                    navController.navigate(LoginRoute) {
                        popUpTo<WelcomeRoute>()
                    }
                },
            )
        }
        composable<ForgotPasswordRoute> {
            ForgotPasswordScreenRoute(
                onBack = { navController.popBackStack() },
                onBackToLogin = { navController.popBackStack() },
            )
        }
        composable<ResetPasswordRoute>(
            deepLinks = listOf(
                navDeepLink { uriPattern = "uniride://reset-password?token={resetCode}" },
            ),
        ) {
            ResetPasswordScreenRoute(
                onGoToLogin = {
                    navController.navigate(LoginRoute) {
                        popUpTo<AuthGraphRoute> { inclusive = false }
                    }
                },
            )
        }
        composable<ActivateAccountRoute>(
            deepLinks = listOf(
                navDeepLink { uriPattern = "uniride://activate?token={activationCode}" },
            ),
        ) {
            ActivateAccountScreenRoute(
                onActivated = { navController.navigateToMain() },
                onGoToLogin = {
                    navController.navigate(LoginRoute) {
                        popUpTo<AuthGraphRoute> { inclusive = false }
                    }
                },
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
