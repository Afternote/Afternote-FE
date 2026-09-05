package com.afternote.core.datastore.di

import com.afternote.core.datastore.LocalStoreRegistry
import com.afternote.core.datastore.LocalStoreRegistryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LocalStoreRegistryModule {
    @Binds
    @Singleton
    abstract fun bindLocalStoreRegistry(impl: LocalStoreRegistryImpl): LocalStoreRegistry
}
