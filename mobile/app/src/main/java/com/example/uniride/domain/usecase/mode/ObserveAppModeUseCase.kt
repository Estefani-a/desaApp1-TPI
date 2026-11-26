package com.example.uniride.domain.usecase.mode

import com.example.uniride.domain.model.AppMode
import com.example.uniride.domain.repository.AppModeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Mode (passenger or driver) the user chose last with the selector. */
class ObserveAppModeUseCase @Inject constructor(
    private val appModeRepository: AppModeRepository,
) {
    operator fun invoke(): Flow<AppMode> = appModeRepository.observeAppMode()
}
