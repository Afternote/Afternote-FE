package com.afternote.core.network.di

import com.afternote.core.network.interceptor.OptionalDebugNetworkInterceptor
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import okhttp3.Interceptor

@Module
@InstallIn(SingletonComponent::class)
interface NetworkOptionalInterceptorModule {
    @Multibinds
    @OptionalDebugNetworkInterceptor
    fun optionalDebugInterceptors(): Set<Interceptor>
}
