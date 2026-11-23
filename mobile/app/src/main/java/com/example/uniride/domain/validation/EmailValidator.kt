package com.example.uniride.domain.validation

enum class EmailValidationError {
    BLANK,
    INVALID_FORMAT,
    INVALID_DOMAIN,
}

sealed interface EmailValidationResult {
    data object Valid : EmailValidationResult
    data class Invalid(val error: EmailValidationError) : EmailValidationResult
}

/**
 * Validates institutional UADE email addresses (@uade.edu.ar) for RF01.
 * Pure Kotlin rule with no Android framework dependencies.
 */
object EmailValidator {

    const val ALLOWED_DOMAIN = "uade.edu.ar"

    private val EMAIL_FORMAT_REGEX = Regex(
        pattern = "^[A-Za-z0-9_%+-]+(?:\\.[A-Za-z0-9_%+-]+)*@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+$"
    )

    fun normalize(rawEmail: String): String = rawEmail.trim().lowercase()

    fun validate(rawEmail: String): EmailValidationResult {
        val normalized = normalize(rawEmail)

        if (normalized.isEmpty()) {
            return EmailValidationResult.Invalid(EmailValidationError.BLANK)
        }

        if (!EMAIL_FORMAT_REGEX.matches(normalized)) {
            return EmailValidationResult.Invalid(EmailValidationError.INVALID_FORMAT)
        }

        val domain = normalized.substringAfter('@')
        if (domain != ALLOWED_DOMAIN) {
            return EmailValidationResult.Invalid(EmailValidationError.INVALID_DOMAIN)
        }

        return EmailValidationResult.Valid
    }
}
