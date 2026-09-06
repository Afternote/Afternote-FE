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
        // 파일명·SESSION 수명 계약은 유지한다. 과거의 동적 boolean 키는 도달 불가능한 임시 ID용이라
        // 무시하고, 이제 재기동 뒤에도 안정적인 senderId를 아래 저장소의 새 단일 집합에 기록한다.
        registry.store(name = "afternote_identity_verification", scope = StoreScope.SESSION)
}
