package com.afternote.core.network.di

import com.afternote.core.network.interceptor.ImageMockInterceptor
import com.afternote.core.network.interceptor.OptionalDebugNetworkInterceptor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor

@Module
@InstallIn(SingletonComponent::class)
abstract class InterceptorModule {
    @Binds
    @IntoSet
    @OptionalDebugNetworkInterceptor
    abstract fun bindImageMockInterceptor(interceptor: ImageMockInterceptor): Interceptor
}
