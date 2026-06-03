package com.afternote.feature.afternote.data.di

import com.afternote.feature.afternote.data.repositoryimpl.receiver.ReceiverAuthRepositoryImpl
import com.afternote.feature.receiver.domain.repository.ReceiverAuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReceiverAuthRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindReceiverAuthRepository(impl: ReceiverAuthRepositoryImpl): ReceiverAuthRepository
}
