package com.example.uniride.presentation.navigation

import com.example.uniride.domain.model.AppMode
import org.junit.Assert.assertEquals
import org.junit.Test

class TopLevelDestinationTest {

    @Test
    fun `el pasajero ve inicio, explorar y perfil`() {
        assertEquals(
            listOf(TopLevelDestination.HOME, TopLevelDestination.FEED, TopLevelDestination.PROFILE),
            TopLevelDestination.forMode(AppMode.PASAJERO),
        )
    }

    @Test
    fun `el conductor ve inicio, publicar y perfil`() {
        assertEquals(
            listOf(TopLevelDestination.HOME, TopLevelDestination.PUBLISH, TopLevelDestination.PROFILE),
            TopLevelDestination.forMode(AppMode.CONDUCTOR),
        )
    }
}
