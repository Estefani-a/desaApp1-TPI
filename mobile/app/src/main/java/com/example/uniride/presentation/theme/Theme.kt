package com.example.uniride.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

internal val LightColorScheme = lightColorScheme(
    primary = InkLight,
    onPrimary = OnInkLight,
    primaryContainer = InkContainerLight,
    onPrimaryContainer = OnInkContainerLight,
    secondary = GraySecondaryLight,
    onSecondary = OnGraySecondaryLight,
    secondaryContainer = GraySecondaryContainerLight,
    onSecondaryContainer = OnGraySecondaryContainerLight,
    tertiary = GrayTertiaryLight,
    onTertiary = OnGrayTertiaryLight,
    tertiaryContainer = GrayTertiaryContainerLight,
    onTertiaryContainer = OnGrayTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = BackgroundLight,
    onSurface = OnBackgroundLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    inversePrimary = InversePrimaryLight,
)

internal val DarkColorScheme = darkColorScheme(
    primary = InkDark,
    onPrimary = OnInkDark,
    primaryContainer = InkContainerDark,
    onPrimaryContainer = OnInkContainerDark,
    secondary = GraySecondaryDark,
    onSecondary = OnGraySecondaryDark,
    secondaryContainer = GraySecondaryContainerDark,
    onSecondaryContainer = OnGraySecondaryContainerDark,
    tertiary = GrayTertiaryDark,
    onTertiary = OnGrayTertiaryDark,
    tertiaryContainer = GrayTertiaryContainerDark,
    onTertiaryContainer = OnGrayTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = BackgroundDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = InversePrimaryDark,
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
