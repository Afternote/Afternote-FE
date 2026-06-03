package com.afternote.feature.afternote.data.di

import com.afternote.feature.afternote.data.service.AfternoteApiService
import com.afternote.feature.afternote.data.service.MusicApiService
import com.afternote.feature.afternote.data.service.ReceiverAfternoteApiService
import com.afternote.feature.afternote.data.service.ReceiverAuthApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AfternoteServiceModule {
    @Provides
    @Singleton
    fun provideAfternoteApiService(retrofit: Retrofit): AfternoteApiService = retrofit.create<AfternoteApiService>()

    @Provides
    @Singleton
    fun provideMusicApiService(retrofit: Retrofit): MusicApiService = retrofit.create<MusicApiService>()

    @Provides
    @Singleton
    fun provideReceiverAfternoteApiService(retrofit: Retrofit): ReceiverAfternoteApiService = retrofit.create<ReceiverAfternoteApiService>()

    @Provides
    @Singleton
    fun provideReceiverAuthApiService(retrofit: Retrofit): ReceiverAuthApiService = retrofit.create<ReceiverAuthApiService>()
}
