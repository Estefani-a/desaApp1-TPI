package com.example.uniride.presentation.profile

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** Perfil y cierre de sesión (RF01). */
@Serializable
data object ProfileRoute

/**
 * Profile screen slot (RF01). Exists so instrumented tests, which have no Hilt, can replace
 * [ProfileScreenRoute] with a plain composable; the app always uses [DefaultProfileContent].
 */
typealias ProfileContent = @Composable () -> Unit

val DefaultProfileContent: ProfileContent = {
    ProfileScreenRoute()
}

fun NavGraphBuilder.profileGraph(
    profileContent: ProfileContent = DefaultProfileContent,
) {
    composable<ProfileRoute> {
        profileContent()
    }
}
