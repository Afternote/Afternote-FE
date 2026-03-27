package com.afternote.core.network.di

import com.afternote.core.network.service.AuthApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    // 반환 타입 생략하면 오류 남?
    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService? = retrofit.create(AuthApiService::class.java)
}
