package com.example.uniride.domain.usecase.auth

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.User
import com.example.uniride.domain.repository.AccountRepository

/** Activates a pending account using the token received in the email deep link (RF01). */
class ActivateAccountUseCase(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(activationCode: String): AppResult<User> {
        val code = activationCode.trim()
        if (code.isEmpty()) {
            return AppResult.Failure(DataError.VALIDATION)
        }
        return accountRepository.activate(code)
    }
}
