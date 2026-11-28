package com.example.uniride.data.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.map
import com.example.uniride.data.remote.datasource.AuthRemoteDataSource
import com.example.uniride.data.remote.dto.ApiErrorCode
import com.example.uniride.data.remote.dto.ConfirmPasswordResetRequest
import com.example.uniride.data.remote.dto.PasswordResetRequest
import com.example.uniride.data.remote.safeApiCall
import com.example.uniride.data.remote.safeApiCallWithCode
import com.example.uniride.domain.repository.ConfirmPasswordResetOutcome
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
    ): ConfirmPasswordResetOutcome {
        val call = safeApiCallWithCode {
            remote.confirmPasswordReset(
                ConfirmPasswordResetRequest(token = resetCode, newPassword = newPassword),
            )
        }
        return when (val result = call.result) {
            is AppResult.Success -> ConfirmPasswordResetOutcome.Confirmed
            is AppResult.Failure -> when (call.errorCode) {
                ApiErrorCode.INVALID_OR_EXPIRED_TOKEN,
                ApiErrorCode.NOT_FOUND,
                -> ConfirmPasswordResetOutcome.InvalidOrExpiredCode
                ApiErrorCode.WEAK_PASSWORD -> ConfirmPasswordResetOutcome.WeakPassword
                else -> ConfirmPasswordResetOutcome.Failed(result.error)
            }
        }
    }
}
