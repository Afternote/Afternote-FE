package com.afternote.core.data.di

import com.afternote.core.data.repoimpl.auth.PasskeyRepositoryImpl
import com.afternote.core.domain.repository.auth.PasskeyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CorePasskeyRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindPasskeyRepository(impl: PasskeyRepositoryImpl): PasskeyRepository
}
