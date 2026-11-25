package com.example.uniride.domain.usecase.auth

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.repository.AccountRepository
import com.example.uniride.domain.validation.EmailValidationError
import com.example.uniride.domain.validation.EmailValidationResult
import com.example.uniride.domain.validation.EmailValidator
import javax.inject.Inject

/** Result of requesting a new activation email (RF01). */
sealed interface ResendActivationResult {
    data object Sent : ResendActivationResult
    data class InvalidEmail(val error: EmailValidationError) : ResendActivationResult
    data class Failed(val error: DataError) : ResendActivationResult
}

/** Requests a new activation email for an institutional UADE address (RF01). */
class ResendActivationUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(email: String): ResendActivationResult {
        val emailResult = EmailValidator.validate(email)
        if (emailResult is EmailValidationResult.Invalid) {
            return ResendActivationResult.InvalidEmail(emailResult.error)
        }
        return when (val result = accountRepository.resendActivation(EmailValidator.normalize(email))) {
            is AppResult.Success -> ResendActivationResult.Sent
            is AppResult.Failure -> ResendActivationResult.Failed(result.error)
        }
    }
}
