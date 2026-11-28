package com.example.uniride.domain.usecase.push

import com.example.uniride.domain.repository.NotificationPermissionRepository
import javax.inject.Inject

/** Marks the contextual notification permission prompt (RNF08) as handled so it is not repeated. */
class MarkNotificationPermissionPromptedUseCase @Inject constructor(
    private val repository: NotificationPermissionRepository,
) {
    suspend operator fun invoke() = repository.markPrompted()
}
