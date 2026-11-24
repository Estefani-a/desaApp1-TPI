package com.example.uniride.di

import com.example.uniride.data.datastore.FeedFiltersDataStore
import com.example.uniride.data.remote.datasource.FakeBackendStore
import com.example.uniride.data.remote.datasource.FakeTripRemoteDataSource
import com.example.uniride.data.remote.datasource.TripRemoteDataSource
import com.example.uniride.data.repository.InMemoryCampusRepository
import com.example.uniride.data.repository.InMemoryTripRepository
import com.example.uniride.domain.repository.CampusRepository
import com.example.uniride.domain.repository.FeedFiltersRepository
import com.example.uniride.domain.repository.TripRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Trips and campuses (RF02 and RF03). */
@Module
@InstallIn(SingletonComponent::class)
abstract class TripModule {

    /** Provisional until Room (E1): see [InMemoryTripRepository]. */
    @Binds
    abstract fun bindTripRepository(impl: InMemoryTripRepository): TripRepository

    @Binds
    abstract fun bindCampusRepository(impl: InMemoryCampusRepository): CampusRepository

    @Binds
    abstract fun bindFeedFiltersRepository(impl: FeedFiltersDataStore): FeedFiltersRepository

    companion object {

        /**
         * Provisional until A1 (`NetworkModule` and the `USE_FAKE_BACKEND` flag): the app talks to
         * the in-memory fake backend of A2, already logged in as the demo passenger because the
         * login of RF01 is not merged yet.
         */
        @Provides
        @Singleton
        fun provideFakeBackendStore(): FakeBackendStore =
            FakeBackendStore().apply { currentUserId = FakeBackendStore.DEFAULT_PASSENGER_ID }

        @Provides
        @Singleton
        fun provideTripRemoteDataSource(store: FakeBackendStore): TripRemoteDataSource =
            FakeTripRemoteDataSource(store, simulatedDelayMs = FAKE_DELAY_MS)

        /** Enough to see the loading states without making the demo slow. */
        private const val FAKE_DELAY_MS = 600L
    }
}
