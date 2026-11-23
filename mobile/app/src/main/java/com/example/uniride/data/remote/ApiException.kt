package com.example.uniride.data.remote

import com.example.uniride.data.remote.dto.ApiErrorDto

/**
 * Common HTTP error exception shared by remote data sources (both Retrofit and fakes),
 * so safeApiCall handles HTTP errors through a single path.
 */
class ApiException(
    val httpCode: Int,
    val apiError: ApiErrorDto,
) : Exception(apiError.message)
