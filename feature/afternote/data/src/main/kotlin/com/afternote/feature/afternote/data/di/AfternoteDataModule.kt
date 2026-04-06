package com.afternote.feature.afternote.data.di

import com.afternote.feature.afternote.data.repositoryimpl.stub.StubAuthorReceiversDirectoryPort
import com.afternote.feature.afternote.data.repositoryimpl.stub.StubCurrentAuthorUserIdPort
import com.afternote.feature.afternote.data.repositoryimpl.stub.StubDownloadAllReceivedUseCase
import com.afternote.feature.afternote.data.repositoryimpl.stub.StubExportReceivedRepository
import com.afternote.feature.afternote.data.repositoryimpl.stub.StubGetAfterNotesByAuthCodeUseCase
import com.afternote.feature.afternote.data.repositoryimpl.stub.StubGetAfternoteDetailByAuthCodeUseCase
import com.afternote.feature.afternote.data.repositoryimpl.stub.StubLoadMindRecordsByAuthCodePort
import com.afternote.feature.afternote.data.repositoryimpl.stub.StubLoadSenderMessageByAuthCodePort
import com.afternote.feature.afternote.data.repositoryimpl.stub.StubLoadTimeLettersByAuthCodePort
import com.afternote.feature.afternote.data.repositoryimpl.stub.StubReceiverAuthCodeProvider
import com.afternote.feature.afternote.domain.port.AuthorReceiversDirectoryPort
import com.afternote.feature.afternote.domain.port.CurrentAuthorUserIdPort
import com.afternote.feature.afternote.domain.port.ExportReceivedRepository
import com.afternote.feature.afternote.domain.port.LoadMindRecordsByAuthCodePort
import com.afternote.feature.afternote.domain.port.LoadSenderMessageByAuthCodePort
import com.afternote.feature.afternote.domain.port.LoadTimeLettersByAuthCodePort
import com.afternote.feature.afternote.domain.port.ReceiverAuthCodeProvider
import com.afternote.feature.afternote.domain.usecase.receiver.DownloadAllReceivedUseCase
import com.afternote.feature.afternote.domain.usecase.receiver.GetAfterNotesByAuthCodeUseCase
import com.afternote.feature.afternote.domain.usecase.receiver.GetAfternoteDetailByAuthCodeUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds domain ports / receiver use cases to stub [repositoryimpl.stub] implementations until real APIs exist.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AfternoteDataModule {
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

    @Binds
    @Singleton
    abstract fun bindGetAfterNotesByAuthCode(impl: StubGetAfterNotesByAuthCodeUseCase): GetAfterNotesByAuthCodeUseCase

    @Binds
    @Singleton
    abstract fun bindGetAfternoteDetailByAuthCode(impl: StubGetAfternoteDetailByAuthCodeUseCase): GetAfternoteDetailByAuthCodeUseCase

    @Binds
    @Singleton
    abstract fun bindDownloadAllReceived(impl: StubDownloadAllReceivedUseCase): DownloadAllReceivedUseCase
}
