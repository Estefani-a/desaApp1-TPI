package com.example.uniride.domain.usecase.campus

import com.example.uniride.domain.model.Campus
import com.example.uniride.domain.repository.CampusRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** UADE campuses with their meeting points, for the filters of the feed and the publish form. */
class ObserveCampusesUseCase @Inject constructor(
    private val campusRepository: CampusRepository,
) {
    operator fun invoke(): Flow<List<Campus>> = campusRepository.observeCampuses()
}
