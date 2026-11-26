package com.example.uniride.domain.usecase.profile

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.Vehicle
import com.example.uniride.domain.validation.ProfileValidationError
import com.example.uniride.fake.FakeProfileRepository
import com.example.uniride.fake.sampleUser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileUseCasesTest {

    @Test
    fun `observar usuario actual devuelve el flujo del repositorio`() = runTest {
        val user = sampleUser()
        val repo = FakeProfileRepository(initialUser = user)
        val useCase = ObserveCurrentUserUseCase(repo)

        assertEquals(user, useCase().first())
    }

    @Test
    fun `refrescar perfil delega al repositorio`() = runTest {
        val repo = FakeProfileRepository()
        val useCase = RefreshProfileUseCase(repo)

        assertEquals(AppResult.Success(Unit), useCase())
        assertEquals(1, repo.refreshCalls)
    }

    @Test
    fun `actualizar perfil rechaza datos invalidos sin llamar al repositorio`() = runTest {
        val repo = FakeProfileRepository()
        val useCase = UpdateProfileUseCase(repo)
        val invalid = Profile(" ", "Ingeniería", "hola", PreferredMode.CONDUCTOR, vehicle = null)

        val result = useCase(invalid)

        assertEquals(
            UpdateProfileResult.Invalid(
                setOf(
                    ProfileValidationError.FULL_NAME_BLANK,
                    ProfileValidationError.PHONE_INVALID,
                    ProfileValidationError.VEHICLE_REQUIRED,
                ),
            ),
            result,
        )
        assertNull(repo.lastUpdatedProfile)
    }

    @Test
    fun `actualizar perfil normaliza datos antes de delegar al repositorio`() = runTest {
        val repo = FakeProfileRepository(initialUser = sampleUser())
        val useCase = UpdateProfileUseCase(repo)
        val raw = Profile(
            fullName = "  Lucía Fernández  ",
            career = " Ingeniería ",
            phone = " +5491155550001 ",
            preferredMode = PreferredMode.CONDUCTOR,
            vehicle = Vehicle(" Toyota ", " Yaris ", " Blanco ", " af456gh "),
        )

        val result = useCase(raw)

        assertTrue(result is UpdateProfileResult.Updated)
        assertEquals("Lucía Fernández", repo.lastUpdatedProfile?.fullName)
        assertEquals("AF456GH", repo.lastUpdatedProfile?.vehicle?.plate)
    }

    @Test
    fun `actualizar perfil propaga error del repositorio`() = runTest {
        val repo = FakeProfileRepository().apply {
            updateResult = AppResult.Failure(DataError.SERVER)
        }
        val useCase = UpdateProfileUseCase(repo)
        val valid = Profile("Lucía Fernández", "Ingeniería", "+5491155550001", PreferredMode.PASAJERO, null)

        assertEquals(UpdateProfileResult.Failed(DataError.SERVER), useCase(valid))
    }
}
