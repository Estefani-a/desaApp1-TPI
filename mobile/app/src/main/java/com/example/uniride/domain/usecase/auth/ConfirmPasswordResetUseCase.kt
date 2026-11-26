package com.example.uniride.domain.usecase.auth

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.repository.PasswordRecoveryRepository
import com.example.uniride.domain.validation.PasswordPolicy
import com.example.uniride.domain.validation.PasswordValidationError
import com.example.uniride.domain.validation.PasswordValidationResult
import javax.inject.Inject

/** Result of confirming a password reset with a deep link token (RF01). */
sealed interface ConfirmPasswordResetResult {
    data object Confirmed : ConfirmPasswordResetResult

    data class Invalid(
        val codeBlank: Boolean = false,
        val passwordErrors: Set<PasswordValidationError> = emptySet(),
    ) : ConfirmPasswordResetResult

    data class Failed(val error: DataError) : ConfirmPasswordResetResult
}

/** Confirms a password reset after checking that the new password satisfies [PasswordPolicy] (RF01). */
class ConfirmPasswordResetUseCase @Inject constructor(
    private val passwordRecoveryRepository: PasswordRecoveryRepository,
) {
    suspend operator fun invoke(resetCode: String, newPassword: String): ConfirmPasswordResetResult {
        val code = resetCode.trim()
        val codeBlank = code.isEmpty()
        val passwordErrors =
            (PasswordPolicy.validate(newPassword) as? PasswordValidationResult.Invalid)?.errors.orEmpty()

        if (codeBlank || passwordErrors.isNotEmpty()) {
            return ConfirmPasswordResetResult.Invalid(
                codeBlank = codeBlank,
                passwordErrors = passwordErrors,
            )
        }

        return when (
            val result = passwordRecoveryRepository.confirmReset(
                resetCode = code,
                newPassword = newPassword,
            )
        ) {
            is AppResult.Success -> ConfirmPasswordResetResult.Confirmed
            is AppResult.Failure -> ConfirmPasswordResetResult.Failed(result.error)
        }
    }
}
