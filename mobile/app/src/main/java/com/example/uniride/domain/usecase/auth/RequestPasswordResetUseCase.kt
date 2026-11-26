package com.example.uniride.domain.usecase.auth

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.repository.PasswordRecoveryRepository
import com.example.uniride.domain.validation.EmailValidationError
import com.example.uniride.domain.validation.EmailValidationResult
import com.example.uniride.domain.validation.EmailValidator
import javax.inject.Inject

/** Result of requesting a password recovery email (RF01). */
sealed interface RequestPasswordResetResult {
    data object Sent : RequestPasswordResetResult
    data class InvalidEmail(val error: EmailValidationError) : RequestPasswordResetResult
    data class Failed(val error: DataError) : RequestPasswordResetResult
}

/** Requests a password reset email for an institutional UADE address (RF01). */
class RequestPasswordResetUseCase @Inject constructor(
    private val passwordRecoveryRepository: PasswordRecoveryRepository,
) {
    suspend operator fun invoke(email: String): RequestPasswordResetResult {
        val emailResult = EmailValidator.validate(email)
        if (emailResult is EmailValidationResult.Invalid) {
            return RequestPasswordResetResult.InvalidEmail(emailResult.error)
        }
        return when (val result = passwordRecoveryRepository.requestReset(EmailValidator.normalize(email))) {
            is AppResult.Success -> RequestPasswordResetResult.Sent
            is AppResult.Failure -> RequestPasswordResetResult.Failed(result.error)
        }
    }
}
