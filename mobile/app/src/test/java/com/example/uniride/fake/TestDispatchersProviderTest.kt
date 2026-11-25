package com.example.uniride.fake

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TestDispatchersProviderTest {

    @Test
    fun `los tres dispatchers apuntan al TestDispatcher recibido`() {
        val testDispatcher = StandardTestDispatcher()

        val provider = TestDispatchersProvider(testDispatcher)

        assertSame(testDispatcher, provider.io)
        assertSame(testDispatcher, provider.default)
        assertSame(testDispatcher, provider.main)
    }

    @Test
    fun `el trabajo en io no corre hasta avanzar el tiempo virtual`() = runTest {
        val provider = TestDispatchersProvider(StandardTestDispatcher(testScheduler))
        var done = false

        CoroutineScope(provider.main).launch {
            withContext(provider.io) { done = true }
        }

        assertFalse(done)
        advanceUntilIdle()
        assertTrue(done)
    }
}
