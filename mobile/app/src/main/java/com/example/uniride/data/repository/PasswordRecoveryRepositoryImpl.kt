package com.example.uniride.data.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.map
import com.example.uniride.data.remote.datasource.AuthRemoteDataSource
import com.example.uniride.data.remote.dto.ConfirmPasswordResetRequest
import com.example.uniride.data.remote.dto.PasswordResetRequest
import com.example.uniride.data.remote.safeApiCall
import com.example.uniride.domain.repository.PasswordRecoveryRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Password recovery flow repository (RF01). */
@Singleton
class PasswordRecoveryRepositoryImpl @Inject constructor(
    private val remote: AuthRemoteDataSource,
) : PasswordRecoveryRepository {

    override suspend fun requestReset(email: String): AppResult<Unit> = safeApiCall {
        remote.requestPasswordReset(PasswordResetRequest(email = email))
    }.map { }

    override suspend fun confirmReset(
        resetCode: String,
        newPassword: String,
    ): AppResult<Unit> = safeApiCall {
        remote.confirmPasswordReset(
            ConfirmPasswordResetRequest(token = resetCode, newPassword = newPassword),
        )
    }
}
