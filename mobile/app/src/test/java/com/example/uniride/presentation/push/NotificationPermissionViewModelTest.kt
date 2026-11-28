package com.example.uniride.presentation.push

import app.cash.turbine.test
import com.example.uniride.R
import com.example.uniride.data.datastore.NotificationPermissionDataStore
import com.example.uniride.domain.usecase.push.MarkNotificationPermissionPromptedUseCase
import com.example.uniride.domain.usecase.push.ObserveNotificationPermissionPromptedUseCase
import com.example.uniride.fake.InMemoryPreferencesDataStore
import com.example.uniride.fake.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NotificationPermissionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `confirmar marca el aviso como mostrado y solicita el permiso del sistema`() = runTest {
        val repository = NotificationPermissionDataStore(InMemoryPreferencesDataStore())
        val viewModel = NotificationPermissionViewModel(
            observePrompted = ObserveNotificationPermissionPromptedUseCase(repository),
            markPrompted = MarkNotificationPermissionPromptedUseCase(repository),
        )

        viewModel.uiState.test {
            assertEquals(false, awaitItem().hasPrompted)
            viewModel.effects.test {
                viewModel.onAction(NotificationPermissionAction.Confirm)
                assertEquals(NotificationPermissionEffect.LaunchSystemRequest, awaitItem())
            }
            assertEquals(true, awaitItem().hasPrompted)
        }
    }

    @Test
    fun `descartar el aviso marca como mostrado y comunica que no llegaran push`() = runTest {
        val repository = NotificationPermissionDataStore(InMemoryPreferencesDataStore())
        val viewModel = NotificationPermissionViewModel(
            observePrompted = ObserveNotificationPermissionPromptedUseCase(repository),
            markPrompted = MarkNotificationPermissionPromptedUseCase(repository),
        )

        viewModel.uiState.test {
            assertEquals(false, awaitItem().hasPrompted)
            viewModel.effects.test {
                viewModel.onAction(NotificationPermissionAction.Dismiss)
                assertEquals(
                    NotificationPermissionEffect.ShowMessage(R.string.push_permission_declined),
                    awaitItem(),
                )
            }
            assertEquals(true, awaitItem().hasPrompted)
        }
    }

    @Test
    fun `rechazar en el dialogo del sistema comunica que el estado sigue visible en la app`() = runTest {
        val repository = NotificationPermissionDataStore(InMemoryPreferencesDataStore())
        val viewModel = NotificationPermissionViewModel(
            observePrompted = ObserveNotificationPermissionPromptedUseCase(repository),
            markPrompted = MarkNotificationPermissionPromptedUseCase(repository),
        )

        viewModel.effects.test {
            viewModel.onAction(NotificationPermissionAction.SystemResult(granted = false))
            assertEquals(
                NotificationPermissionEffect.ShowMessage(R.string.push_permission_declined),
                awaitItem(),
            )
        }
    }

    @Test
    fun `conceder el permiso en el dialogo del sistema no muestra aviso de rechazo`() = runTest {
        val repository = NotificationPermissionDataStore(InMemoryPreferencesDataStore())
        val viewModel = NotificationPermissionViewModel(
            observePrompted = ObserveNotificationPermissionPromptedUseCase(repository),
            markPrompted = MarkNotificationPermissionPromptedUseCase(repository),
        )

        viewModel.effects.test {
            viewModel.onAction(NotificationPermissionAction.SystemResult(granted = true))
            expectNoEvents()
        }
    }

    @Test
    fun `recuerda que el aviso ya fue mostrado en una nueva instancia`() = runTest {
        val dataStore = InMemoryPreferencesDataStore()
        val repository = NotificationPermissionDataStore(dataStore)
        repository.markPrompted()

        val viewModel = NotificationPermissionViewModel(
            observePrompted = ObserveNotificationPermissionPromptedUseCase(repository),
            markPrompted = MarkNotificationPermissionPromptedUseCase(repository),
        )

        viewModel.uiState.test {
            assertEquals(true, awaitItem().hasPrompted)
        }
    }
}
