package com.afternote.feature.receiver.data.di

import com.afternote.feature.receiver.data.repositoryimpl.IdentityVerificationRepositoryImpl
import com.afternote.feature.receiver.data.repositoryimpl.ReceiverRepositoryImpl
import com.afternote.feature.receiver.domain.repository.IdentityVerificationRepository
import com.afternote.feature.receiver.domain.repository.ReceiverRepository
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
    abstract fun bindIdentityVerificationRepository(impl: IdentityVerificationRepositoryImpl): IdentityVerificationRepository
}
