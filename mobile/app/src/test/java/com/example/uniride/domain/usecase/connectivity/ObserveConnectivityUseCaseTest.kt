package com.example.uniride.domain.usecase.connectivity

import app.cash.turbine.test
import com.example.uniride.fake.FakeNetworkMonitor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveConnectivityUseCaseTest {

    @Test
    fun `emite true inicialmente cuando hay conexion`() = runTest {
        val fakeMonitor = FakeNetworkMonitor(initialOnline = true)
        val useCase = ObserveConnectivityUseCase(fakeMonitor)

        useCase().test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emite false inicialmente cuando no hay conexion`() = runTest {
        val fakeMonitor = FakeNetworkMonitor(initialOnline = false)
        val useCase = ObserveConnectivityUseCase(fakeMonitor)

        useCase().test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emite los cambios de conectividad al perder y recuperar red`() = runTest {
        val fakeMonitor = FakeNetworkMonitor(initialOnline = true)
        val useCase = ObserveConnectivityUseCase(fakeMonitor)

        useCase().test {
            assertTrue(awaitItem())
            fakeMonitor.setOnline(false)
            assertFalse(awaitItem())
            fakeMonitor.setOnline(true)
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
