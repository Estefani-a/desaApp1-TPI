package com.example.uniride.domain.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.domain.model.Campus
import kotlinx.coroutines.flow.Flow

/** UADE campuses and their meeting points. Available offline after the first refresh. */
interface CampusRepository {

    fun observeCampuses(): Flow<List<Campus>>

    suspend fun refresh(): AppResult<Unit>
}
