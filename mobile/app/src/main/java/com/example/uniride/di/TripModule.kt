package com.example.uniride.di

import com.example.uniride.data.datastore.FeedFiltersDataStore
import com.example.uniride.data.repository.InMemoryCampusRepository
import com.example.uniride.data.repository.TripRepositoryImpl
import com.example.uniride.domain.repository.CampusRepository
import com.example.uniride.domain.repository.FeedFiltersRepository
import com.example.uniride.domain.repository.TripRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Trips and campuses (RF02 and RF03). */
@Module
@InstallIn(SingletonComponent::class)
abstract class TripModule {

    @Binds
    abstract fun bindTripRepository(impl: TripRepositoryImpl): TripRepository

    @Binds
    abstract fun bindCampusRepository(impl: InMemoryCampusRepository): CampusRepository

    @Binds
    abstract fun bindFeedFiltersRepository(impl: FeedFiltersDataStore): FeedFiltersRepository
}
