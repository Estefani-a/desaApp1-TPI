package com.example.uniride.domain.usecase.auth

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.repository.AccountRepository
import com.example.uniride.domain.validation.EmailValidationError
import com.example.uniride.domain.validation.EmailValidationResult
import com.example.uniride.domain.validation.EmailValidator
import com.example.uniride.domain.validation.PasswordPolicy
import com.example.uniride.domain.validation.PasswordValidationError
import com.example.uniride.domain.validation.PasswordValidationResult
import com.example.uniride.domain.validation.ProfileValidationError
import com.example.uniride.domain.validation.ProfileValidationResult
import com.example.uniride.domain.validation.ProfileValidator
import javax.inject.Inject

/** Result of trying to register a new institutional account (RF01). */
sealed interface RegisterResult {
    data object Registered : RegisterResult

    data class Invalid(
        val emailError: EmailValidationError? = null,
        val passwordErrors: Set<PasswordValidationError> = emptySet(),
        val profileErrors: Set<ProfileValidationError> = emptySet(),
    ) : RegisterResult

    data class Failed(val error: DataError) : RegisterResult
}

/**
 * Registers a new UADE student account after enforcing email, password and profile domain rules (RF01).
 */
class RegisterUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        profile: Profile,
    ): RegisterResult {
        val emailError = (EmailValidator.validate(email) as? EmailValidationResult.Invalid)?.error
        val passwordErrors = (PasswordPolicy.validate(password) as? PasswordValidationResult.Invalid)?.errors.orEmpty()
        val profileErrors = (ProfileValidator.validate(profile) as? ProfileValidationResult.Invalid)?.errors.orEmpty()

        if (emailError != null || passwordErrors.isNotEmpty() || profileErrors.isNotEmpty()) {
            return RegisterResult.Invalid(
                emailError = emailError,
                passwordErrors = passwordErrors,
                profileErrors = profileErrors,
            )
        }

        return when (
            val result = accountRepository.register(
                email = EmailValidator.normalize(email),
                password = password,
                profile = ProfileValidator.normalize(profile),
            )
        ) {
            is AppResult.Success -> RegisterResult.Registered
            is AppResult.Failure -> RegisterResult.Failed(result.error)
        }
    }
}
