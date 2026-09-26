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
internal object ReceiverInvitationDataStoreModule {
    @Provides
    @Singleton
    @ReceiverInvitationDataStore
    fun provideReceiverInvitationDataStore(registry: LocalStoreRegistry): DataStore<Preferences> =
        // name 은 저장 파일명 계약 — 바꾸면 보관 중인 초대가 끊긴다 (#912 필수 주의).
        // DEVICE 스코프 — 초대는 계정이 아니라 기기로 들어온 것이라 로그아웃에 지우지 않는다 (#944).
        registry.store(name = "ReceiverInvitation", scope = StoreScope.DEVICE)
}
