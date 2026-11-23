package com.example.uniride.domain.validation

enum class PasswordValidationError {
    BLANK,
    TOO_SHORT,
    MISSING_LETTER,
    MISSING_DIGIT,
}

sealed interface PasswordValidationResult {
    data object Valid : PasswordValidationResult
    data class Invalid(val errors: Set<PasswordValidationError>) : PasswordValidationResult {
        /**
         * First validation error according to the insertion order in [PasswordPolicy.validate]
         * (BLANK, otherwise TOO_SHORT -> MISSING_LETTER -> MISSING_DIGIT).
         */
        val primaryError: PasswordValidationError
            get() = errors.first()
    }
}

/**
 * Validates the password strength rules for RF01:
 * minimum 8 characters, at least one letter (Unicode via Char.isLetter) and one ASCII digit.
 */
object PasswordPolicy {

    const val MIN_LENGTH = 8

    fun validate(password: String): PasswordValidationResult {
        if (password.isBlank()) {
            return PasswordValidationResult.Invalid(setOf(PasswordValidationError.BLANK))
        }

        // Preserve priority order: PasswordValidationResult.Invalid.primaryError relies on it.
        val errors = buildSet {
            if (password.length < MIN_LENGTH) {
                add(PasswordValidationError.TOO_SHORT)
            }
            if (password.none { it.isLetter() }) {
                add(PasswordValidationError.MISSING_LETTER)
            }
            if (password.none { it in '0'..'9' }) {
                add(PasswordValidationError.MISSING_DIGIT)
            }
        }

        return if (errors.isEmpty()) {
            PasswordValidationResult.Valid
        } else {
            PasswordValidationResult.Invalid(errors)
        }
    }
}
