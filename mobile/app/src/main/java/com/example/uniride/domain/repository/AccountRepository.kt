package com.example.uniride.domain.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.User

/** Outcome of creating a new institutional account (RF01, docs/API.md §4.1). */
sealed interface RegisterOutcome {
    data object Registered : RegisterOutcome
    data object EmailAlreadyRegistered : RegisterOutcome
    data class Failed(val error: DataError) : RegisterOutcome
}

/** Outcome of activating a pending account from an email link (RF01, docs/API.md §4.2). */
sealed interface ActivateOutcome {
    data class Activated(val user: User) : ActivateOutcome
    data object InvalidOrExpiredCode : ActivateOutcome
    data class Failed(val error: DataError) : ActivateOutcome
}

/** Creating and activating an account with the institutional email (RF01). */
interface AccountRepository {

    /** The account stays pending until the student opens the link of the activation email. */
    suspend fun register(email: String, password: String, profile: Profile): RegisterOutcome

    /** Activates the account with the code of the email link and starts the session. */
    suspend fun activate(activationCode: String): ActivateOutcome

    suspend fun resendActivation(email: String): AppResult<Unit>
}
