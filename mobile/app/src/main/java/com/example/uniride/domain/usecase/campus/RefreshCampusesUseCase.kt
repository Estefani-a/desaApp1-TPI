package com.example.uniride.domain.usecase.campus

import com.example.uniride.core.result.AppResult
import com.example.uniride.domain.repository.CampusRepository
import javax.inject.Inject

/** Brings the campuses from the server. After the first time they are available offline. */
class RefreshCampusesUseCase @Inject constructor(
    private val campusRepository: CampusRepository,
) {
    suspend operator fun invoke(): AppResult<Unit> = campusRepository.refresh()
}
