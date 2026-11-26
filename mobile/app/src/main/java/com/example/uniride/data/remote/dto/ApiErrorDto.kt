package com.example.uniride.data.remote.dto

import com.squareup.moshi.JsonClass

/**
 * Standard error body returned by the backend GlobalExceptionHandler (docs/API.md §2).
 */
@JsonClass(generateAdapter = true)
data class ApiErrorDto(
    val code: String,
    val message: String,
    val fields: Map<String, String>? = null,
) {
    val errorCode: ApiErrorCode
        get() = ApiErrorCode.fromRaw(code)
}

/**
 * Known backend error codes documented in docs/API.md §2.
 */
enum class ApiErrorCode {
    VALIDATION_ERROR,
    INVALID_EMAIL_DOMAIN,
    WEAK_PASSWORD,
    INVALID_OR_EXPIRED_TOKEN,
    UNAUTHENTICATED,
    INVALID_CREDENTIALS,
    ACCOUNT_NOT_ACTIVATED,
    FORBIDDEN,
    CANNOT_RESERVE_OWN_TRIP,
    NOT_FOUND,
    EMAIL_ALREADY_REGISTERED,
    ID_CONFLICT,
    NO_SEATS_AVAILABLE,
    TRIP_NOT_OPEN,
    ALREADY_RESERVED,
    INVALID_STATUS_TRANSITION,
    TRIP_ALREADY_DEPARTED,
    METHOD_NOT_ALLOWED,
    UNSUPPORTED_MEDIA_TYPE,
    ALREADY_RATED,
    INTERNAL_ERROR,
    UNKNOWN;

    companion object {
        fun fromRaw(raw: String?): ApiErrorCode {
            val normalized = raw?.trim()?.uppercase() ?: return UNKNOWN
            return entries.firstOrNull { it.name == normalized } ?: UNKNOWN
        }
    }
}
