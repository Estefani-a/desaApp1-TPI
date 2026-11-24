package com.example.uniride.domain.repository

import com.example.uniride.core.result.AppResult

/** Forgotten password (RF01). */
interface PasswordRecoveryRepository {

    /** Succeeds the same way whether the email has an account or not, so it reveals nothing. */
    suspend fun requestReset(email: String): AppResult<Unit>

    /** Sets the new password with the code of the email link. */
    suspend fun confirmReset(resetCode: String, newPassword: String): AppResult<Unit>
}
