package com.example.uniride.data.local.database

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `una fecha se guarda como texto ISO y se recupera igual`() {
        val date = LocalDate.of(2026, 11, 20)

        val text = converters.fromLocalDate(date)

        assertEquals("2026-11-20", text)
        assertEquals(date, converters.toLocalDate(text))
    }

    @Test
    fun `una hora se guarda con ancho fijo y se recupera igual`() {
        val time = LocalTime.of(22, 15)

        val text = converters.fromLocalTime(time)

        assertEquals("22:15:00", text)
        assertEquals(time, converters.toLocalTime(text))
    }

    @Test
    fun `las horas guardadas como texto se ordenan igual que las horas`() {
        val times = listOf(LocalTime.of(22, 15), LocalTime.of(9, 5), LocalTime.of(13, 0, 30), LocalTime.of(13, 0))

        val byText = times.sortedBy { converters.fromLocalTime(it) }

        assertEquals(times.sorted(), byText)
    }

    @Test
    fun `un instante se guarda como milisegundos y se recupera igual`() {
        val instant = Instant.parse("2026-11-20T21:00:00.250Z")

        val millis = converters.fromInstant(instant)

        assertEquals(instant.toEpochMilli(), millis)
        assertEquals(instant, converters.toInstant(millis))
    }

    @Test
    fun `los valores nulos de fecha, hora e instante se conservan`() {
        assertNull(converters.fromLocalDate(null))
        assertNull(converters.toLocalDate(null))
        assertNull(converters.fromLocalTime(null))
        assertNull(converters.toLocalTime(null))
        assertNull(converters.fromInstant(null))
        assertNull(converters.toInstant(null))
    }

    @Test
    fun `una lista vacia se guarda como arreglo JSON vacio y se recupera vacia`() {
        val json = converters.fromStringList(emptyList())

        assertEquals("[]", json)
        assertEquals(emptyList<String>(), converters.toStringList(json))
    }

    @Test
    fun `una lista de puntos de encuentro se recupera en el mismo orden`() {
        val points = listOf("Salida Lima 717", "Puerta Av. Independencia")

        assertEquals(points, converters.toStringList(converters.fromStringList(points)))
    }

    @Test
    fun `un punto de encuentro con comas y comillas no rompe la lista`() {
        val points = listOf("Lima 717, esquina Av. Belgrano", "Puerta \"principal\", planta baja", "a|b;c")

        val restored = converters.toStringList(converters.fromStringList(points))

        assertEquals(points, restored)
    }
}
