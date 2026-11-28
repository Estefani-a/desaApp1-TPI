package com.example.uniride.domain.usecase.auth

import com.example.uniride.core.result.DataError
import com.example.uniride.domain.repository.ConfirmPasswordResetOutcome
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
        val confirmPasswordBlank: Boolean = false,
        val passwordMismatch: Boolean = false,
    ) : ConfirmPasswordResetResult

    data object InvalidCode : ConfirmPasswordResetResult
    data object WeakPassword : ConfirmPasswordResetResult
    data class Failed(val error: DataError) : ConfirmPasswordResetResult
}

/** Confirms a password reset after checking [PasswordPolicy] and confirmation in one place (RF01). */
class ConfirmPasswordResetUseCase @Inject constructor(
    private val passwordRecoveryRepository: PasswordRecoveryRepository,
) {
    suspend operator fun invoke(
        resetCode: String,
        newPassword: String,
        confirmPassword: String = newPassword,
    ): ConfirmPasswordResetResult {
        val code = resetCode.trim()
        val codeBlank = code.isEmpty()
        val passwordErrors =
            (PasswordPolicy.validate(newPassword) as? PasswordValidationResult.Invalid)?.errors.orEmpty()
        val confirmPasswordBlank = confirmPassword.isBlank()
        val passwordMismatch = !confirmPasswordBlank && confirmPassword != newPassword

        if (codeBlank || passwordErrors.isNotEmpty() || confirmPasswordBlank || passwordMismatch) {
            return ConfirmPasswordResetResult.Invalid(
                codeBlank = codeBlank,
                passwordErrors = passwordErrors,
                confirmPasswordBlank = confirmPasswordBlank,
                passwordMismatch = passwordMismatch,
            )
        }

        return when (
            val outcome = passwordRecoveryRepository.confirmReset(
                resetCode = code,
                newPassword = newPassword,
            )
        ) {
            ConfirmPasswordResetOutcome.Confirmed -> ConfirmPasswordResetResult.Confirmed
            ConfirmPasswordResetOutcome.InvalidOrExpiredCode -> ConfirmPasswordResetResult.InvalidCode
            ConfirmPasswordResetOutcome.WeakPassword -> ConfirmPasswordResetResult.WeakPassword
            is ConfirmPasswordResetOutcome.Failed -> ConfirmPasswordResetResult.Failed(outcome.error)
        }
    }
}
