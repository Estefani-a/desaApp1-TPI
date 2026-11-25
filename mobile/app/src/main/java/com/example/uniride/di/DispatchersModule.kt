package com.example.uniride.di

import com.example.uniride.core.dispatcher.DefaultDispatchersProvider
import com.example.uniride.core.dispatcher.DispatchersProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DispatchersModule {

    @Binds
    @Singleton
    abstract fun bindDispatchersProvider(impl: DefaultDispatchersProvider): DispatchersProvider
}
