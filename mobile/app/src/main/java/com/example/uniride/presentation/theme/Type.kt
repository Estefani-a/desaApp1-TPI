package com.example.uniride.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.uniride.R

/*
 * Text styles of the high fidelity guide (Figma, "Tipografía · Plus Jakarta Sans"), mapped to the
 * Material roles. Sizes in sp, so they follow the font size chosen in the system (RNF01). Screens
 * use MaterialTheme.typography, never a TextStyle of their own.
 */

/*
 * Plus Jakarta Sans, bundled so it also works offline (license in mobile/third_party). It is a
 * variable font: each weight of the guide is the same file with its own wght axis.
 */
@OptIn(ExperimentalTextApi::class)
private fun jakarta(weight: FontWeight) = Font(
    resId = R.font.plus_jakarta_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

internal val UniRideFontFamily = FontFamily(
    jakarta(FontWeight.Normal),
    jakarta(FontWeight.Medium),
    jakarta(FontWeight.SemiBold),
    jakarta(FontWeight.Bold),
    jakarta(FontWeight.ExtraBold),
)

private val Base = TextStyle(fontFamily = UniRideFontFamily)

val Typography = Typography(
    // Display/L: the headline of the login
    headlineLarge = Base.copy(fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.8).sp),
    // Numeric/L and Brand/Wordmark: prices and the name of the app
    headlineMedium = Base.copy(fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.6).sp),
    // Title/L: titles of the screens
    headlineSmall = Base.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp),
    titleLarge = Base.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp),
    // Title/M: top app bar and sections
    titleMedium = Base.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.2).sp),
    // Title/S: names and places on the cards
    titleSmall = Base.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.1).sp),
    bodyLarge = Base.copy(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    // Body/M and Body/S
    bodyMedium = Base.copy(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = Base.copy(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    // Label/L (buttons), Label/M and Label/S
    labelLarge = Base.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = Base.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp),
    labelSmall = Base.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.2.sp),
)
