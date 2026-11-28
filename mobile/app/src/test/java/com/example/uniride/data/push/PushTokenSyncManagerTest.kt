package com.example.uniride.data.push

import com.example.uniride.data.remote.datasource.AuthRemoteDataSource
import com.example.uniride.data.remote.dto.ActivateRequest
import com.example.uniride.data.remote.dto.AuthResponseDto
import com.example.uniride.data.remote.dto.ConfirmPasswordResetRequest
import com.example.uniride.data.remote.dto.DeviceTokenRequest
import com.example.uniride.data.remote.dto.LoginRequest
import com.example.uniride.data.remote.dto.MessageResponseDto
import com.example.uniride.data.remote.dto.PasswordResetRequest
import com.example.uniride.data.remote.dto.RegisterRequest
import com.example.uniride.data.remote.dto.ResendActivationRequest
import com.example.uniride.data.remote.dto.UpdateProfileRequest
import com.example.uniride.data.remote.dto.UserDto
import com.example.uniride.domain.repository.LoginOutcome
import com.example.uniride.domain.repository.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PushTokenSyncManagerTest {

    @Test
    fun `registra el token FCM cuando la sesion pasa a activa`() = runTest(UnconfinedTestDispatcher()) {
        val session = MutableSessionRepository(initialLoggedIn = false)
        val remote = RecordingDeviceTokenRemote()
        val manager = PushTokenSyncManager(
            sessionRepository = session,
            remote = remote,
            tokenProvider = FakeFcmTokenProvider("fcm-token-123"),
            appScope = backgroundScope,
        )

        manager.start()
        runCurrent()
        assertTrue(remote.uploadedTokens.isEmpty())

        session.loggedIn.value = true
        runCurrent()

        assertEquals(listOf("fcm-token-123"), remote.uploadedTokens)
    }

    @Test
    fun `omite registro cuando Firebase no devuelve token`() = runTest(UnconfinedTestDispatcher()) {
        val session = MutableSessionRepository(initialLoggedIn = true)
        val remote = RecordingDeviceTokenRemote()
        val manager = PushTokenSyncManager(
            sessionRepository = session,
            remote = remote,
            tokenProvider = FakeFcmTokenProvider(null),
            appScope = backgroundScope,
        )

        manager.start()
        runCurrent()

        assertTrue(remote.uploadedTokens.isEmpty())
    }

    @Test
    fun `onNewToken sube el nuevo token si hay sesion activa`() = runTest {
        val session = MutableSessionRepository(initialLoggedIn = true)
        val remote = RecordingDeviceTokenRemote()
        val manager = PushTokenSyncManager(
            sessionRepository = session,
            remote = remote,
            tokenProvider = FakeFcmTokenProvider(null),
            appScope = backgroundScope,
        )

        manager.onNewToken("fcm-rotado-456")

        assertEquals(listOf("fcm-rotado-456"), remote.uploadedTokens)
    }

    @Test
    fun `onNewToken ignora la rotacion cuando no hay sesion`() = runTest {
        val session = MutableSessionRepository(initialLoggedIn = false)
        val remote = RecordingDeviceTokenRemote()
        val manager = PushTokenSyncManager(
            sessionRepository = session,
            remote = remote,
            tokenProvider = FakeFcmTokenProvider(null),
            appScope = backgroundScope,
        )

        manager.onNewToken("fcm-rotado-sin-sesion")

        assertTrue(remote.uploadedTokens.isEmpty())
    }

    @Test
    fun `un error de red al registrar token no rompe el colector ni futuros reintentos`() = runTest(UnconfinedTestDispatcher()) {
        val session = MutableSessionRepository(initialLoggedIn = true)
        val remote = RecordingDeviceTokenRemote().apply { failNext = true }
        val manager = PushTokenSyncManager(
            sessionRepository = session,
            remote = remote,
            tokenProvider = FakeFcmTokenProvider("fcm-token-1"),
            appScope = backgroundScope,
        )

        manager.start()
        runCurrent()
        assertTrue(remote.uploadedTokens.isEmpty())

        session.loggedIn.value = false
        runCurrent()
        session.loggedIn.value = true
        runCurrent()

        assertEquals(listOf("fcm-token-1"), remote.uploadedTokens)
    }

    private class FakeFcmTokenProvider(private val token: String?) : FcmTokenProvider {
        override suspend fun currentToken(): String? = token
    }

    private class MutableSessionRepository(initialLoggedIn: Boolean) : SessionRepository {
        val loggedIn = MutableStateFlow(initialLoggedIn)
        override fun observeIsLoggedIn(): Flow<Boolean> = loggedIn
        override suspend fun login(email: String, password: String): LoginOutcome =
            error("No se usa en este test")
        override suspend fun logout() = Unit
    }

    private class RecordingDeviceTokenRemote : AuthRemoteDataSource {
        val uploadedTokens = mutableListOf<String>()
        var failNext = false

        override suspend fun updateDeviceToken(request: DeviceTokenRequest) {
            if (failNext) {
                failNext = false
                throw IOException("Sin red")
            }
            uploadedTokens += request.fcmToken
        }

        override suspend fun register(request: RegisterRequest): MessageResponseDto = error("No se usa")
        override suspend fun activate(request: ActivateRequest): AuthResponseDto = error("No se usa")
        override suspend fun resendActivation(request: ResendActivationRequest) = error("No se usa")
        override suspend fun login(request: LoginRequest): AuthResponseDto = error("No se usa")
        override suspend fun requestPasswordReset(request: PasswordResetRequest): MessageResponseDto = error("No se usa")
        override suspend fun confirmPasswordReset(request: ConfirmPasswordResetRequest) = error("No se usa")
        override suspend fun getProfile(): UserDto = error("No se usa")
        override suspend fun updateProfile(request: UpdateProfileRequest): UserDto = error("No se usa")
        override suspend fun deleteDeviceToken() = error("No se usa")
    }
}
