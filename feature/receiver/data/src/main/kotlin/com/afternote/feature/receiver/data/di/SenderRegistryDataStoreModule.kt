package com.afternote.feature.receiver.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.afternote.core.datastore.LocalStoreRegistry
import com.afternote.core.datastore.StoreScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SenderRegistryDataStoreModule {
    @Provides
    @Singleton
    @SenderRegistryDataStore
    fun provideSenderRegistryDataStore(registry: LocalStoreRegistry): DataStore<Preferences> =
        registry.store(name = "afternote_sender_registry", scope = StoreScope.SESSION)
}
