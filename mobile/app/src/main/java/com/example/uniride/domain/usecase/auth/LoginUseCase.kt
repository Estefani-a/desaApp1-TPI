package com.example.uniride.domain.usecase.auth

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.User
import com.example.uniride.domain.repository.SessionRepository
import com.example.uniride.domain.validation.EmailValidationResult
import com.example.uniride.domain.validation.EmailValidator

/** Signs in a student with their institutional UADE email and password (RF01). */
class LoginUseCase(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(email: String, password: String): AppResult<User> {
        if (EmailValidator.validate(email) !is EmailValidationResult.Valid || password.isBlank()) {
            return AppResult.Failure(DataError.VALIDATION)
        }
        return sessionRepository.login(
            email = EmailValidator.normalize(email),
            password = password,
        )
    }
}
