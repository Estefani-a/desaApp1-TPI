package com.example.uniride.domain.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError

/** Outcome of confirming a password reset with an email link code (RF01, docs/API.md §4.5). */
sealed interface ConfirmPasswordResetOutcome {
    data object Confirmed : ConfirmPasswordResetOutcome
    data object InvalidOrExpiredCode : ConfirmPasswordResetOutcome
    data object WeakPassword : ConfirmPasswordResetOutcome
    data class Failed(val error: DataError) : ConfirmPasswordResetOutcome
}

/** Forgotten password (RF01). */
interface PasswordRecoveryRepository {

    /** Succeeds the same way whether the email has an account or not, so it reveals nothing. */
    suspend fun requestReset(email: String): AppResult<Unit>

    /** Sets the new password with the code of the email link. */
    suspend fun confirmReset(resetCode: String, newPassword: String): ConfirmPasswordResetOutcome
}
