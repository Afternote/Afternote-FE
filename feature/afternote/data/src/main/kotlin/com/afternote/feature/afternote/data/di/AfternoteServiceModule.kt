package com.afternote.feature.afternote.data.di

import com.afternote.feature.afternote.data.service.AfternoteApiService
import com.afternote.feature.afternote.data.service.MusicApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AfternoteServiceModule {
    @Provides
    @Singleton
    fun provideAfternoteApiService(retrofit: Retrofit): AfternoteApiService = retrofit.create(AfternoteApiService::class.java)

    @Provides
    @Singleton
    fun provideMusicApiService(retrofit: Retrofit): MusicApiService = retrofit.create(MusicApiService::class.java)
}
