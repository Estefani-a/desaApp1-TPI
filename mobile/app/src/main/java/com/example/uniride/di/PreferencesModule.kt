package com.example.uniride.di

import com.example.uniride.data.datastore.AppModeDataStore
import com.example.uniride.domain.repository.AppModeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Preferences of the shell kept in DataStore. */
@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {

    @Binds
    abstract fun bindAppModeRepository(impl: AppModeDataStore): AppModeRepository
}
