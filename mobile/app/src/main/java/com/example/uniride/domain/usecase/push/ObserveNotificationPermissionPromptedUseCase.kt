package com.example.uniride.domain.usecase.push

import com.example.uniride.domain.repository.NotificationPermissionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Whether the contextual notification permission rationale (RNF08) has already been shown. */
class ObserveNotificationPermissionPromptedUseCase @Inject constructor(
    private val repository: NotificationPermissionRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.observeHasPrompted()
}
