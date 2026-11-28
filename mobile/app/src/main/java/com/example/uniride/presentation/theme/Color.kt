package com.example.uniride.presentation.theme

import androidx.compose.ui.graphics.Color

/*
 * Palette of the high fidelity guide (Figma "UniRide · TPO", page "Guía visual (alta fidelidad)",
 * variables "UniRide · Tokens"). Each light value keeps the name of its token. Screens never use
 * these values directly: they read the roles of MaterialTheme.colorScheme (CONVENCIONES §7.5),
 * mapped in Theme.kt. Every "on" color keeps a contrast of at least 4.5:1 with its background
 * (RNF01); ThemeContrastTest checks it.
 */

// Tokens of the guide
internal val Primary = Color(0xFF5B3DF5)
internal val OnPrimary = Color(0xFFFFFFFF)
internal val PrimaryContainer = Color(0xFFEEEAFF)
internal val Accent = Color(0xFFC8F53A)
internal val OnAccent = Color(0xFF0E1020)
internal val Ink = Color(0xFF0E1020)
internal val InkMuted = Color(0xFF5E6275)
internal val InkSubtle = Color(0xFF9A9DAE)
internal val Outline = Color(0xFFE4E5EE)
internal val Surface = Color(0xFFF6F6FA)
internal val Card = Color(0xFFFFFFFF)
internal val Success = Color(0xFF15803D)
internal val SuccessContainer = Color(0xFFE3F8EA)
internal val Warning = Color(0xFFB45309)
internal val WarningContainer = Color(0xFFFEF3C7)
internal val Error = Color(0xFFDC2626)
internal val ErrorContainer = Color(0xFFFDECEC)

/*
 * Not in the guide. The guide uses [Warning] and [Error] as text over their containers, but they
 * stay under 4.5:1 there, so the text uses a darker shade of the same hue. The container steps
 * between [Card] and [Outline] fill the elevations Material asks for.
 */
internal val OnWarningContainer = Color(0xFF92400E)
internal val OnErrorContainer = Color(0xFFB91C1C)
internal val SurfaceContainerLight = Color(0xFFF0F0F6)
internal val SurfaceContainerHighLight = Color(0xFFEAEBF2)

/*
 * Dark theme. The guide only defines the light one: these are the same hues over the ink, with
 * the violet and the red lightened so they keep the contrast.
 */
internal val PrimaryDark = Color(0xFFA898FF)
internal val OnPrimaryDark = Color(0xFF0E1020)
internal val PrimaryContainerDark = Color(0xFF2E2470)
internal val OnPrimaryContainerDark = Color(0xFFEEEAFF)
internal val InkDark = Color(0xFFF6F6FA)
internal val InkMutedDark = Color(0xFFB5B8C9)
internal val OutlineDark = Color(0xFF6E7186)
internal val OutlineVariantDark = Color(0xFF2A2D40)
internal val BackgroundDark = Color(0xFF0E1020)
internal val SurfaceContainerLowestDark = Color(0xFF090A15)
internal val SurfaceContainerLowDark = Color(0xFF15172A)
internal val SurfaceContainerDark = Color(0xFF1A1C30)
internal val SurfaceContainerHighDark = Color(0xFF222439)
internal val SurfaceContainerHighestDark = Color(0xFF2A2D42)
internal val WarningContainerDark = Color(0xFF4A2A06)
internal val OnWarningContainerDark = Color(0xFFFEF3C7)
internal val ErrorDark = Color(0xFFF87171)
internal val OnErrorDark = Color(0xFF0E1020)
internal val ErrorContainerDark = Color(0xFF5F1414)
internal val OnErrorContainerDark = Color(0xFFFDECEC)
