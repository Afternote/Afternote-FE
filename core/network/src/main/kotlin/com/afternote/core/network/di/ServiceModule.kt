package com.afternote.core.network.di

import com.afternote.core.network.BuildConfig
import com.afternote.core.network.calladapter.ApiErrorCallAdapterFactory
import com.afternote.core.network.service.AccountApiService
import com.afternote.core.network.service.AppVersionApiService
import com.afternote.core.network.service.AuthApiService
import com.afternote.core.network.service.ImageApiService
import com.afternote.core.network.service.PushTokenApiService
import com.afternote.core.network.service.TokenApiService
import com.afternote.core.network.service.UserApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    // 반환 타입 생략하면 오류 남?
    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService = retrofit.create<AuthApiService>()

    @Provides
    @Singleton
    @Named("RefreshRetrofit")
    fun provideRefreshRetrofit(
        @Named("RefreshClient") refreshClient: OkHttpClient,
        json: Json,
        apiErrorCallAdapterFactory: ApiErrorCallAdapterFactory,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(refreshClient)
            .addCallAdapterFactory(apiErrorCallAdapterFactory)
            .addConverterFactory(json.asConverterFactory(contentType = "application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideTokenApiService(
        @Named("RefreshRetrofit") retrofit: Retrofit,
    ): TokenApiService = retrofit.create<TokenApiService>()

    @Provides
    @Singleton
    fun provideAccountApiService(retrofit: Retrofit): AccountApiService = retrofit.create<AccountApiService>()

    @Provides
    @Singleton
    fun provideAppVersionApiService(retrofit: Retrofit): AppVersionApiService = retrofit.create<AppVersionApiService>()

    @Provides
    @Singleton
    fun provideUserApiService(retrofit: Retrofit): UserApiService = retrofit.create<UserApiService>()

    @Provides
    @Singleton
    fun providePushTokenApiService(retrofit: Retrofit): PushTokenApiService = retrofit.create<PushTokenApiService>()

    @Provides
    @Singleton
    fun provideImageApiService(retrofit: Retrofit): ImageApiService = retrofit.create<ImageApiService>()
}
