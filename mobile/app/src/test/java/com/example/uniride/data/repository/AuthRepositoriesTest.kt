package com.example.uniride.data.repository

import app.cash.turbine.test
import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.data.remote.datasource.AuthRemoteDataSource
import com.example.uniride.data.remote.datasource.FakeAuthRemoteDataSource
import com.example.uniride.data.remote.datasource.FakeBackendStore
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.Vehicle
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

    private fun sampleProfile(withVehicle: Boolean = false) = Profile(
        fullName = "Juan Pérez",
        career = "Ingeniería en Informática",
        phone = "+5491144445555",
        preferredMode = if (withVehicle) PreferredMode.AMBOS else PreferredMode.PASAJERO,
        vehicle = if (withVehicle) Vehicle("Ford", "Fiesta", "Rojo", "AC321BD") else null,
    )

    @Test
    fun `registro no inicia sesion y activacion guarda usuario en Room y token cifrado`() = runTest {
        val tokenStore = FakeSessionTokenStore()
        val store = FakeBackendStore(tokenProvider = tokenStore::currentToken)
        val remote = FakeAuthRemoteDataSource(store)
        val userDao = FakeUserDao()
        val accountRepo = AccountRepositoryImpl(remote, tokenStore, userDao)

        val regResult = accountRepo.register(
            email = "jperez@uade.edu.ar",
            password = "Password1",
            profile = sampleProfile(),
        )
        assertTrue(regResult is AppResult.Success)
        assertNull(tokenStore.currentToken())
        assertNull(userDao.getCurrentUser())

        val resendResult = accountRepo.resendActivation("jperez@uade.edu.ar")
        assertTrue(resendResult is AppResult.Success)

        val activateResult = accountRepo.activate("fake-activate-jperez@uade.edu.ar")
        assertTrue(activateResult is AppResult.Success)
        val user = (activateResult as AppResult.Success).data
        assertEquals("jperez@uade.edu.ar", user.email)
        assertNotNull(tokenStore.currentToken())
        assertEquals("jperez@uade.edu.ar", userDao.getCurrentUser()?.email)
    }

    @Test
    fun `registro duplicado devuelve CONFLICT y codigo de activacion invalido devuelve VALIDATION`() = runTest {
        val tokenStore = FakeSessionTokenStore()
        val store = FakeBackendStore(tokenProvider = tokenStore::currentToken)
        val remote = FakeAuthRemoteDataSource(store)
        val userDao = FakeUserDao()
        val accountRepo = AccountRepositoryImpl(remote, tokenStore, userDao)

        val duplicate = accountRepo.register(
            email = "lfernandez@uade.edu.ar",
            password = "Password1",
            profile = sampleProfile(),
        )
        assertEquals(AppResult.Failure(DataError.CONFLICT), duplicate)

        val invalidCode = accountRepo.activate("token-inexistente")
        assertEquals(AppResult.Failure(DataError.VALIDATION), invalidCode)
    }

    @Test
    fun `login exitoso guarda token y usuario en Room y emite sesion activa`() = runTest {
        val tokenStore = FakeSessionTokenStore()
        val store = FakeBackendStore(tokenProvider = tokenStore::currentToken)
        val remote = FakeAuthRemoteDataSource(store)
        val userDao = FakeUserDao()
        val cleaner = LocalSessionCleaner { userDao.clear() }
        val sessionRepo = SessionRepositoryImpl(remote, tokenStore, userDao, cleaner, backgroundScope)
        val profileRepo = ProfileRepositoryImpl(remote, userDao, tokenStore)

        sessionRepo.observeIsLoggedIn().test {
            assertFalse(awaitItem())

            val result = sessionRepo.login("lfernandez@uade.edu.ar", FakeBackendStore.DEMO_PASSWORD)
            assertTrue(result is AppResult.Success)
            assertTrue(awaitItem())
            assertEquals("lfernandez@uade.edu.ar", profileRepo.observeCurrentUser().first()?.email)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `login con clave incorrecta devuelve UNAUTHORIZED sin persistir usuario ni token`() = runTest {
        val tokenStore = FakeSessionTokenStore()
        val store = FakeBackendStore(tokenProvider = tokenStore::currentToken)
        val remote = FakeAuthRemoteDataSource(store)
        val userDao = FakeUserDao()
        val cleaner = LocalSessionCleaner { userDao.clear() }
        val sessionRepo = SessionRepositoryImpl(remote, tokenStore, userDao, cleaner, backgroundScope)

        val result = sessionRepo.login("lfernandez@uade.edu.ar", "ClaveMala99")

        assertEquals(AppResult.Failure(DataError.UNAUTHORIZED), result)
        assertNull(tokenStore.currentToken())
        assertNull(userDao.getCurrentUser())
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
        val sessionRepo = SessionRepositoryImpl(remote, tokenStore, userDao, cleaner, backgroundScope)

        sessionRepo.login("mgomez@uade.edu.ar", FakeBackendStore.DEMO_PASSWORD)
        assertNotNull(tokenStore.currentToken())
        assertNotNull(userDao.getCurrentUser())

        failDeleteDeviceToken = true
        sessionRepo.logout()

        assertNull(tokenStore.currentToken())
        assertNull(userDao.getCurrentUser())
        assertEquals(1, clearCalls)
        assertFalse(sessionRepo.observeIsLoggedIn().first())
    }

    @Test
    fun `evento global de 401 dispara limpieza de Room y cierra la sesion automaticamente`() = runTest {
        val tokenStore = FakeSessionTokenStore()
        val store = FakeBackendStore(tokenProvider = tokenStore::currentToken)
        val remote = FakeAuthRemoteDataSource(store)
        val userDao = FakeUserDao()
        var clearCalls = 0
        val cleaner = LocalSessionCleaner {
            clearCalls++
            userDao.clear()
        }
        val sessionRepo = SessionRepositoryImpl(remote, tokenStore, userDao, cleaner, backgroundScope)
        runCurrent()

        sessionRepo.login("lfernandez@uade.edu.ar", FakeBackendStore.DEMO_PASSWORD)
        assertTrue(sessionRepo.observeIsLoggedIn().first())

        tokenStore.notifyUnauthorized()
        runCurrent()

        assertEquals(1, clearCalls)
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
        val sessionRepo = SessionRepositoryImpl(remote, tokenStore, userDao, cleaner, backgroundScope)

        assertTrue(recoveryRepo.requestReset("mgomez@uade.edu.ar") is AppResult.Success)
        assertTrue(
            recoveryRepo.confirmReset("fake-reset-mgomez@uade.edu.ar", "NuevaClave99") is AppResult.Success,
        )

        assertEquals(
            AppResult.Failure(DataError.UNAUTHORIZED),
            sessionRepo.login("mgomez@uade.edu.ar", FakeBackendStore.DEMO_PASSWORD),
        )
        assertTrue(sessionRepo.login("mgomez@uade.edu.ar", "NuevaClave99") is AppResult.Success)
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
        val sessionRepo = SessionRepositoryImpl(remote, tokenStore, userDao, cleaner, backgroundScope)
        val profileRepo = ProfileRepositoryImpl(remote, userDao, tokenStore)
        runCurrent()

        sessionRepo.login("mgomez@uade.edu.ar", FakeBackendStore.DEMO_PASSWORD)
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
        assertEquals(1, clearCalls)
        assertNull(profileRepo.observeCurrentUser().first())
    }
}
