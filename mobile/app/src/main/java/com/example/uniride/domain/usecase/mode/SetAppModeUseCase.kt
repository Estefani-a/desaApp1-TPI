package com.example.uniride.domain.usecase.mode

import com.example.uniride.domain.model.AppMode
import com.example.uniride.domain.repository.AppModeRepository
import javax.inject.Inject

/** Keeps the mode chosen with the selector for the next time the app opens. */
class SetAppModeUseCase @Inject constructor(
    private val appModeRepository: AppModeRepository,
) {
    suspend operator fun invoke(mode: AppMode) = appModeRepository.setAppMode(mode)
}
