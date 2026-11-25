package com.example.uniride.presentation.navigation

import com.example.uniride.domain.model.AppMode
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Test

class AppModesTest {

    private val vehicle = Vehicle("Volkswagen", "Gol Trend", "Gris", "AB123CD")

    private fun profile(mode: PreferredMode, vehicle: Vehicle?) =
        Profile("Lucía Fernández", "Ingeniería", "+5491155550000", mode, vehicle)

    @Test
    fun `quien elige ambos y tiene vehiculo puede cambiar de modo`() {
        assertEquals(AppMode.entries.toSet(), availableModes(profile(PreferredMode.AMBOS, vehicle)))
    }

    @Test
    fun `el pasajero solo es pasajero`() {
        assertEquals(setOf(AppMode.PASAJERO), availableModes(profile(PreferredMode.PASAJERO, vehicle)))
    }

    @Test
    fun `el conductor con vehiculo solo es conductor`() {
        assertEquals(setOf(AppMode.CONDUCTOR), availableModes(profile(PreferredMode.CONDUCTOR, vehicle)))
    }

    @Test
    fun `sin vehiculo no se puede ser conductor`() {
        assertEquals(setOf(AppMode.PASAJERO), availableModes(profile(PreferredMode.CONDUCTOR, null)))
        assertEquals(setOf(AppMode.PASAJERO), availableModes(profile(PreferredMode.AMBOS, null)))
    }

    @Test
    fun `un modo que ya no esta permitido pasa al primero permitido`() {
        assertEquals(AppMode.PASAJERO, effectiveMode(AppMode.CONDUCTOR, setOf(AppMode.PASAJERO)))
        assertEquals(AppMode.CONDUCTOR, effectiveMode(AppMode.CONDUCTOR, AppMode.entries.toSet()))
    }
}
