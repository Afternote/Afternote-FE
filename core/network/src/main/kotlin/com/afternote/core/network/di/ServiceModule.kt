package com.afternote.core.network.di

import com.afternote.core.network.BuildConfig
import com.afternote.core.network.service.AuthApiService
import com.afternote.core.network.service.ImageApiService
import com.afternote.core.network.service.TokenApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    // 반환 타입 생략하면 오류 남?
    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService = retrofit.create(AuthApiService::class.java)

    @Provides
    @Singleton
    fun provideTokenApiService(
        @Named("RefreshClient") refreshClient: OkHttpClient,
        json: Json,
    ): TokenApiService =
        Retrofit
            .Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(refreshClient)
            .addConverterFactory(json.asConverterFactory(contentType = "application/json".toMediaType()))
            .build()
            .create(TokenApiService::class.java)

    @Provides
    @Singleton
    fun provideImageApiService(retrofit: Retrofit): ImageApiService = retrofit.create(ImageApiService::class.java)
}
