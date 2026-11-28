package com.example.uniride.presentation.push

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniride.R
import com.example.uniride.domain.usecase.push.MarkNotificationPermissionPromptedUseCase
import com.example.uniride.domain.usecase.push.ObserveNotificationPermissionPromptedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Manages the one-time contextual `POST_NOTIFICATIONS` prompt and refusal feedback (RNF08). */
@HiltViewModel
class NotificationPermissionViewModel @Inject constructor(
    observePrompted: ObserveNotificationPermissionPromptedUseCase,
    private val markPrompted: MarkNotificationPermissionPromptedUseCase,
) : ViewModel() {

    val uiState: StateFlow<NotificationPermissionUiState> = observePrompted()
        .map { prompted -> NotificationPermissionUiState(hasPrompted = prompted) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationPermissionUiState())

    private val _effects = Channel<NotificationPermissionEffect>(Channel.BUFFERED)
    val effects: Flow<NotificationPermissionEffect> = _effects.receiveAsFlow()

    fun onAction(action: NotificationPermissionAction) {
        when (action) {
            NotificationPermissionAction.Confirm -> viewModelScope.launch {
                markPrompted()
                _effects.send(NotificationPermissionEffect.LaunchSystemRequest)
            }
            NotificationPermissionAction.Dismiss -> viewModelScope.launch {
                markPrompted()
                _effects.send(NotificationPermissionEffect.ShowMessage(R.string.push_permission_declined))
            }
            is NotificationPermissionAction.SystemResult -> if (!action.granted) {
                viewModelScope.launch {
                    _effects.send(NotificationPermissionEffect.ShowMessage(R.string.push_permission_declined))
                }
            }
        }
    }
}
