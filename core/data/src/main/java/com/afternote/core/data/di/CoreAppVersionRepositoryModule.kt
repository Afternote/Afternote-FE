package com.afternote.core.data.di

import com.afternote.core.data.repoimpl.appversion.AppVersionRepositoryImpl
import com.afternote.core.domain.repository.appversion.AppVersionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CoreAppVersionRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAppVersionRepository(impl: AppVersionRepositoryImpl): AppVersionRepository
}
