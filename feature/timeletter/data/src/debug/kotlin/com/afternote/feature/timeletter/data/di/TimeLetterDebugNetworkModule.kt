package com.afternote.feature.timeletter.data.di

import com.afternote.core.network.interceptor.OptionalDebugNetworkInterceptor
import com.afternote.feature.timeletter.data.network.TimeLetterDebugMockNetworkInterceptor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor

@Module
@InstallIn(SingletonComponent::class)
abstract class TimeLetterDebugNetworkModule {
    @Binds
    @IntoSet
    @OptionalDebugNetworkInterceptor
    abstract fun bindTimeLetterDebugMockNetworkInterceptor(interceptor: TimeLetterDebugMockNetworkInterceptor): Interceptor
}
