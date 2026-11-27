package com.example.uniride.presentation.push

import app.cash.turbine.test
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
    fun `emite false inicialmente y true despues de marcar el aviso como mostrado`() = runTest {
        val repository = NotificationPermissionDataStore(InMemoryPreferencesDataStore())
        val viewModel = NotificationPermissionViewModel(
            observePrompted = ObserveNotificationPermissionPromptedUseCase(repository),
            markPrompted = MarkNotificationPermissionPromptedUseCase(repository),
        )

        viewModel.hasPrompted.test {
            assertEquals(false, awaitItem())
            viewModel.onPromptHandled()
            assertEquals(true, awaitItem())
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

        viewModel.hasPrompted.test {
            assertEquals(true, awaitItem())
        }
    }
}
