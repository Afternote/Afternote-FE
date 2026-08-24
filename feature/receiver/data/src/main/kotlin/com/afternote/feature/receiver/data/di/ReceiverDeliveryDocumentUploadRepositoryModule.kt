package com.afternote.feature.receiver.data.di

import com.afternote.feature.receiver.data.repositoryimpl.ReceiverDeliveryDocumentUploadRepositoryImpl
import com.afternote.feature.receiver.domain.repository.ReceiverDeliveryDocumentUploadRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReceiverDeliveryDocumentUploadRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindReceiverDeliveryDocumentUploadRepository(
        impl: ReceiverDeliveryDocumentUploadRepositoryImpl,
    ): ReceiverDeliveryDocumentUploadRepository
}
