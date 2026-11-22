package com.example.uniride.data.remote.datasource

import com.example.uniride.data.remote.dto.ApiErrorDto

/**
 * Exception thrown by Fake*RemoteDataSource implementations to mirror backend HTTP errors.
 * Allows safeApiCall to map fake and real HTTP failures through the same DataError path.
 */
class FakeApiException(
    val httpCode: Int,
    val apiError: ApiErrorDto,
) : Exception(apiError.message)
