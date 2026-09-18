package com.afternote.core.data.di

import com.afternote.core.data.repoimpl.invitation.PendingReceiverInvitationStoreImpl
import com.afternote.core.data.repoimpl.invitation.ReceiverInvitationRepositoryImpl
import com.afternote.core.domain.repository.PendingReceiverInvitationStore
import com.afternote.core.domain.repository.ReceiverInvitationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 카카오톡 수신자 초대 계약 바인딩 (#944). */
@Module
@InstallIn(SingletonComponent::class)
internal interface CoreReceiverInvitationModule {
    @Binds
    fun bindReceiverInvitationRepository(impl: ReceiverInvitationRepositoryImpl): ReceiverInvitationRepository

    @Binds
    @Singleton
    fun bindPendingReceiverInvitationStore(impl: PendingReceiverInvitationStoreImpl): PendingReceiverInvitationStore
}
