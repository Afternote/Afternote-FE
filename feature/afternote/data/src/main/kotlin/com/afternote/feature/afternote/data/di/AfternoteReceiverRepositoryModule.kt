package com.afternote.feature.afternote.data.di

import com.afternote.feature.afternote.data.repositoryimpl.receiver.AuthorReceiverRepositoryImpl
import com.afternote.feature.afternote.data.repositoryimpl.receiver.ReceiverRepositoryImpl
import com.afternote.feature.afternote.domain.repository.AuthorReceiverRepository
import com.afternote.feature.afternote.domain.repository.ReceiverRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AfternoteReceiverRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindReceiverRepository(impl: ReceiverRepositoryImpl): ReceiverRepository

    @Binds
    @Singleton
    abstract fun bindAuthorReceiverRepository(impl: AuthorReceiverRepositoryImpl): AuthorReceiverRepository
}
