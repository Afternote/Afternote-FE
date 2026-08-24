package com.afternote.core.datastore.di

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
internal object TokenDataStoreModule {
    @Provides
    @Singleton
    @TokenDataStore
    fun provideTokenDataStore(registry: LocalStoreRegistry): DataStore<Preferences> =
        // name 은 저장 파일명 계약 — 바꾸면 기존 사용자 로그인이 풀린다 (#912 필수 주의).
        registry.store(name = "Token", scope = StoreScope.SESSION)
}
