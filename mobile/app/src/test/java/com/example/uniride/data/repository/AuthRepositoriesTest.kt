package com.example.uniride.data.repository

import app.cash.turbine.test
import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.data.push.PushNotifier
import com.example.uniride.data.push.PushPayload
import com.example.uniride.data.remote.datasource.AuthRemoteDataSource
import com.example.uniride.data.remote.datasource.FakeAuthRemoteDataSource
import com.example.uniride.data.remote.datasource.FakeBackendStore
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.Vehicle
import com.example.uniride.domain.repository.ActivateOutcome
import com.example.uniride.domain.repository.ConfirmPasswordResetOutcome
import com.example.uniride.domain.repository.LoginOutcome
import com.example.uniride.domain.repository.RegisterOutcome
import com.example.uniride.fake.FakeSessionTokenStore
import com.example.uniride.fake.FakeUserDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoriesTest {

    private class RecordingPushNotifier : PushNotifier {
        var cancelAllCalls = 0
        override fun ensureChannel() = Unit
        override fun show(payload: PushPayload) = Unit
        override fun cancelAll() {
            cancelAllCalls++
        }
    }

    private fun sampleProfile(withVehicle: Boolean = false) = Profile(
        fullName = "Juan Pérez",
        career = "Ingeniería en Informática",
        phone = "+5491144445555",
        preferredMode = if (withVehicle) PreferredMode.AMBOS else PreferredMode.PASAJERO,
        vehicle = if (withVehicle) Vehicle("Ford", "Fiesta", "Rojo", "AC321BD") else null,
    )

    @Test
    fun `registro no inicia sesion y activacion limpia sesion previa antes de guardar usuario y token`() = runTest {
        val tokenStore = FakeSessionTokenStore()
        val store = FakeBackendStore(tokenProvider = tokenStore::currentToken)
        val remote = FakeAuthRemoteDataSource(store)
        val userDao = FakeUserDao()
        var clearCalls = 0
        val cleaner = LocalSessionCleaner {
            clearCalls++
            userDao.clear()
        }
        val accountRepo = AccountRepositoryImpl(remote, tokenStore, userDao, cleaner)

        val regOutcome = accountRepo.register(
            email = "jperez@uade.edu.ar",
            password = "Password1",
            profile = sampleProfile(),
        )
        assertEquals(RegisterOutcome.Registered, regOutcome)
        assertNull(tokenStore.currentToken())
        assertNull(userDao.getCurrentUser())

        val resendResult = accountRepo.resendActivation("jperez@uade.edu.ar")
        assertTrue(resendResult is AppResult.Success)

        val activateOutcome = accountRepo.activate("fake-activate-jperez@uade.edu.ar")
        assertTrue(activateOutcome is ActivateOutcome.Activated)
        val user = (activateOutcome as ActivateOutcome.Activated).user
        assertEquals("jperez@uade.edu.ar", user.email)
        assertEquals(1, clearCalls)
        assertNotNull(tokenStore.currentToken())
        assertEquals("jperez@uade.edu.ar", userDao.getCurrentUser()?.email)
    }

    @Test
    fun `registro duplicado devuelve EmailAlreadyRegistered`() = runTest {
        val tokenStore = FakeSessionTokenStore()
        val store = FakeBackendStore(tokenProvider = tokenStore::currentToken)
        val remote = FakeAuthRemoteDataSource(store)
        val userDao = FakeUserDao()
        val cleaner = LocalSessionCleaner { userDao.clear() }
        val accountRepo = AccountRepositoryImpl(remote, tokenStore, userDao, cleaner)

        val duplicate = accountRepo.register(
            email = "lfernandez@uade.edu.ar",
            password = "Password1",
            profile = sampleProfile(),
        )

        assertEquals(RegisterOutcome.EmailAlreadyRegistered, duplicate)
    }

    @Test
    fun `activacion con codigo inexistente devuelve InvalidOrExpiredCode`() = runTest {
        val tokenStore = FakeSessionTokenStore()
        val store = FakeBackendStore(tokenProvider = tokenStore::currentToken)
        val remote = FakeAuthRemoteDataSource(store)
        val userDao = FakeUserDao()
        val cleaner = LocalSessionCleaner { userDao.clear() }
        val accountRepo = AccountRepositoryImpl(remote, tokenStore, userDao, cleaner)

        val invalidCode = accountRepo.activate("token-inexistente")

        assertEquals(ActivateOutcome.InvalidOrExpiredCode, invalidCode)
    }

    @Test
    fun `login exitoso guarda token y usuario en Room y emite sesion activa`() = runTest {
        val tokenStore = FakeSessionTokenStore()
        val store = FakeBackendStore(tokenProvider = tokenStore::currentToken)
        val remote = FakeAuthRemoteDataSource(store)
        val userDao = FakeUserDao()
        val cleaner = LocalSessionCleaner { userDao.clear() }
        val sessionRepo = SessionRepositoryImpl(remote, tokenStore, userDao, cleaner, RecordingPushNotifier(), backgroundScope)
        val profileRepo = ProfileRepositoryImpl(remote, userDao, tokenStore)

        sessionRepo.observeIsLoggedIn().test {
            assertFalse(awaitItem())

            val outcome = sessionRepo.login("lfernandez@uade.edu.ar", FakeBackendStore.DEMO_PASSWORD)
            assertTrue(outcome is LoginOutcome.Success)
            assertTrue(awaitItem())
            assertEquals("lfernandez@uade.edu.ar", profileRepo.observeCurrentUser().first()?.email)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `login con clave incorrecta devuelve InvalidCredentials sin persistir usuario ni token`() = runTest {
        val tokenStore = FakeSessionTokenStore()
        val store = FakeBackendStore(tokenProvider = tokenStore::currentToken)
        val remote = FakeAuthRemoteDataSource(store)
        val userDao = FakeUserDao()
        val cleaner = LocalSessionCleaner { userDao.clear() }
        val sessionRepo = SessionRepositoryImpl(remote, tokenStore, userDao, cleaner, RecordingPushNotifier(), backgroundScope)

        val outcome = sessionRepo.login("lfernandez@uade.edu.ar", "ClaveMala99")

        assertEquals(LoginOutcome.InvalidCredentials, outcome)
        assertNull(tokenStore.currentToken())
        assertNull(userDao.getCurrentUser())
    }

    @Test
    fun `login de cuenta pendiente devuelve AccountNotActivated`() = runTest {
        val tokenStore = FakeSessionTokenStore()
        val store = FakeBackendStore(tokenProvider = tokenStore::currentToken)
        val remote = FakeAuthRemoteDataSource(store)
        val userDao = FakeUserDao()
        val cleaner = LocalSessionCleaner { userDao.clear() }
        val accountRepo = AccountRepositoryImpl(remote, tokenStore, userDao, cleaner)
        val sessionRepo = SessionRepositoryImpl(remote, tokenStore, userDao, cleaner, RecordingPushNotifier(), backgroundScope)

        accountRepo.register("pendiente@uade.edu.ar", "Password1", sampleProfile())
        val outcome = sessionRepo.login("pendiente@uade.edu.ar", "Password1")

        assertEquals(LoginOutcome.AccountNotActivated, outcome)
        assertNull(tokenStore.currentToken())
    }

    @Test
    fun `logout limpia token y datos locales incluso cuando falla la red`() = runTest {
        val tokenStore = FakeSessionTokenStore()
        val store = FakeBackendStore(tokenProvider = tokenStore::currentToken)
        val fakeRemote = FakeAuthRemoteDataSource(store)
        var failDeleteDeviceToken = false
        val remote = object : AuthRemoteDataSource by fakeRemote {
            override suspend fun deleteDeviceToken() {
                if (failDeleteDeviceToken) throw IOException("Sin conexión")
                fakeRemote.deleteDeviceToken()
            }
        }
        val userDao = FakeUserDao()
        var clearCalls = 0
        val cleaner = LocalSessionCleaner {
            clearCalls++
            userDao.clear()
        }
        val notifier = RecordingPushNotifier()
        val sessionRepo = SessionRepositoryImpl(remote, tokenStore, userDao, cleaner, notifier, backgroundScope)

        sessionRepo.login("mgomez@uade.edu.ar", FakeBackendStore.DEMO_PASSWORD)
        assertNotNull(tokenStore.currentToken())
        assertNotNull(userDao.getCurrentUser())
        val clearsAfterLogin = clearCalls

        failDeleteDeviceToken = true
        sessionRepo.logout()

        assertNull(tokenStore.currentToken())
        assertNull(userDao.getCurrentUser())
        assertEquals(clearsAfterLogin + 1, clearCalls)
        assertEquals(1, notifier.cancelAllCalls)
        assertFalse(sessionRepo.observeIsLoggedIn().first())
    }

    @Test
    fun `evento global de 401 dispara limpieza de Room, cancela notificaciones y cierra la sesion automaticamente`() = runTest {
        val tokenStore = FakeSessionTokenStore()
        val store = FakeBackendStore(tokenProvider = tokenStore::currentToken)
        val remote = FakeAuthRemoteDataSource(store)
        val userDao = FakeUserDao()
        var clearCalls = 0
        val cleaner = LocalSessionCleaner {
            clearCalls++
            userDao.clear()
        }
        val notifier = RecordingPushNotifier()
        val sessionRepo = SessionRepositoryImpl(remote, tokenStore, userDao, cleaner, notifier, backgroundScope)
        runCurrent()

        sessionRepo.login("lfernandez@uade.edu.ar", FakeBackendStore.DEMO_PASSWORD)
        val clearsAfterLogin = clearCalls
        assertTrue(sessionRepo.observeIsLoggedIn().first())

        tokenStore.notifyUnauthorized()
        runCurrent()

        assertEquals(clearsAfterLogin + 1, clearCalls)
        assertEquals(1, notifier.cancelAllCalls)
        assertNull(userDao.getCurrentUser())
        assertFalse(sessionRepo.observeIsLoggedIn().first())
    }

    @Test
    fun `recuperacion de contrasena permite iniciar sesion con la nueva clave`() = runTest {
        val tokenStore = FakeSessionTokenStore()
        val store = FakeBackendStore(tokenProvider = tokenStore::currentToken)
        val remote = FakeAuthRemoteDataSource(store)
        val userDao = FakeUserDao()
        val cleaner = LocalSessionCleaner { userDao.clear() }
        val recoveryRepo = PasswordRecoveryRepositoryImpl(remote)
        val sessionRepo = SessionRepositoryImpl(remote, tokenStore, userDao, cleaner, RecordingPushNotifier(), backgroundScope)

        assertTrue(recoveryRepo.requestReset("mgomez@uade.edu.ar") is AppResult.Success)
        assertEquals(
            ConfirmPasswordResetOutcome.Confirmed,
            recoveryRepo.confirmReset("fake-reset-mgomez@uade.edu.ar", "NuevaClave99"),
        )

        assertEquals(
            LoginOutcome.InvalidCredentials,
            sessionRepo.login("mgomez@uade.edu.ar", FakeBackendStore.DEMO_PASSWORD),
        )
        assertTrue(sessionRepo.login("mgomez@uade.edu.ar", "NuevaClave99") is LoginOutcome.Success)
    }

    @Test
    fun `confirmar recuperacion distingue token vencido de contrasena debil`() = runTest {
        val tokenStore = FakeSessionTokenStore()
        val store = FakeBackendStore(tokenProvider = tokenStore::currentToken)
        val remote = FakeAuthRemoteDataSource(store)
        val recoveryRepo = PasswordRecoveryRepositoryImpl(remote)

        assertEquals(
            ConfirmPasswordResetOutcome.InvalidOrExpiredCode,
            recoveryRepo.confirmReset("token-vencido", "NuevaClave99"),
        )
        assertEquals(
            ConfirmPasswordResetOutcome.WeakPassword,
            recoveryRepo.confirmReset("fake-reset-mgomez@uade.edu.ar", "corta"),
        )
    }

    @Test
    fun `perfil actualiza y refresca Room y notifica 401 si la sesion expiro`() = runTest {
        val tokenStore = FakeSessionTokenStore()
        val store = FakeBackendStore(tokenProvider = tokenStore::currentToken)
        val remote = FakeAuthRemoteDataSource(store)
        val userDao = FakeUserDao()
        var clearCalls = 0
        val cleaner = LocalSessionCleaner {
            clearCalls++
            userDao.clear()
        }
        val sessionRepo = SessionRepositoryImpl(remote, tokenStore, userDao, cleaner, RecordingPushNotifier(), backgroundScope)
        val profileRepo = ProfileRepositoryImpl(remote, userDao, tokenStore)
        runCurrent()

        sessionRepo.login("mgomez@uade.edu.ar", FakeBackendStore.DEMO_PASSWORD)
        val clearsAfterLogin = clearCalls
        val updatedProfile = sampleProfile(withVehicle = true)
        val updateResult = profileRepo.update(updatedProfile)
        assertTrue(updateResult is AppResult.Success)
        assertTrue(profileRepo.observeCurrentUser().first()?.canDrive == true)

        assertTrue(profileRepo.refresh() is AppResult.Success)
        assertEquals("AC321BD", profileRepo.observeCurrentUser().first()?.profile?.vehicle?.plate)

        // Simula token expirado/inválido en backend fake -> refresh responde 401 y limpia la sesión
        tokenStore.saveToken("fake-jwt-usuario-inexistente")
        val expiredRefresh = profileRepo.refresh()
        runCurrent()

        assertEquals(AppResult.Failure(DataError.UNAUTHORIZED), expiredRefresh)
        assertEquals(1, tokenStore.unauthorizedNotifications)
        assertEquals(clearsAfterLogin + 1, clearCalls)
        assertNull(profileRepo.observeCurrentUser().first())
    }

    @Test
    fun `registerDeviceToken delega el PUT al datasource remoto con sesion activa`() = runTest {
        val tokenStore = FakeSessionTokenStore()
        val store = FakeBackendStore(tokenProvider = tokenStore::currentToken)
        val remote = FakeAuthRemoteDataSource(store)
        val userDao = FakeUserDao()
        val cleaner = LocalSessionCleaner { userDao.clear() }
        val sessionRepo = SessionRepositoryImpl(remote, tokenStore, userDao, cleaner, RecordingPushNotifier(), backgroundScope)

        sessionRepo.login("lfernandez@uade.edu.ar", FakeBackendStore.DEMO_PASSWORD)
        val result = sessionRepo.registerDeviceToken("fcm-token-demo")

        assertEquals(AppResult.Success(Unit), result)
    }
}
