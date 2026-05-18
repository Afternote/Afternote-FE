package com.afternote.feature.afternote.data.di

import com.afternote.feature.afternote.data.repositoryimpl.receiver.ReceiverDeliveryDocumentUploadRepositoryImpl
import com.afternote.feature.afternote.domain.repository.receiver.ReceiverDeliveryDocumentUploadRepository
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
