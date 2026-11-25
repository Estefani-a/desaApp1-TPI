package com.example.uniride.core.result

/**
 * Result of a repository operation (CONVENCIONES §6.3): repositories never throw.
 *
 * Helpers (`map`, `onSuccess`, `onFailure`, `getOrNull`, `fold`) live in AppResultExtensions.kt.
 */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: DataError) : AppResult<Nothing>
}

enum class DataError {
    NO_CONNECTION, // IOException / UnknownHostException
    TIMEOUT,
    UNAUTHORIZED, // 401 → se cierra la sesión (11.3)
    NOT_FOUND, // 404
    CONFLICT, // 409 (por ejemplo, sin vacantes)
    VALIDATION, // 400/422
    SERVER, // 5xx
    UNKNOWN,
}
