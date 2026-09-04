package com.afternote.feature.timeletter.data.di

import android.content.Context
import com.afternote.core.common.di.IoDispatcher
import com.afternote.feature.timeletter.data.api.ReceiverTimeLetterApiService
import com.afternote.feature.timeletter.data.api.TimeLetterApiService
import com.afternote.feature.timeletter.data.repositoryImpl.FileMetadataRepositoryImpl
import com.afternote.feature.timeletter.data.repositoryImpl.ReceiverTimeLetterRepositoryImpl
import com.afternote.feature.timeletter.data.repositoryImpl.TimeLetterRepositoryImpl
import com.afternote.feature.timeletter.data.repositoryImpl.VoiceRecorderRepositoryImpl
import com.afternote.feature.timeletter.domain.repository.FileMetadataRepository
import com.afternote.feature.timeletter.domain.repository.ReceiverTimeLetterRepository
import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
import com.afternote.feature.timeletter.domain.repository.VoiceRecorderRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TimeLetterModule {
    @Provides
    @Singleton
    fun provideTimeLetterApiService(retrofit: Retrofit): TimeLetterApiService = retrofit.create<TimeLetterApiService>()

    @Provides
    @Singleton
    fun provideTimeLetterRepository(timeLetterApiService: TimeLetterApiService): TimeLetterRepository =
        TimeLetterRepositoryImpl(timeLetterApiService)

    @Provides
    @Singleton
    fun provideFileMetadataRepository(
        @ApplicationContext context: Context,
    ): FileMetadataRepository = FileMetadataRepositoryImpl(context)

    @Provides
    @Singleton
    fun provideVoiceRecorderRepository(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): VoiceRecorderRepository = VoiceRecorderRepositoryImpl(context, ioDispatcher)

    @Provides
    @Singleton
    fun provideReceiverTimeLetterApiService(retrofit: Retrofit): ReceiverTimeLetterApiService =
        retrofit.create(ReceiverTimeLetterApiService::class.java)

    @Provides
    @Singleton
    fun provideReceiverTimeLetterRepository(receiverTimeLetterApiService: ReceiverTimeLetterApiService): ReceiverTimeLetterRepository =
        ReceiverTimeLetterRepositoryImpl(receiverTimeLetterApiService)
}
