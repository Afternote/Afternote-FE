package com.afternote.feature.setting.data.di

import com.afternote.feature.setting.data.NotificationPreferenceRepositoryImpl
import com.afternote.feature.setting.data.SettingAccountRepositoryImpl
import com.afternote.feature.setting.data.SettingNotificationRepositoryImpl
import com.afternote.feature.setting.domain.NotificationPreferenceRepository
import com.afternote.feature.setting.domain.SettingAccountRepository
import com.afternote.feature.setting.domain.SettingNotificationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 설정 계정·알림 저장소 바인딩 (#1429).
 *
 * 두 구현 모두 상태가 없어 unscoped 다 — 매 주입마다 새로 만들어도 서버가 정본이다.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class SettingUserRepositoryModule {
    @Binds
    internal abstract fun bindSettingAccountRepository(impl: SettingAccountRepositoryImpl): SettingAccountRepository

    @Binds
    internal abstract fun bindSettingNotificationRepository(impl: SettingNotificationRepositoryImpl): SettingNotificationRepository
}

/**
 * 서버 알림 설정 저장소 바인딩 (#2321).
 *
 * [SettingUserRepositoryModule] 과 따로 둔다 — 앱 계측 테스트가 그 모듈을 대역으로 통째로 갈아 끼우므로,
 * 거기 함께 두면 대역을 쓰는 테스트 그래프에서 이 바인딩까지 빠진다.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class NotificationPreferenceRepositoryModule {
    @Binds
    internal abstract fun bindNotificationPreferenceRepository(
        impl: NotificationPreferenceRepositoryImpl,
    ): NotificationPreferenceRepository
}
