package com.example.uniride.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.uniride.R
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * Formats of dates, times and money in Spanish of Argentina. Pure functions, so they can be
 * tested without a device; the composable wrappers only add the strings of strings.xml.
 */

val SpanishArgentina: Locale = Locale.forLanguageTag("es-AR")

/** Business time zone of the API (docs/API.md §1). */
val BusinessZone: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")

private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm", SpanishArgentina)
private val DayMonthFormatter = DateTimeFormatter.ofPattern("EEE d MMM", SpanishArgentina)

/** `22:15`. */
fun formatTime(time: LocalTime): String = time.format(TimeFormatter)

/** `jue 20 nov`, without the dots some JDKs add to the abbreviations. */
fun formatDayMonth(date: LocalDate): String = date.format(DayMonthFormatter).replace(".", "")

/** `$ 3.500`. Amounts are whole pesos (docs/API.md §1). */
fun formatMoney(pesos: Int): String = "$ " + NumberFormat.getIntegerInstance(SpanishArgentina).format(pesos)

/** `4,8`. */
fun formatRating(rating: Double): String = String.format(SpanishArgentina, "%.1f", rating)

/** `Hoy`, `Mañana` or `jue 20 nov`. */
@Composable
fun dateLabel(date: LocalDate, today: LocalDate = LocalDate.now(BusinessZone)): String = when (date) {
    today -> stringResource(R.string.common_today)
    today.plusDays(1) -> stringResource(R.string.common_tomorrow)
    else -> formatDayMonth(date)
}
