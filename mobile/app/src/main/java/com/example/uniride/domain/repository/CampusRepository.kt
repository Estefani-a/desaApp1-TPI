package com.example.uniride.domain.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.domain.model.Campus
import kotlinx.coroutines.flow.Flow

/** Campuses and meeting points (`GET /campuses`, docs/API.md §5.2), cached in Room. */
interface CampusRepository {

    fun observeCampuses(): Flow<List<Campus>>

    suspend fun refreshCampuses(): AppResult<Unit>
}
