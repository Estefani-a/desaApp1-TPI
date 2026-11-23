package com.example.uniride.domain.usecase.auth

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.repository.PasswordRecoveryRepository
import com.example.uniride.domain.validation.EmailValidationResult
import com.example.uniride.domain.validation.EmailValidator

/** Requests a password reset email for an institutional UADE address (RF01). */
class RequestPasswordResetUseCase(
    private val passwordRecoveryRepository: PasswordRecoveryRepository,
) {
    suspend operator fun invoke(email: String): AppResult<Unit> {
        if (EmailValidator.validate(email) !is EmailValidationResult.Valid) {
            return AppResult.Failure(DataError.VALIDATION)
        }
        return passwordRecoveryRepository.requestReset(EmailValidator.normalize(email))
    }
}
