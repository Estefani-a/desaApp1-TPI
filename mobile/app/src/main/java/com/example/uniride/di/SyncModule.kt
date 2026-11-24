package com.example.uniride.di

import com.example.uniride.data.sync.SyncHandler
import com.example.uniride.data.sync.SyncScheduler
import com.example.uniride.data.sync.WorkManagerSyncScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import javax.inject.Singleton

/**
 * Offline sync (CONVENCIONES §6.2). [syncHandlers] declares the set of handlers, so it is valid
 * while it is empty. Each area adds its own in its module:
 *
 * ```
 * @Binds @IntoSet
 * abstract fun bindTripSyncHandler(impl: TripSyncHandler): SyncHandler
 * ```
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindSyncScheduler(impl: WorkManagerSyncScheduler): SyncScheduler

    @Multibinds
    abstract fun syncHandlers(): Set<SyncHandler>
}
