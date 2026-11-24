package com.example.uniride.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/** RNF01: every text color of the theme has a contrast of at least 4.5:1 with its background. */
class ThemeContrastTest {

    @Test
    fun `el tema claro cumple el contraste minimo`() = assertContrast(LightColorScheme)

    @Test
    fun `el tema oscuro cumple el contraste minimo`() = assertContrast(DarkColorScheme)

    private fun assertContrast(scheme: ColorScheme) {
        val pairs = mapOf(
            "primary" to (scheme.onPrimary to scheme.primary),
            "primaryContainer" to (scheme.onPrimaryContainer to scheme.primaryContainer),
            "secondary" to (scheme.onSecondary to scheme.secondary),
            "secondaryContainer" to (scheme.onSecondaryContainer to scheme.secondaryContainer),
            "tertiary" to (scheme.onTertiary to scheme.tertiary),
            "tertiaryContainer" to (scheme.onTertiaryContainer to scheme.tertiaryContainer),
            "error" to (scheme.onError to scheme.error),
            "errorContainer" to (scheme.onErrorContainer to scheme.errorContainer),
            "surface" to (scheme.onSurface to scheme.surface),
            "surfaceVariant" to (scheme.onSurfaceVariant to scheme.surfaceVariant),
            "onSurfaceVariant sobre surface" to (scheme.onSurfaceVariant to scheme.surface),
            "surfaceContainer" to (scheme.onSurface to scheme.surfaceContainer),
            "surfaceContainerHighest" to (scheme.onSurfaceVariant to scheme.surfaceContainerHighest),
            "inverseSurface" to (scheme.inverseOnSurface to scheme.inverseSurface),
        )
        pairs.forEach { (name, colors) ->
            val ratio = contrast(colors.first, colors.second)
            assertTrue("$name tiene contraste $ratio", ratio >= 4.5)
        }
    }

    private fun contrast(a: Color, b: Color): Double {
        val lighter = maxOf(a.luminance(), b.luminance()) + 0.05
        val darker = minOf(a.luminance(), b.luminance()) + 0.05
        return lighter.toDouble() / darker
    }
}
