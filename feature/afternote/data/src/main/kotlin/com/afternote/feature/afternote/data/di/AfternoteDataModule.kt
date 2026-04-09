package com.afternote.feature.afternote.data.di

import com.afternote.core.common.dev.DevEnvironment
import com.afternote.feature.afternote.data.repositoryimpl.receiver.RealAuthorReceiversDirectoryPort
import com.afternote.feature.afternote.data.repositoryimpl.receiver.RealCurrentAuthorUserIdPort
import com.afternote.feature.afternote.data.repositoryimpl.receiver.RealDownloadAllReceivedUseCase
import com.afternote.feature.afternote.data.repositoryimpl.receiver.RealExportReceivedRepository
import com.afternote.feature.afternote.data.repositoryimpl.receiver.RealGetAfterNotesByAuthCodeUseCase
import com.afternote.feature.afternote.data.repositoryimpl.receiver.RealGetAfternoteDetailByAuthCodeUseCase
import com.afternote.feature.afternote.data.repositoryimpl.receiver.RealLoadMindRecordsByAuthCodePort
import com.afternote.feature.afternote.data.repositoryimpl.receiver.RealLoadSenderMessageByAuthCodePort
import com.afternote.feature.afternote.data.repositoryimpl.receiver.RealLoadTimeLettersByAuthCodePort
import com.afternote.feature.afternote.data.repositoryimpl.receiver.RealReceiverAuthCodeProvider
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Receiver-related ports / use cases: [DevEnvironment.USE_FAKE_DATA] chooses stub vs real binding.
 * Real types currently delegate to stubs until HTTP wiring replaces them.
 */
@Module
@InstallIn(SingletonComponent::class)
object AfternoteDataModule {
    @Provides
    @Singleton
    fun provideReceiverAuthCodeProvider(
        stub: StubReceiverAuthCodeProvider,
        real: RealReceiverAuthCodeProvider,
    ): ReceiverAuthCodeProvider = if (DevEnvironment.USE_FAKE_DATA) stub else real

    @Provides
    @Singleton
    fun provideExportReceivedRepository(
        stub: StubExportReceivedRepository,
        real: RealExportReceivedRepository,
    ): ExportReceivedRepository = if (DevEnvironment.USE_FAKE_DATA) stub else real

    @Provides
    @Singleton
    fun provideLoadMindRecords(
        stub: StubLoadMindRecordsByAuthCodePort,
        real: RealLoadMindRecordsByAuthCodePort,
    ): LoadMindRecordsByAuthCodePort = if (DevEnvironment.USE_FAKE_DATA) stub else real

    @Provides
    @Singleton
    fun provideLoadTimeLetters(
        stub: StubLoadTimeLettersByAuthCodePort,
        real: RealLoadTimeLettersByAuthCodePort,
    ): LoadTimeLettersByAuthCodePort = if (DevEnvironment.USE_FAKE_DATA) stub else real

    @Provides
    @Singleton
    fun provideLoadSenderMessage(
        stub: StubLoadSenderMessageByAuthCodePort,
        real: RealLoadSenderMessageByAuthCodePort,
    ): LoadSenderMessageByAuthCodePort = if (DevEnvironment.USE_FAKE_DATA) stub else real

    @Provides
    @Singleton
    fun provideCurrentAuthorUserId(
        stub: StubCurrentAuthorUserIdPort,
        real: RealCurrentAuthorUserIdPort,
    ): CurrentAuthorUserIdPort = if (DevEnvironment.USE_FAKE_DATA) stub else real

    @Provides
    @Singleton
    fun provideAuthorReceiversDirectory(
        stub: StubAuthorReceiversDirectoryPort,
        real: RealAuthorReceiversDirectoryPort,
    ): AuthorReceiversDirectoryPort = if (DevEnvironment.USE_FAKE_DATA) stub else real

    @Provides
    @Singleton
    fun provideGetAfterNotesByAuthCode(
        stub: StubGetAfterNotesByAuthCodeUseCase,
        real: RealGetAfterNotesByAuthCodeUseCase,
    ): GetAfterNotesByAuthCodeUseCase = if (DevEnvironment.USE_FAKE_DATA) stub else real

    @Provides
    @Singleton
    fun provideGetAfternoteDetailByAuthCode(
        stub: StubGetAfternoteDetailByAuthCodeUseCase,
        real: RealGetAfternoteDetailByAuthCodeUseCase,
    ): GetAfternoteDetailByAuthCodeUseCase = if (DevEnvironment.USE_FAKE_DATA) stub else real

    @Provides
    @Singleton
    fun provideDownloadAllReceived(
        stub: StubDownloadAllReceivedUseCase,
        real: RealDownloadAllReceivedUseCase,
    ): DownloadAllReceivedUseCase = if (DevEnvironment.USE_FAKE_DATA) stub else real
}
