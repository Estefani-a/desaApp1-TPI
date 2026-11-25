package com.example.uniride.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.ZoneId
import javax.inject.Singleton

/**
 * "Now" for the use cases, in the business time zone of the API (docs/API.md §1). The tests
 * pass a fixed clock instead.
 */
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.system(ZoneId.of("America/Argentina/Buenos_Aires"))
}
