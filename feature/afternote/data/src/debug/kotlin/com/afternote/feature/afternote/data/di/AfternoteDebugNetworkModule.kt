package com.afternote.feature.afternote.data.di

import com.afternote.core.network.interceptor.OptionalDebugNetworkInterceptor
import com.afternote.feature.afternote.data.network.AfternoteDebugMockNetworkInterceptor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor

@Module
@InstallIn(SingletonComponent::class)
abstract class AfternoteDebugNetworkModule {
    @Binds
    @IntoSet
    @OptionalDebugNetworkInterceptor
    abstract fun bindAfternoteDebugMockNetworkInterceptor(interceptor: AfternoteDebugMockNetworkInterceptor): Interceptor
}
