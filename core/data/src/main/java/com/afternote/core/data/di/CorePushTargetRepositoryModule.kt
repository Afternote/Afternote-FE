package com.afternote.core.data.di

import com.afternote.core.data.repoimpl.push.PushTargetRepositoryImpl
import com.afternote.core.domain.repository.push.PushTargetRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CorePushTargetRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindPushTargetRepository(impl: PushTargetRepositoryImpl): PushTargetRepository
}
