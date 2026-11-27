package com.example.uniride.di

import com.example.uniride.data.datastore.NotificationPermissionDataStore
import com.example.uniride.data.push.AndroidPushNotifier
import com.example.uniride.data.push.FcmTokenProvider
import com.example.uniride.data.push.FirebaseFcmTokenProvider
import com.example.uniride.data.push.PushNotifier
import com.example.uniride.domain.repository.NotificationPermissionRepository
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
    abstract fun bindNotificationPermissionRepository(
        impl: NotificationPermissionDataStore,
    ): NotificationPermissionRepository
}
