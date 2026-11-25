package com.example.uniride.di

import android.content.Context
import androidx.room.Room
import com.example.uniride.data.local.dao.CampusDao
import com.example.uniride.data.local.dao.ReservationDao
import com.example.uniride.data.local.dao.TripDao
import com.example.uniride.data.local.dao.UserDao
import com.example.uniride.data.local.database.UniRideDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): UniRideDatabase =
        Room.databaseBuilder(context, UniRideDatabase::class.java, "uniride.db").build()

    @Provides
    fun provideUserDao(database: UniRideDatabase): UserDao = database.userDao()

    @Provides
    fun provideTripDao(database: UniRideDatabase): TripDao = database.tripDao()

    @Provides
    fun provideReservationDao(database: UniRideDatabase): ReservationDao = database.reservationDao()

    @Provides
    fun provideCampusDao(database: UniRideDatabase): CampusDao = database.campusDao()
}
