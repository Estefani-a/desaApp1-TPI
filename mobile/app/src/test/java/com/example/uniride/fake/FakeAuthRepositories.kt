package com.example.uniride.fake

import com.example.uniride.core.result.AppResult
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.Rating
import com.example.uniride.domain.model.User
import com.example.uniride.domain.model.UserId
import com.example.uniride.domain.repository.AccountRepository
import com.example.uniride.domain.repository.ActivateOutcome
import com.example.uniride.domain.repository.ConfirmPasswordResetOutcome
import com.example.uniride.domain.repository.LoginOutcome
import com.example.uniride.domain.repository.PasswordRecoveryRepository
import com.example.uniride.domain.repository.ProfileRepository
import com.example.uniride.domain.repository.RegisterOutcome
import com.example.uniride.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Shared fakes for RF01 domain repositories used by UseCase and ViewModel unit tests. */
class FakeAccountRepository : AccountRepository {
    var lastRegisteredEmail: String? = null
    var lastRegisteredProfile: Profile? = null
    var lastActivatedCode: String? = null
    var activateCalls: Int = 0
    var lastResendEmail: String? = null

    var registerOutcome: RegisterOutcome = RegisterOutcome.Registered
    var activateOutcome: ActivateOutcome = ActivateOutcome.Activated(sampleUser())
    var resendResult: AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun register(email: String, password: String, profile: Profile): RegisterOutcome {
        lastRegisteredEmail = email
        lastRegisteredProfile = profile
        return registerOutcome
    }

    override suspend fun activate(activationCode: String): ActivateOutcome {
        activateCalls++
        lastActivatedCode = activationCode
        return activateOutcome
    }

    override suspend fun resendActivation(email: String): AppResult<Unit> {
        lastResendEmail = email
        return resendResult
    }
}

class FakeSessionRepository(
    initialLoggedIn: Boolean = false,
) : SessionRepository {
    private val loggedIn = MutableStateFlow(initialLoggedIn)
    var lastLoginEmail: String? = null
    var logoutCalls: Int = 0
    var loginOutcome: LoginOutcome = LoginOutcome.Success(sampleUser())

    override fun observeIsLoggedIn(): Flow<Boolean> = loggedIn.asStateFlow()

    override suspend fun login(email: String, password: String): LoginOutcome {
        lastLoginEmail = email
        if (loginOutcome is LoginOutcome.Success) {
            loggedIn.value = true
        }
        return loginOutcome
    }

    override suspend fun logout() {
        logoutCalls++
        loggedIn.value = false
    }
}

class FakePasswordRecoveryRepository : PasswordRecoveryRepository {
    var lastResetEmail: String? = null
    var lastConfirmCode: String? = null
    var lastNewPassword: String? = null
    var requestResult: AppResult<Unit> = AppResult.Success(Unit)
    var confirmOutcome: ConfirmPasswordResetOutcome = ConfirmPasswordResetOutcome.Confirmed

    override suspend fun requestReset(email: String): AppResult<Unit> {
        lastResetEmail = email
        return requestResult
    }

    override suspend fun confirmReset(resetCode: String, newPassword: String): ConfirmPasswordResetOutcome {
        lastConfirmCode = resetCode
        lastNewPassword = newPassword
        return confirmOutcome
    }
}

class FakeProfileRepository(
    initialUser: User? = null,
) : ProfileRepository {
    val currentUser = MutableStateFlow(initialUser)
    var refreshCalls: Int = 0
    var lastUpdatedProfile: Profile? = null
    var refreshResult: AppResult<Unit> = AppResult.Success(Unit)
    var updateResult: AppResult<User>? = null

    override fun observeCurrentUser(): Flow<User?> = currentUser.asStateFlow()

    override suspend fun refresh(): AppResult<Unit> {
        refreshCalls++
        return refreshResult
    }

    override suspend fun update(profile: Profile): AppResult<User> {
        lastUpdatedProfile = profile
        updateResult?.let { return it }
        val updated = (currentUser.value ?: sampleUser()).copy(profile = profile)
        currentUser.value = updated
        return AppResult.Success(updated)
    }
}

internal fun sampleUser(
    email: String = "lfernandez@uade.edu.ar",
    profile: Profile = Profile(
        fullName = "Lucía Fernández",
        career = "Ingeniería en Informática",
        phone = "+5491155550000",
        preferredMode = PreferredMode.AMBOS,
        vehicle = null,
    ),
): User = User(
    id = UserId("6f1c2a4e-1b0e-4a8e-9a55-2c1f3e7b9d10"),
    email = email,
    profile = profile,
    rating = Rating(average = 4.8, count = 12),
)
