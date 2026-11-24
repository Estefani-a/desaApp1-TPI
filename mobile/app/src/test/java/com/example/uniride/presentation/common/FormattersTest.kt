package com.example.uniride.presentation.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class FormattersTest {

    @Test
    fun `la hora se muestra con dos digitos y formato 24 horas`() {
        assertEquals("08:05", formatTime(LocalTime.of(8, 5)))
        assertEquals("22:15", formatTime(LocalTime.of(22, 15)))
    }

    @Test
    fun `el aporte se muestra en pesos con separador de miles`() {
        assertEquals("$ 3.500", formatMoney(3500))
        assertEquals("$ 0", formatMoney(0))
        assertEquals("$ 12.000", formatMoney(12000))
    }

    @Test
    fun `la calificacion usa coma decimal`() {
        assertEquals("4,8", formatRating(4.8))
        assertEquals("5,0", formatRating(5.0))
    }

    @Test
    fun `la fecha corta tiene dia de la semana, numero y mes sin puntos`() {
        val label = formatDayMonth(LocalDate.of(2026, 11, 20))
        assertEquals("vie 20 nov", label)
    }
}
