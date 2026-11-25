package com.example.uniride.di

import com.example.uniride.data.repository.AccountRepositoryImpl
import com.example.uniride.data.repository.LocalSessionCleaner
import com.example.uniride.data.repository.PasswordRecoveryRepositoryImpl
import com.example.uniride.data.repository.ProfileRepositoryImpl
import com.example.uniride.data.repository.RoomLocalSessionCleaner
import com.example.uniride.data.repository.SessionRepositoryImpl
import com.example.uniride.domain.repository.AccountRepository
import com.example.uniride.domain.repository.PasswordRecoveryRepository
import com.example.uniride.domain.repository.ProfileRepository
import com.example.uniride.domain.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt bindings for institutional authentication, session and profile repositories (RF01). */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindPasswordRecoveryRepository(
        impl: PasswordRecoveryRepositoryImpl,
    ): PasswordRecoveryRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindLocalSessionCleaner(impl: RoomLocalSessionCleaner): LocalSessionCleaner
}
