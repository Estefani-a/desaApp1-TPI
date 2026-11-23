package com.example.uniride.domain.usecase.auth

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.repository.AccountRepository
import com.example.uniride.domain.validation.EmailValidationResult
import com.example.uniride.domain.validation.EmailValidator
import com.example.uniride.domain.validation.PasswordPolicy
import com.example.uniride.domain.validation.PasswordValidationResult
import com.example.uniride.domain.validation.ProfileValidationResult
import com.example.uniride.domain.validation.ProfileValidator

/**
 * Registers a new UADE student account after enforcing email, password and profile domain rules (RF01).
 */
class RegisterUseCase(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        profile: Profile,
    ): AppResult<Unit> {
        if (EmailValidator.validate(email) !is EmailValidationResult.Valid) {
            return AppResult.Failure(DataError.VALIDATION)
        }
        if (PasswordPolicy.validate(password) !is PasswordValidationResult.Valid) {
            return AppResult.Failure(DataError.VALIDATION)
        }
        if (ProfileValidator.validate(profile) !is ProfileValidationResult.Valid) {
            return AppResult.Failure(DataError.VALIDATION)
        }

        return accountRepository.register(
            email = EmailValidator.normalize(email),
            password = password,
            profile = ProfileValidator.normalize(profile),
        )
    }
}
