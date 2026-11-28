package com.example.uniride.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/*
 * Roles of the guide: primary is the violet of the main actions, the active tab and the links;
 * secondary is the ink of the selected options (segmented buttons, the dark trip card);
 * tertiary is the lime accent of the "HOY" badge and the route dots. The bottom bar indicator
 * and the info chips use the light violet (secondaryContainer), and the warnings and the
 * offline banner use the amber of the guide (tertiaryContainer).
 */
internal val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = Primary,
    secondary = Ink,
    onSecondary = Card,
    secondaryContainer = PrimaryContainer,
    onSecondaryContainer = Primary,
    tertiary = Accent,
    onTertiary = OnAccent,
    tertiaryContainer = WarningContainer,
    onTertiaryContainer = OnWarningContainer,
    error = Error,
    onError = Card,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Surface,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = Outline,
    onSurfaceVariant = InkMuted,
    outline = InkSubtle,
    outlineVariant = Outline,
    surfaceContainerLowest = Card,
    surfaceContainerLow = Card,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = Outline,
    inverseSurface = Ink,
    inverseOnSurface = Card,
    inversePrimary = Accent,
)

internal val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = InkDark,
    onSecondary = BackgroundDark,
    secondaryContainer = PrimaryContainerDark,
    onSecondaryContainer = OnPrimaryContainerDark,
    tertiary = Accent,
    onTertiary = OnAccent,
    tertiaryContainer = WarningContainerDark,
    onTertiaryContainer = OnWarningContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = InkDark,
    surface = BackgroundDark,
    onSurface = InkDark,
    surfaceVariant = SurfaceContainerHighestDark,
    onSurfaceVariant = InkMutedDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    inverseSurface = InkDark,
    inverseOnSurface = BackgroundDark,
    inversePrimary = Primary,
)

/**
 * Theme of the app (N1). It does not use the dynamic colors of Android 12+: the screens must look
 * like the Figma guide on every phone, and the contrast of this palette is checked (RNF01).
 */
@Composable
fun UniRideTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
