package com.afternote.feature.receiver.data.di

import com.afternote.feature.receiver.data.service.ReceiverAfternoteApiService
import com.afternote.feature.receiver.data.service.ReceiverAuthApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ReceiverServiceModule {
    @Provides
    @Singleton
    fun provideReceiverAfternoteApiService(retrofit: Retrofit): ReceiverAfternoteApiService = retrofit.create<ReceiverAfternoteApiService>()

    @Provides
    @Singleton
    fun provideReceiverAuthApiService(retrofit: Retrofit): ReceiverAuthApiService = retrofit.create<ReceiverAuthApiService>()
}
