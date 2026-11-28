package com.example.uniride.di

import com.example.uniride.data.datastore.NotificationPermissionDataStore
import com.example.uniride.data.push.AndroidPushNotifier
import com.example.uniride.data.push.DeviceTokenSyncScheduler
import com.example.uniride.data.push.FcmTokenProvider
import com.example.uniride.data.push.FirebaseFcmTokenProvider
import com.example.uniride.data.push.PushNotifier
import com.example.uniride.data.push.PushTapIntentFactory
import com.example.uniride.data.push.WorkManagerDeviceTokenSyncScheduler
import com.example.uniride.domain.repository.NotificationPermissionRepository
import com.example.uniride.presentation.navigation.ActivityPushTapIntentFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt bindings for FCM push notifications and contextual permission state (RF04, RNF08). */
@Module
@InstallIn(SingletonComponent::class)
abstract class PushModule {

    @Binds
    @Singleton
    abstract fun bindFcmTokenProvider(impl: FirebaseFcmTokenProvider): FcmTokenProvider

    @Binds
    @Singleton
    abstract fun bindPushNotifier(impl: AndroidPushNotifier): PushNotifier

    @Binds
    @Singleton
    abstract fun bindPushTapIntentFactory(impl: ActivityPushTapIntentFactory): PushTapIntentFactory

    @Binds
    @Singleton
    abstract fun bindDeviceTokenSyncScheduler(
        impl: WorkManagerDeviceTokenSyncScheduler,
    ): DeviceTokenSyncScheduler

    @Binds
    @Singleton
    abstract fun bindNotificationPermissionRepository(
        impl: NotificationPermissionDataStore,
    ): NotificationPermissionRepository
}
