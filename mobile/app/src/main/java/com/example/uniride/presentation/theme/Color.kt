package com.example.uniride.presentation.theme

import androidx.compose.ui.graphics.Color

/*
 * Palette of the high fidelity guide (Figma "UniRide · TPO"): monochrome, with near-black for the
 * main actions and the selected options, white surfaces, light grays for containers and borders,
 * and red only for errors. Screens never use these values directly: they read the roles of
 * MaterialTheme.colorScheme (CONVENCIONES §7.5). Every "on" color keeps a contrast of at least
 * 4.5:1 with its background (RNF01); ThemeContrastTest checks it.
 */

// Light
internal val InkLight = Color(0xFF1C1C1E)
internal val OnInkLight = Color(0xFFFFFFFF)
internal val InkContainerLight = Color(0xFFE8E8EB)
internal val OnInkContainerLight = Color(0xFF1C1C1E)

internal val GraySecondaryLight = Color(0xFF48484D)
internal val OnGraySecondaryLight = Color(0xFFFFFFFF)
internal val GraySecondaryContainerLight = Color(0xFFEDEDF0)
internal val OnGraySecondaryContainerLight = Color(0xFF1C1C1E)

internal val GrayTertiaryLight = Color(0xFF5B5B61)
internal val OnGrayTertiaryLight = Color(0xFFFFFFFF)
internal val GrayTertiaryContainerLight = Color(0xFFE5E5E8)
internal val OnGrayTertiaryContainerLight = Color(0xFF1C1C1E)

internal val ErrorLight = Color(0xFFC62828)
internal val OnErrorLight = Color(0xFFFFFFFF)
internal val ErrorContainerLight = Color(0xFFFDE7E7)
internal val OnErrorContainerLight = Color(0xFF5F0A0A)

internal val BackgroundLight = Color(0xFFFFFFFF)
internal val OnBackgroundLight = Color(0xFF1C1C1E)
internal val SurfaceVariantLight = Color(0xFFF2F2F4)
internal val OnSurfaceVariantLight = Color(0xFF5C5C62)
internal val OutlineLight = Color(0xFF8E8E93)
internal val OutlineVariantLight = Color(0xFFDCDCE0)
internal val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
internal val SurfaceContainerLowLight = Color(0xFFF8F8F9)
internal val SurfaceContainerLight = Color(0xFFF2F2F4)
internal val SurfaceContainerHighLight = Color(0xFFECECEF)
internal val SurfaceContainerHighestLight = Color(0xFFE5E5E8)
internal val InverseSurfaceLight = Color(0xFF2C2C2E)
internal val InverseOnSurfaceLight = Color(0xFFF2F2F4)
internal val InversePrimaryLight = Color(0xFFE5E5E8)

// Dark
internal val InkDark = Color(0xFFF2F2F4)
internal val OnInkDark = Color(0xFF1C1C1E)
internal val InkContainerDark = Color(0xFF3A3A3D)
internal val OnInkContainerDark = Color(0xFFF2F2F4)

internal val GraySecondaryDark = Color(0xFFC7C7CC)
internal val OnGraySecondaryDark = Color(0xFF1C1C1E)
internal val GraySecondaryContainerDark = Color(0xFF333336)
internal val OnGraySecondaryContainerDark = Color(0xFFF2F2F4)

internal val GrayTertiaryDark = Color(0xFFAEAEB2)
internal val OnGrayTertiaryDark = Color(0xFF1C1C1E)
internal val GrayTertiaryContainerDark = Color(0xFF3A3A3D)
internal val OnGrayTertiaryContainerDark = Color(0xFFF2F2F4)

internal val ErrorDark = Color(0xFFFFB4AB)
internal val OnErrorDark = Color(0xFF690005)
internal val ErrorContainerDark = Color(0xFF8C1D18)
internal val OnErrorContainerDark = Color(0xFFFFDAD6)

internal val BackgroundDark = Color(0xFF121214)
internal val OnBackgroundDark = Color(0xFFF2F2F4)
internal val SurfaceVariantDark = Color(0xFF2C2C2E)
internal val OnSurfaceVariantDark = Color(0xFFC7C7CC)
internal val OutlineDark = Color(0xFF8E8E93)
internal val OutlineVariantDark = Color(0xFF3A3A3D)
internal val SurfaceContainerLowestDark = Color(0xFF0C0C0E)
internal val SurfaceContainerLowDark = Color(0xFF1A1A1C)
internal val SurfaceContainerDark = Color(0xFF1F1F21)
internal val SurfaceContainerHighDark = Color(0xFF2A2A2C)
internal val SurfaceContainerHighestDark = Color(0xFF333336)
internal val InverseSurfaceDark = Color(0xFFF2F2F4)
internal val InverseOnSurfaceDark = Color(0xFF1C1C1E)
internal val InversePrimaryDark = Color(0xFF1C1C1E)
