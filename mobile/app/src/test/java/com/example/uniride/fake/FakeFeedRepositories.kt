package com.example.uniride.fake

import com.example.uniride.core.result.AppResult
import com.example.uniride.domain.model.Campus
import com.example.uniride.domain.model.TripFilters
import com.example.uniride.domain.repository.CampusRepository
import com.example.uniride.domain.repository.FeedFiltersRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [FeedFiltersRepository]: what is saved is what [observeLastFilters] emits. */
class FakeFeedFiltersRepository(initial: TripFilters = TripFilters()) : FeedFiltersRepository {

    val saved = MutableStateFlow(initial)

    override fun observeLastFilters(): Flow<TripFilters> = saved

    override suspend fun saveLastFilters(filters: TripFilters) {
        saved.value = filters
    }
}

/** In-memory [CampusRepository]: [refresh] loads [campusesFromServer]. */
class FakeCampusRepository(
    private val campusesFromServer: List<Campus> = emptyList(),
) : CampusRepository {

    val campuses = MutableStateFlow<List<Campus>>(emptyList())

    override fun observeCampuses(): Flow<List<Campus>> = campuses

    override suspend fun refresh(): AppResult<Unit> {
        campuses.value = campusesFromServer
        return AppResult.Success(Unit)
    }
}
