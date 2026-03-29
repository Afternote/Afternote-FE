package com.afternote.feature.afternote.presentation.di

import com.afternote.feature.afternote.presentation.author.edit.wiring.AuthorReceiversDirectoryPort
import com.afternote.feature.afternote.presentation.author.edit.wiring.CurrentAuthorUserIdPort
import com.afternote.feature.afternote.presentation.author.edit.wiring.StubAuthorReceiversDirectoryPort
import com.afternote.feature.afternote.presentation.author.edit.wiring.StubCurrentAuthorUserIdPort
import com.afternote.feature.afternote.presentation.receiver.wiring.ExportReceivedRepository
import com.afternote.feature.afternote.presentation.receiver.wiring.LoadMindRecordsByAuthCodePort
import com.afternote.feature.afternote.presentation.receiver.wiring.LoadSenderMessageByAuthCodePort
import com.afternote.feature.afternote.presentation.receiver.wiring.LoadTimeLettersByAuthCodePort
import com.afternote.feature.afternote.presentation.receiver.wiring.ReceiverAuthCodeProvider
import com.afternote.feature.afternote.presentation.receiver.wiring.StubExportReceivedRepository
import com.afternote.feature.afternote.presentation.receiver.wiring.StubLoadMindRecordsByAuthCodePort
import com.afternote.feature.afternote.presentation.receiver.wiring.StubLoadSenderMessageByAuthCodePort
import com.afternote.feature.afternote.presentation.receiver.wiring.StubLoadTimeLettersByAuthCodePort
import com.afternote.feature.afternote.presentation.receiver.wiring.StubReceiverAuthCodeProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AfternotePresentationBindingsModule {
    @Binds
    @Singleton
    abstract fun bindReceiverAuthCodeProvider(impl: StubReceiverAuthCodeProvider): ReceiverAuthCodeProvider

    @Binds
    @Singleton
    abstract fun bindExportReceivedRepository(impl: StubExportReceivedRepository): ExportReceivedRepository

    @Binds
    @Singleton
    abstract fun bindLoadMindRecords(impl: StubLoadMindRecordsByAuthCodePort): LoadMindRecordsByAuthCodePort

    @Binds
    @Singleton
    abstract fun bindLoadTimeLetters(impl: StubLoadTimeLettersByAuthCodePort): LoadTimeLettersByAuthCodePort

    @Binds
    @Singleton
    abstract fun bindLoadSenderMessage(impl: StubLoadSenderMessageByAuthCodePort): LoadSenderMessageByAuthCodePort

    @Binds
    @Singleton
    abstract fun bindCurrentAuthorUserId(impl: StubCurrentAuthorUserIdPort): CurrentAuthorUserIdPort

    @Binds
    @Singleton
    abstract fun bindAuthorReceiversDirectory(impl: StubAuthorReceiversDirectoryPort): AuthorReceiversDirectoryPort
}
