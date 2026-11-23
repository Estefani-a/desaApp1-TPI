package com.example.uniride.domain.usecase.auth

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.repository.PasswordRecoveryRepository
import com.example.uniride.domain.validation.PasswordPolicy
import com.example.uniride.domain.validation.PasswordValidationResult

/** Confirms a password reset after checking that the new password satisfies [PasswordPolicy] (RF01). */
class ConfirmPasswordResetUseCase(
    private val passwordRecoveryRepository: PasswordRecoveryRepository,
) {
    suspend operator fun invoke(resetCode: String, newPassword: String): AppResult<Unit> {
        val code = resetCode.trim()
        if (code.isEmpty() || PasswordPolicy.validate(newPassword) !is PasswordValidationResult.Valid) {
            return AppResult.Failure(DataError.VALIDATION)
        }
        return passwordRecoveryRepository.confirmReset(
            resetCode = code,
            newPassword = newPassword,
        )
    }
}
