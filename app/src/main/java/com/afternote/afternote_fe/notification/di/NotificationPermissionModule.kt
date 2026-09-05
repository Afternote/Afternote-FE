package com.afternote.afternote_fe.notification.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.afternote.afternote_fe.notification.DataStoreNotificationPermissionRequestStore
import com.afternote.afternote_fe.notification.NotificationPermissionRequestStore
import com.afternote.core.datastore.LocalStoreRegistry
import com.afternote.core.datastore.StoreScope
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NotificationPermissionDataStore

@Module
@InstallIn(SingletonComponent::class)
internal object NotificationPermissionDataStoreModule {
    @Provides
    @Singleton
    @NotificationPermissionDataStore
    fun provideNotificationPermissionDataStore(registry: LocalStoreRegistry): DataStore<Preferences> =
        // name 은 저장 파일명 계약 — 바꾸면 이미 물어본 사용자에게 다시 묻는다 (#912 필수 주의).
        registry.store(name = "NotificationPermission", scope = StoreScope.DEVICE)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NotificationPermissionStoreModule {
    @Binds
    @Singleton
    abstract fun bindNotificationPermissionRequestStore(
        impl: DataStoreNotificationPermissionRequestStore,
    ): NotificationPermissionRequestStore
}
