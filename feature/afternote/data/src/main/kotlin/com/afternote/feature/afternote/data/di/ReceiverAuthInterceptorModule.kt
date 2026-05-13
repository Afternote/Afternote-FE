package com.afternote.feature.afternote.data.di

import com.afternote.core.network.interceptor.FeatureNetworkInterceptor
import com.afternote.feature.afternote.data.network.ReceiverAuthInterceptor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor

@Module
@InstallIn(SingletonComponent::class)
abstract class ReceiverAuthInterceptorModule {
    @Binds
    @IntoSet
    @FeatureNetworkInterceptor
    abstract fun bindReceiverAuthInterceptor(impl: ReceiverAuthInterceptor): Interceptor
}
