package com.example.uniride.data.push

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.fake.FakeSessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PushTokenSyncManagerTest {

    @Test
    fun `registra el token FCM cuando la sesion pasa a activa`() = runTest(UnconfinedTestDispatcher()) {
        val session = FakeSessionRepository(initialLoggedIn = false)
        var retryCalls = 0
        val manager = PushTokenSyncManager(
            sessionRepository = session,
            tokenProvider = FakeFcmTokenProvider("fcm-token-123"),
            retryScheduler = DeviceTokenSyncScheduler { retryCalls++ },
            appScope = backgroundScope,
        )

        manager.start()
        runCurrent()
        assertTrue(session.registeredDeviceTokens.isEmpty())

        session.setLoggedIn(true)
        runCurrent()

        assertEquals(listOf("fcm-token-123"), session.registeredDeviceTokens)
        assertEquals(0, retryCalls)
    }

    @Test
    fun `omite registro cuando Firebase no devuelve token`() = runTest(UnconfinedTestDispatcher()) {
        val session = FakeSessionRepository(initialLoggedIn = true)
        var retryCalls = 0
        val manager = PushTokenSyncManager(
            sessionRepository = session,
            tokenProvider = FakeFcmTokenProvider(null),
            retryScheduler = DeviceTokenSyncScheduler { retryCalls++ },
            appScope = backgroundScope,
        )

        manager.start()
        runCurrent()

        assertTrue(session.registeredDeviceTokens.isEmpty())
        assertEquals(0, retryCalls)
    }

    @Test
    fun `onNewToken sube el nuevo token si hay sesion activa`() = runTest {
        val session = FakeSessionRepository(initialLoggedIn = true)
        var retryCalls = 0
        val manager = PushTokenSyncManager(
            sessionRepository = session,
            tokenProvider = FakeFcmTokenProvider(null),
            retryScheduler = DeviceTokenSyncScheduler { retryCalls++ },
            appScope = backgroundScope,
        )

        manager.onNewToken("  fcm-rotado-456  ")

        assertEquals(listOf("fcm-rotado-456"), session.registeredDeviceTokens)
        assertEquals(0, retryCalls)
    }

    @Test
    fun `onNewToken ignora la rotacion cuando no hay sesion`() = runTest {
        val session = FakeSessionRepository(initialLoggedIn = false)
        val manager = PushTokenSyncManager(
            sessionRepository = session,
            tokenProvider = FakeFcmTokenProvider(null),
            retryScheduler = DeviceTokenSyncScheduler {},
            appScope = backgroundScope,
        )

        manager.onNewToken("fcm-rotado-sin-sesion")

        assertTrue(session.registeredDeviceTokens.isEmpty())
    }

    @Test
    fun `programa reintento con WorkManager cuando el PUT falla por error transitorio`() =
        runTest(UnconfinedTestDispatcher()) {
            val session = FakeSessionRepository(initialLoggedIn = true).apply {
                registerDeviceTokenResult = AppResult.Failure(DataError.NO_CONNECTION)
            }
            var retryCalls = 0
            val manager = PushTokenSyncManager(
                sessionRepository = session,
                tokenProvider = FakeFcmTokenProvider("fcm-token-1"),
                retryScheduler = DeviceTokenSyncScheduler { retryCalls++ },
                appScope = backgroundScope,
            )

            manager.start()
            runCurrent()

            assertTrue(session.registeredDeviceTokens.isEmpty())
            assertEquals(1, retryCalls)
        }

    @Test
    fun `no programa reintento cuando el servidor responde UNAUTHORIZED`() =
        runTest(UnconfinedTestDispatcher()) {
            val session = FakeSessionRepository(initialLoggedIn = true).apply {
                registerDeviceTokenResult = AppResult.Failure(DataError.UNAUTHORIZED)
            }
            var retryCalls = 0
            val manager = PushTokenSyncManager(
                sessionRepository = session,
                tokenProvider = FakeFcmTokenProvider("fcm-token-1"),
                retryScheduler = DeviceTokenSyncScheduler { retryCalls++ },
                appScope = backgroundScope,
            )

            manager.start()
            runCurrent()

            assertEquals(0, retryCalls)
        }

    @Test
    fun `retryTokenSync devuelve RETRY si persiste la falta de red y DONE al recuperar`() = runTest {
        val session = FakeSessionRepository(initialLoggedIn = true).apply {
            registerDeviceTokenResult = AppResult.Failure(DataError.TIMEOUT)
        }
        val manager = PushTokenSyncManager(
            sessionRepository = session,
            tokenProvider = FakeFcmTokenProvider("fcm-token-retry"),
            retryScheduler = DeviceTokenSyncScheduler {},
            appScope = backgroundScope,
        )

        assertEquals(TokenSyncOutcome.RETRY, manager.retryTokenSync())

        session.registerDeviceTokenResult = AppResult.Success(Unit)
        assertEquals(TokenSyncOutcome.DONE, manager.retryTokenSync())
        assertEquals(listOf("fcm-token-retry"), session.registeredDeviceTokens)
    }

    @Test
    fun `retryTokenSync devuelve DONE sin subir nada si ya no hay sesion`() = runTest {
        val session = FakeSessionRepository(initialLoggedIn = false)
        val manager = PushTokenSyncManager(
            sessionRepository = session,
            tokenProvider = FakeFcmTokenProvider("fcm-token-1"),
            retryScheduler = DeviceTokenSyncScheduler {},
            appScope = backgroundScope,
        )

        assertEquals(TokenSyncOutcome.DONE, manager.retryTokenSync())
        assertTrue(session.registeredDeviceTokens.isEmpty())
    }

    private class FakeFcmTokenProvider(private val token: String?) : FcmTokenProvider {
        override suspend fun currentToken(): String? = token
    }
}
