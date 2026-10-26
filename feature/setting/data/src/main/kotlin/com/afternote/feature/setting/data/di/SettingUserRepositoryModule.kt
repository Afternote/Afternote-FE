package com.afternote.feature.setting.data.di

import com.afternote.feature.setting.data.SettingAccountRepositoryImpl
import com.afternote.feature.setting.data.SettingNotificationRepositoryImpl
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
