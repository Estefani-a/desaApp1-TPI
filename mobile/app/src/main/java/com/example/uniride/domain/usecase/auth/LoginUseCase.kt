package com.example.uniride.domain.usecase.auth

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.User
import com.example.uniride.domain.repository.SessionRepository
import com.example.uniride.domain.validation.EmailValidationError
import com.example.uniride.domain.validation.EmailValidationResult
import com.example.uniride.domain.validation.EmailValidator
import javax.inject.Inject

/** Result of signing in with institutional email and password (RF01). */
sealed interface LoginResult {
    data class LoggedIn(val user: User) : LoginResult

    data class Invalid(
        val emailError: EmailValidationError? = null,
        val passwordBlank: Boolean = false,
    ) : LoginResult

    data class Failed(val error: DataError) : LoginResult
}

/** Signs in a student with their institutional UADE email and password (RF01). */
class LoginUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(email: String, password: String): LoginResult {
        val emailError = (EmailValidator.validate(email) as? EmailValidationResult.Invalid)?.error
        val passwordBlank = password.isBlank()

        if (emailError != null || passwordBlank) {
            return LoginResult.Invalid(emailError = emailError, passwordBlank = passwordBlank)
        }

        return when (
            val result = sessionRepository.login(
                email = EmailValidator.normalize(email),
                password = password,
            )
        ) {
            is AppResult.Success -> LoginResult.LoggedIn(result.data)
            is AppResult.Failure -> LoginResult.Failed(result.error)
        }
    }
}
