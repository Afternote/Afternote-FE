package com.afternote.feature.timeletter.data.di

import com.afternote.feature.timeletter.data.api.TimeLetterApiService
import com.afternote.feature.timeletter.data.repositoryImpl.TimeLetterRepositoryImpl
import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TimeLetterModule {
    @Provides
    @Singleton
    fun provideTimeLetterApiService(retrofit: Retrofit): TimeLetterApiService = retrofit.create(TimeLetterApiService::class.java)

    @Provides
    @Singleton
    fun provideTimeLetterRepository(timeLetterApiService: TimeLetterApiService): TimeLetterRepository =
        TimeLetterRepositoryImpl(timeLetterApiService)
}
