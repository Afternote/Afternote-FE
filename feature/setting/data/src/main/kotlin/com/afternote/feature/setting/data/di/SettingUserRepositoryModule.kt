package com.afternote.feature.setting.data.di

import com.afternote.feature.setting.data.SettingAccountRepositoryImpl
import com.afternote.feature.setting.data.SettingNotificationRepositoryImpl
import com.afternote.feature.setting.domain.SettingAccountRepository
import com.afternote.feature.setting.domain.SettingNotificationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
public abstract class SettingUserRepositoryModule {
    @Binds
    internal abstract fun bindAccount(impl: SettingAccountRepositoryImpl): SettingAccountRepository

    @Binds
    internal abstract fun bindNotification(impl: SettingNotificationRepositoryImpl): SettingNotificationRepository
}
