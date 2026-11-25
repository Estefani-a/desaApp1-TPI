package com.example.uniride.domain.usecase.auth

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.Vehicle
import com.example.uniride.domain.validation.EmailValidationError
import com.example.uniride.domain.validation.PasswordValidationError
import com.example.uniride.domain.validation.ProfileValidationError
import com.example.uniride.fake.FakeAccountRepository
import com.example.uniride.fake.FakePasswordRecoveryRepository
import com.example.uniride.fake.FakeSessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthUseCasesTest {

    private fun validProfile() = Profile(
        fullName = "  Lucía Fernández  ",
        career = " Ingeniería en Informática ",
        phone = " +5491155550001 ",
        preferredMode = PreferredMode.CONDUCTOR,
        vehicle = Vehicle(" Volkswagen ", " Gol Trend ", " Gris ", " ab123cd "),
    )

    @Test
    fun `registro rechaza correo no institucional sin llamar al repositorio`() = runTest {
        val repo = FakeAccountRepository()
        val useCase = RegisterUseCase(repo)

        val result = useCase("alumno@gmail.com", "Password1", validProfile())

        assertEquals(
            RegisterResult.Invalid(emailError = EmailValidationError.INVALID_DOMAIN),
            result,
        )
        assertNull(repo.lastRegisteredEmail)
    }

    @Test
    fun `registro rechaza contrasena debil sin llamar al repositorio`() = runTest {
        val repo = FakeAccountRepository()
        val useCase = RegisterUseCase(repo)

        val result = useCase("lfernandez@uade.edu.ar", "corta", validProfile())

        assertEquals(
            RegisterResult.Invalid(
                passwordErrors = setOf(
                    PasswordValidationError.TOO_SHORT,
                    PasswordValidationError.MISSING_DIGIT,
                ),
            ),
            result,
        )
        assertNull(repo.lastRegisteredEmail)
    }

    @Test
    fun `registro rechaza perfil de conductor sin vehiculo`() = runTest {
        val repo = FakeAccountRepository()
        val useCase = RegisterUseCase(repo)
        val profile = validProfile().copy(preferredMode = PreferredMode.CONDUCTOR, vehicle = null)

        val result = useCase("lfernandez@uade.edu.ar", "Password1", profile)

        assertEquals(
            RegisterResult.Invalid(profileErrors = setOf(ProfileValidationError.VEHICLE_REQUIRED)),
            result,
        )
        assertNull(repo.lastRegisteredEmail)
    }

    @Test
    fun `registro normaliza correo y perfil antes de delegar al repositorio`() = runTest {
        val repo = FakeAccountRepository()
        val useCase = RegisterUseCase(repo)

        val result = useCase("  LFERNANDEZ@UADE.EDU.AR ", "Password1", validProfile())

        assertEquals(RegisterResult.Registered, result)
        assertEquals("lfernandez@uade.edu.ar", repo.lastRegisteredEmail)
        assertEquals("AB123CD", repo.lastRegisteredProfile?.vehicle?.plate)
    }

    @Test
    fun `registro propaga error CONFLICT del repositorio`() = runTest {
        val repo = FakeAccountRepository().apply {
            registerResult = AppResult.Failure(DataError.CONFLICT)
        }
        val useCase = RegisterUseCase(repo)

        val result = useCase("lfernandez@uade.edu.ar", "Password1", validProfile())

        assertEquals(RegisterResult.Failed(DataError.CONFLICT), result)
    }

    @Test
    fun `activacion rechaza codigo en blanco`() = runTest {
        val repo = FakeAccountRepository()
        val useCase = ActivateAccountUseCase(repo)

        val result = useCase("   ")

        assertEquals(ActivateAccountResult.InvalidCode, result)
        assertNull(repo.lastActivatedCode)
    }

    @Test
    fun `activacion recorta espacios del codigo y activa la cuenta`() = runTest {
        val repo = FakeAccountRepository()
        val useCase = ActivateAccountUseCase(repo)

        val result = useCase("  tok-123  ")

        assertTrue(result is ActivateAccountResult.Activated)
        assertEquals("tok-123", repo.lastActivatedCode)
    }

    @Test
    fun `activacion propaga error del repositorio`() = runTest {
        val repo = FakeAccountRepository().apply {
            activateResult = AppResult.Failure(DataError.NO_CONNECTION)
        }
        val useCase = ActivateAccountUseCase(repo)

        assertEquals(ActivateAccountResult.Failed(DataError.NO_CONNECTION), useCase("tok-123"))
    }

    @Test
    fun `reenviar activacion rechaza correo no institucional`() = runTest {
        val repo = FakeAccountRepository()
        val useCase = ResendActivationUseCase(repo)

        val result = useCase("alumno@gmail.com")

        assertEquals(ResendActivationResult.InvalidEmail(EmailValidationError.INVALID_DOMAIN), result)
        assertNull(repo.lastResendEmail)
    }

    @Test
    fun `reenviar activacion normaliza correo valido`() = runTest {
        val repo = FakeAccountRepository()
        val useCase = ResendActivationUseCase(repo)

        val result = useCase("  LFERNANDEZ@UADE.EDU.AR ")

        assertEquals(ResendActivationResult.Sent, result)
        assertEquals("lfernandez@uade.edu.ar", repo.lastResendEmail)
    }

    @Test
    fun `reenviar activacion propaga error del repositorio`() = runTest {
        val repo = FakeAccountRepository().apply {
            resendResult = AppResult.Failure(DataError.TIMEOUT)
        }
        val useCase = ResendActivationUseCase(repo)

        assertEquals(ResendActivationResult.Failed(DataError.TIMEOUT), useCase("lfernandez@uade.edu.ar"))
    }

    @Test
    fun `login rechaza correo no institucional`() = runTest {
        val repo = FakeSessionRepository()
        val useCase = LoginUseCase(repo)

        val result = useCase("alumno@gmail.com", "Secreta123")

        assertEquals(LoginResult.Invalid(emailError = EmailValidationError.INVALID_DOMAIN), result)
        assertNull(repo.lastLoginEmail)
    }

    @Test
    fun `login rechaza contrasena en blanco`() = runTest {
        val repo = FakeSessionRepository()
        val useCase = LoginUseCase(repo)

        val result = useCase("lfernandez@uade.edu.ar", "   ")

        assertEquals(LoginResult.Invalid(passwordBlank = true), result)
        assertNull(repo.lastLoginEmail)
    }

    @Test
    fun `login normaliza correo valido y devuelve usuario`() = runTest {
        val repo = FakeSessionRepository()
        val useCase = LoginUseCase(repo)

        val result = useCase("  LFERNANDEZ@UADE.EDU.AR ", "Secreta123")

        assertTrue(result is LoginResult.LoggedIn)
        assertEquals("lfernandez@uade.edu.ar", repo.lastLoginEmail)
    }

    @Test
    fun `login propaga UNAUTHORIZED del repositorio`() = runTest {
        val repo = FakeSessionRepository().apply {
            loginResult = AppResult.Failure(DataError.UNAUTHORIZED)
        }
        val useCase = LoginUseCase(repo)

        assertEquals(LoginResult.Failed(DataError.UNAUTHORIZED), useCase("lfernandez@uade.edu.ar", "Mala1234"))
    }

    @Test
    fun `logout delega el cierre de sesion al repositorio`() = runTest {
        val repo = FakeSessionRepository(initialLoggedIn = true)
        val observeLoggedIn = ObserveIsLoggedInUseCase(repo)
        val logout = LogoutUseCase(repo)

        assertTrue(observeLoggedIn().first())
        logout()
        assertEquals(1, repo.logoutCalls)
        assertFalse(observeLoggedIn().first())
    }

    @Test
    fun `solicitar recuperacion rechaza correo no institucional`() = runTest {
        val repo = FakePasswordRecoveryRepository()
        val useCase = RequestPasswordResetUseCase(repo)

        val result = useCase("alumno@gmail.com")

        assertEquals(RequestPasswordResetResult.InvalidEmail(EmailValidationError.INVALID_DOMAIN), result)
        assertNull(repo.lastResetEmail)
    }

    @Test
    fun `solicitar recuperacion normaliza correo valido`() = runTest {
        val repo = FakePasswordRecoveryRepository()
        val useCase = RequestPasswordResetUseCase(repo)

        val result = useCase("  LFERNANDEZ@UADE.EDU.AR ")

        assertEquals(RequestPasswordResetResult.Sent, result)
        assertEquals("lfernandez@uade.edu.ar", repo.lastResetEmail)
    }

    @Test
    fun `confirmar recuperacion rechaza contrasena debil`() = runTest {
        val repo = FakePasswordRecoveryRepository()
        val useCase = ConfirmPasswordResetUseCase(repo)

        val result = useCase("reset-token-1", "debil")

        assertEquals(
            ConfirmPasswordResetResult.Invalid(
                passwordErrors = setOf(
                    PasswordValidationError.TOO_SHORT,
                    PasswordValidationError.MISSING_DIGIT,
                ),
            ),
            result,
        )
        assertNull(repo.lastConfirmCode)
    }

    @Test
    fun `confirmar recuperacion recorta token y delega contrasena valida`() = runTest {
        val repo = FakePasswordRecoveryRepository()
        val useCase = ConfirmPasswordResetUseCase(repo)

        val result = useCase("  reset-token-1  ", "NuevaClave1")

        assertEquals(ConfirmPasswordResetResult.Confirmed, result)
        assertEquals("reset-token-1", repo.lastConfirmCode)
        assertEquals("NuevaClave1", repo.lastNewPassword)
    }

    @Test
    fun `confirmar recuperacion propaga error del repositorio`() = runTest {
        val repo = FakePasswordRecoveryRepository().apply {
            confirmResult = AppResult.Failure(DataError.VALIDATION)
        }
        val useCase = ConfirmPasswordResetUseCase(repo)

        assertEquals(ConfirmPasswordResetResult.Failed(DataError.VALIDATION), useCase("tok", "NuevaClave1"))
    }
}
