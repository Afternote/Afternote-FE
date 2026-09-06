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
internal object ReceiverMasterKeyDataStoreModule {
    @Provides
    @Singleton
    @ReceiverMasterKeyDataStore
    fun provideReceiverMasterKeyDataStore(registry: LocalStoreRegistry): DataStore<Preferences> =
        // name 은 저장 파일명 계약 (#912 필수 주의). 구 SharedPreferences XML 과 같은 이름이지만
        // 자동 마이그레이션하지 않는다는 기존 결정이 있다 — 마이그레이션을 붙이지 말 것.
        registry.store(name = "afternote_receiver_auth", scope = StoreScope.SESSION)
}
