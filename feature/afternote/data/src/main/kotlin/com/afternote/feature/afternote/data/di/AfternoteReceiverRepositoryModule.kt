package com.afternote.feature.afternote.data.di

import com.afternote.feature.afternote.data.repositoryimpl.receiver.DefaultAuthorDirectoryRepository
import com.afternote.feature.afternote.data.repositoryimpl.receiver.RealReceiverRepository
import com.afternote.feature.afternote.domain.repository.AuthorDirectoryRepository
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
    abstract fun bindReceiverRepository(impl: RealReceiverRepository): ReceiverRepository

    @Binds
    @Singleton
    abstract fun bindAuthorDirectoryRepository(impl: DefaultAuthorDirectoryRepository): AuthorDirectoryRepository
}
