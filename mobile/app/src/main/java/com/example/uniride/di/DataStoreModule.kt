package com.example.uniride.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Device-scoped DataStore Preferences that survives logout and 401 session cleanup (for example,
 * whether the contextual `POST_NOTIFICATIONS` prompt was already shown on this device, RNF08).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DevicePreferences

/**
 * DataStore Preferences of the app (non-sensitive preferences).
 * - Unqualified (`uniride_preferences`): user-scoped (feed filters, mode…), cleared on logout/401.
 * - [DevicePreferences] (`uniride_device_preferences`): device-scoped, preserved across logins.
 *
 * The session token does NOT go here: it goes in SessionTokenStore, encrypted (§6.4).
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("uniride_preferences") }

    @Provides
    @Singleton
    @DevicePreferences
    fun provideDevicePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("uniride_device_preferences") }
}
