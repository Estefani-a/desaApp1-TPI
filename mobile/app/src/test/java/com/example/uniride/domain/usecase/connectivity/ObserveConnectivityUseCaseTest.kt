package com.example.uniride.domain.usecase.connectivity

import com.example.uniride.fake.FakeNetworkMonitor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveConnectivityUseCaseTest {

    @Test
    fun `emite true inicialmente cuando hay conexion`() = runBlocking {
        val fakeMonitor = FakeNetworkMonitor(initialOnline = true)
        val useCase = ObserveConnectivityUseCase(fakeMonitor)

        assertTrue(useCase().first())
    }

    @Test
    fun `emite false inicialmente cuando no hay conexion`() = runBlocking {
        val fakeMonitor = FakeNetworkMonitor(initialOnline = false)
        val useCase = ObserveConnectivityUseCase(fakeMonitor)

        assertFalse(useCase().first())
    }

    @Test
    fun `emite los cambios de conectividad al perder y recuperar red`() = runBlocking {
        val fakeMonitor = FakeNetworkMonitor(initialOnline = true)
        val useCase = ObserveConnectivityUseCase(fakeMonitor)
        val emissions = mutableListOf<Boolean>()

        val job = launch {
            useCase().take(3).toList(emissions)
        }

        // Wait until the collector starts receiving the initial value, then toggle states.
        kotlinx.coroutines.yield()
        fakeMonitor.setOnline(false)
        kotlinx.coroutines.yield()
        fakeMonitor.setOnline(true)
        job.join()

        assertEquals(listOf(true, false, true), emissions)
    }
}
