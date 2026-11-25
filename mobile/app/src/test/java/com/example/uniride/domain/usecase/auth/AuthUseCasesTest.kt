package com.example.uniride.domain.usecase.auth

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.Vehicle
import com.example.uniride.fake.FakeAccountRepository
import com.example.uniride.fake.FakePasswordRecoveryRepository
import com.example.uniride.fake.FakeProfileRepository
import com.example.uniride.fake.FakeSessionRepository
import com.example.uniride.fake.sampleUser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthUseCasesTest {

    private val validProfile = Profile(
        fullName = "  Lucía Fernández ",
        career = " Ingeniería en Informática ",
        phone = " +5491155550000 ",
        preferredMode = PreferredMode.AMBOS,
        vehicle = Vehicle(" Volkswagen ", " Gol Trend ", " Gris ", " ab123cd "),
    )

    @Test
    fun `registrar con dominio externo falla por validacion sin llamar al repositorio`() = runTest {
        val repo = FakeAccountRepository()
        val useCase = RegisterUseCase(repo)

        val result = useCase("usuario@gmail.com", "Secreta123", validProfile)

        assertEquals(AppResult.Failure(DataError.VALIDATION), result)
        assertNull(repo.lastRegisteredEmail)
    }

    @Test
    fun `registrar con datos validos normaliza correo y perfil antes de llamar al repositorio`() = runTest {
        val repo = FakeAccountRepository()
        val useCase = RegisterUseCase(repo)

        val result = useCase("  L.Fernandez@UADE.EDU.AR ", "Secreta123", validProfile)

        assertEquals(AppResult.Success(Unit), result)
        assertEquals("l.fernandez@uade.edu.ar", repo.lastRegisteredEmail)
        assertEquals("Lucía Fernández", repo.lastRegisteredProfile?.fullName)
        assertEquals("AB123CD", repo.lastRegisteredProfile?.vehicle?.plate)
    }

    @Test
    fun `activar cuenta rechaza token vacio y recorta espacios cuando es valido`() = runTest {
        val repo = FakeAccountRepository()
        val useCase = ActivateAccountUseCase(repo)

        assertEquals(AppResult.Failure(DataError.VALIDATION), useCase("   "))
        assertNull(repo.lastActivatedCode)

        val ok = useCase("  token-123  ")
        assertTrue(ok is AppResult.Success)
        assertEquals("token-123", repo.lastActivatedCode)
    }

    @Test
    fun `login valida dominio institucional y actualiza sesion hasta logout`() = runTest {
        val repo = FakeSessionRepository()
        val login = LoginUseCase(repo)
        val logout = LogoutUseCase(repo)
        val observeLoggedIn = ObserveIsLoggedInUseCase(repo)

        assertFalse(observeLoggedIn().first())
        assertEquals(AppResult.Failure(DataError.VALIDATION), login("otro@gmail.com", "Secreta123"))

        val result = login("  LFERNANDEZ@UADE.EDU.AR ", "Secreta123")
        assertTrue(result is AppResult.Success)
        assertEquals("lfernandez@uade.edu.ar", repo.lastLoginEmail)
        assertTrue(observeLoggedIn().first())

        logout()
        assertFalse(observeLoggedIn().first())
        assertEquals(1, repo.logoutCalls)
    }

    @Test
    fun `recuperacion de contrasena valida correo uade y politica de nueva clave`() = runTest {
        val repo = FakePasswordRecoveryRepository()
        val requestReset = RequestPasswordResetUseCase(repo)
        val confirmReset = ConfirmPasswordResetUseCase(repo)

        assertEquals(AppResult.Failure(DataError.VALIDATION), requestReset("malo@hotmail.com"))
        assertEquals(AppResult.Success(Unit), requestReset("  ALU@UADE.EDU.AR "))
        assertEquals("alu@uade.edu.ar", repo.lastResetEmail)

        assertEquals(AppResult.Failure(DataError.VALIDATION), confirmReset("code-1", "debil"))
        assertEquals(AppResult.Success(Unit), confirmReset(" code-1 ", "NuevaClave9"))
        assertEquals("code-1", repo.lastConfirmCode)
        assertEquals("NuevaClave9", repo.lastNewPassword)
    }

    @Test
    fun `perfil observa usuario refresca y actualiza normalizando datos`() = runTest {
        val repo = FakeProfileRepository(initialUser = sampleUser())
        val observeUser = ObserveCurrentUserUseCase(repo)
        val refresh = RefreshProfileUseCase(repo)
        val update = UpdateProfileUseCase(repo)

        assertEquals("lfernandez@uade.edu.ar", observeUser().first()?.email)
        assertEquals(AppResult.Success(Unit), refresh())
        assertEquals(1, repo.refreshCalls)

        val invalidProfile = validProfile.copy(fullName = "  ")
        assertEquals(AppResult.Failure(DataError.VALIDATION), update(invalidProfile))

        val updated = update(validProfile)
        assertTrue(updated is AppResult.Success)
        assertEquals("AB123CD", repo.lastUpdatedProfile?.vehicle?.plate)
    }
}
