package com.example.uniride.di

import android.content.Context
import com.example.uniride.BuildConfig
import com.example.uniride.core.dispatcher.DispatchersProvider
import com.example.uniride.data.connectivity.ConnectivityNetworkMonitor
import com.example.uniride.data.datastore.EncryptedSessionTokenStore
import com.example.uniride.data.datastore.SessionTokenStore
import com.example.uniride.data.remote.api.AuthApi
import com.example.uniride.data.remote.api.ReservationApi
import com.example.uniride.data.remote.api.TripApi
import com.example.uniride.data.remote.datasource.AuthRemoteDataSource
import com.example.uniride.data.remote.datasource.FakeAuthRemoteDataSource
import com.example.uniride.data.remote.datasource.FakeBackendStore
import com.example.uniride.data.remote.datasource.FakeReservationRemoteDataSource
import com.example.uniride.data.remote.datasource.FakeTripRemoteDataSource
import com.example.uniride.data.remote.datasource.ReservationRemoteDataSource
import com.example.uniride.data.remote.datasource.RetrofitAuthRemoteDataSource
import com.example.uniride.data.remote.datasource.RetrofitReservationRemoteDataSource
import com.example.uniride.data.remote.datasource.RetrofitTripRemoteDataSource
import com.example.uniride.data.remote.datasource.TripRemoteDataSource
import com.example.uniride.data.remote.interceptor.AuthInterceptor
import com.example.uniride.domain.repository.NetworkMonitor
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module for the network stack, connectivity monitor and remote data sources (A1, A2, A3).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TIMEOUT_SECONDS = 30L

    @Provides
    @Singleton
    fun provideSessionTokenStore(
        @ApplicationContext context: Context,
        @ApplicationScope appScope: CoroutineScope,
        dispatchers: DispatchersProvider,
    ): SessionTokenStore = EncryptedSessionTokenStore(context, appScope, dispatchers)

    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context,
        @ApplicationScope appScope: CoroutineScope,
    ): NetworkMonitor = ConnectivityNetworkMonitor(context, appScope)

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().build()

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenStore: SessionTokenStore): AuthInterceptor =
        AuthInterceptor(tokenStore)

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)

        if (BuildConfig.DEBUG) {
            // Level.HEADERS avoids leaking passwords or access tokens in /auth/* request/response bodies.
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
                redactHeader("Authorization")
            }
            builder.addInterceptor(logging)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideTripApi(retrofit: Retrofit): TripApi = retrofit.create(TripApi::class.java)

    @Provides
    @Singleton
    fun provideReservationApi(retrofit: Retrofit): ReservationApi =
        retrofit.create(ReservationApi::class.java)

    @Provides
    @Singleton
    fun provideFakeBackendStore(tokenStore: SessionTokenStore): FakeBackendStore =
        FakeBackendStore(tokenProvider = tokenStore::currentToken)

    @Provides
    @Singleton
    fun provideAuthRemoteDataSource(
        real: RetrofitAuthRemoteDataSource,
        fakeStore: FakeBackendStore,
    ): AuthRemoteDataSource =
        if (BuildConfig.USE_FAKE_BACKEND) FakeAuthRemoteDataSource(fakeStore) else real

    @Provides
    @Singleton
    fun provideTripRemoteDataSource(
        real: RetrofitTripRemoteDataSource,
        fakeStore: FakeBackendStore,
    ): TripRemoteDataSource =
        if (BuildConfig.USE_FAKE_BACKEND) {
            // Enough delay to see the loading states without making the demo slow
            FakeTripRemoteDataSource(fakeStore, simulatedDelayMs = 600L)
        } else {
            real
        }

    @Provides
    @Singleton
    fun provideReservationRemoteDataSource(
        real: RetrofitReservationRemoteDataSource,
        fakeStore: FakeBackendStore,
    ): ReservationRemoteDataSource =
        if (BuildConfig.USE_FAKE_BACKEND) FakeReservationRemoteDataSource(fakeStore) else real
}
