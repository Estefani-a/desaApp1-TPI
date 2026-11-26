package com.example.uniride.di

import com.example.uniride.data.repository.ReservationRepositoryImpl
import com.example.uniride.data.sync.ReservationSyncHandler
import com.example.uniride.data.sync.SyncHandler
import com.example.uniride.domain.repository.ReservationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** Seat requests (RF04). */
@Module
@InstallIn(SingletonComponent::class)
abstract class ReservationModule {

    @Binds
    abstract fun bindReservationRepository(impl: ReservationRepositoryImpl): ReservationRepository

    @Binds
    @IntoSet
    abstract fun bindReservationSyncHandler(impl: ReservationSyncHandler): SyncHandler
}
