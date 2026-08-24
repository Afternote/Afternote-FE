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
internal object IdentityVerificationDataStoreModule {
    @Provides
    @Singleton
    @IdentityVerificationDataStore
    fun provideIdentityVerificationDataStore(registry: LocalStoreRegistry): DataStore<Preferences> =
        // name 은 저장 파일명 계약 — 바꾸면 기존 사용자의 본인인증 상태가 끊긴다 (#912 필수 주의).
        // SESSION 인 이유 — 인증 기록이 이 로컬 boolean 뿐이라 계정 전환을 감지하지 못하므로,
        // 로그아웃 시 지워서 다음 계정이 이전 사용자의 본인인증 완료 상태를 승계하지 못하게
        // 한다. 같은 사람의 재로그인 시 재인증은 감수한다 (#912).
        registry.store(name = "afternote_identity_verification", scope = StoreScope.SESSION)
}
