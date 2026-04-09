package com.afternote.feature.afternote.data.di

import com.afternote.core.network.interceptor.OptionalDebugNetworkInterceptor
import com.afternote.feature.afternote.data.network.AfternoteDebugMockNetworkInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AfternoteDebugNetworkModule {
    @Provides
    @IntoSet
    @OptionalDebugNetworkInterceptor
    @Singleton
    fun provideAfternoteDebugMockNetworkInterceptor(): Interceptor = AfternoteDebugMockNetworkInterceptor()
}
