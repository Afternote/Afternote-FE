package com.afternote.afternote_fe.messaging.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.afternote.afternote_fe.messaging.DataStorePushInstallationIdStore
import com.afternote.afternote_fe.messaging.FirebaseDevicePushTargetProvider
import com.afternote.afternote_fe.messaging.PushInstallationIdStore
import com.afternote.afternote_fe.messaging.PushPermissionReader
import com.afternote.afternote_fe.messaging.SystemPushPermissionReader
import com.afternote.core.datastore.LocalStoreRegistry
import com.afternote.core.datastore.StoreScope
import com.afternote.core.domain.push.DevicePushTargetProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PushInstallationDataStore

/**
 * 식별자 발급 수단(Firebase)은 `app` 에만 있으므로 코어 계약의 구현도 여기서 바인딩한다 (#1493).
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class PushTargetModule {
    @Binds
    @Singleton
    abstract fun bindDevicePushTargetProvider(impl: FirebaseDevicePushTargetProvider): DevicePushTargetProvider

    @Binds
    @Singleton
    abstract fun bindPushInstallationIdStore(impl: DataStorePushInstallationIdStore): PushInstallationIdStore

    @Binds
    @Singleton
    abstract fun bindPushPermissionReader(impl: SystemPushPermissionReader): PushPermissionReader
}

@Module
@InstallIn(SingletonComponent::class)
internal object PushInstallationDataStoreModule {
    @Provides
    @Singleton
    @PushInstallationDataStore
    fun providePushInstallationDataStore(registry: LocalStoreRegistry): DataStore<Preferences> =
        // name 은 저장 파일명 계약 — 바꾸면 같은 기기가 서버에 새 설치로 쌓인다 (#912 필수 주의).
        registry.store(name = "PushInstallation", scope = StoreScope.DEVICE)
}
