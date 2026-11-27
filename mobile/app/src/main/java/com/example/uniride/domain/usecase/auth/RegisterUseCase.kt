package com.example.uniride.domain.usecase.auth

import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.repository.AccountRepository
import com.example.uniride.domain.repository.RegisterOutcome
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
        val confirmPasswordBlank: Boolean = false,
        val passwordMismatch: Boolean = false,
        val profileErrors: Set<ProfileValidationError> = emptySet(),
    ) : RegisterResult

    data object EmailAlreadyRegistered : RegisterResult
    data class Failed(val error: DataError) : RegisterResult
}

/**
 * Registers a new UADE student account after enforcing email, password, confirmation,
 * and profile domain rules in one place (RF01).
 */
class RegisterUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        profile: Profile,
        confirmPassword: String = password,
    ): RegisterResult {
        val emailError = (EmailValidator.validate(email) as? EmailValidationResult.Invalid)?.error
        val passwordErrors = (PasswordPolicy.validate(password) as? PasswordValidationResult.Invalid)?.errors.orEmpty()
        val confirmPasswordBlank = confirmPassword.isBlank()
        val passwordMismatch = !confirmPasswordBlank && confirmPassword != password
        val profileErrors = (ProfileValidator.validate(profile) as? ProfileValidationResult.Invalid)?.errors.orEmpty()

        if (emailError != null ||
            passwordErrors.isNotEmpty() ||
            confirmPasswordBlank ||
            passwordMismatch ||
            profileErrors.isNotEmpty()
        ) {
            return RegisterResult.Invalid(
                emailError = emailError,
                passwordErrors = passwordErrors,
                confirmPasswordBlank = confirmPasswordBlank,
                passwordMismatch = passwordMismatch,
                profileErrors = profileErrors,
            )
        }

        return when (
            val outcome = accountRepository.register(
                email = EmailValidator.normalize(email),
                password = password,
                profile = ProfileValidator.normalize(profile),
            )
        ) {
            RegisterOutcome.Registered -> RegisterResult.Registered
            RegisterOutcome.EmailAlreadyRegistered -> RegisterResult.EmailAlreadyRegistered
            is RegisterOutcome.Failed -> RegisterResult.Failed(outcome.error)
        }
    }
}
