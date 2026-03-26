package com.afternote.core.data.di

import com.afternote.core.data.AuthRepositoryImpl
import com.afternote.core.data.UserSessionRepositoryImpl
import com.afternote.core.domain.repository.AuthRepository
import com.afternote.core.domain.usecase.UserSessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Suppress("unused")
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Suppress("unused")
    @Binds
    @Singleton
    abstract fun bindUserSessionRepository(impl: UserSessionRepositoryImpl): UserSessionRepository
}
