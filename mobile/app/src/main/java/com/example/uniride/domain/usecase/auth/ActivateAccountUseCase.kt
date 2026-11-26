package com.example.uniride.domain.usecase.auth

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.User
import com.example.uniride.domain.repository.AccountRepository
import javax.inject.Inject

/** Result of activating a pending account from an email deep link (RF01). */
sealed interface ActivateAccountResult {
    data class Activated(val user: User) : ActivateAccountResult
    data object InvalidCode : ActivateAccountResult
    data class Failed(val error: DataError) : ActivateAccountResult
}

/** Activates a pending account using the token received in the email deep link (RF01). */
class ActivateAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(activationCode: String): ActivateAccountResult {
        val code = activationCode.trim()
        if (code.isEmpty()) {
            return ActivateAccountResult.InvalidCode
        }
        return when (val result = accountRepository.activate(code)) {
            is AppResult.Success -> ActivateAccountResult.Activated(result.data)
            is AppResult.Failure -> ActivateAccountResult.Failed(result.error)
        }
    }
}
