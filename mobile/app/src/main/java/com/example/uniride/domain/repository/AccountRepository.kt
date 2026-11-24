package com.example.uniride.domain.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.User

/** Creating and activating an account with the institutional email (RF01). */
interface AccountRepository {

    /** The account stays pending until the student opens the link of the activation email. */
    suspend fun register(email: String, password: String, profile: Profile): AppResult<Unit>

    /** Activates the account with the code of the email link and starts the session. */
    suspend fun activate(activationCode: String): AppResult<User>

    suspend fun resendActivation(email: String): AppResult<Unit>
}
