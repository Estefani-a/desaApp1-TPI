package com.example.uniride.data.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.data.mapper.toDomain
import com.example.uniride.data.remote.datasource.TripRemoteDataSource
import com.example.uniride.data.remote.safeApiCall
import com.example.uniride.domain.model.Campus
import com.example.uniride.domain.repository.CampusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * **Provisional** [CampusRepository] while Room (E1) is not merged: the campuses are kept in
 * memory after the first refresh. With Room they are cached in a table, like the trips.
 */
@Singleton
class InMemoryCampusRepository @Inject constructor(
    private val remote: TripRemoteDataSource,
) : CampusRepository {

    private val campuses = MutableStateFlow<List<Campus>>(emptyList())

    override fun observeCampuses(): Flow<List<Campus>> = campuses.asStateFlow()

    override suspend fun refresh(): AppResult<Unit> =
        when (val result = safeApiCall { remote.getCampuses().map { it.toDomain() } }) {
            is AppResult.Success -> {
                campuses.value = result.data
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }
}
