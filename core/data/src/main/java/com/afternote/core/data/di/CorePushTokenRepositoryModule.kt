package com.afternote.core.data.di

import com.afternote.core.data.repoimpl.push.PushTokenRepositoryImpl
import com.afternote.core.domain.repository.push.PushTokenRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CorePushTokenRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindPushTokenRepository(impl: PushTokenRepositoryImpl): PushTokenRepository
}
