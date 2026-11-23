package com.example.uniride.di

import android.content.Context
import com.example.uniride.data.connectivity.ConnectivityNetworkMonitor
import com.example.uniride.domain.repository.NetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** [NetworkMonitor] of A3, for the "Sin conexión" banner of every screen. */
@Module
@InstallIn(SingletonComponent::class)
object ConnectivityModule {

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor =
        ConnectivityNetworkMonitor(context)
}
