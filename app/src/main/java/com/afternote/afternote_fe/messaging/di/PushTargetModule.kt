package com.afternote.afternote_fe.messaging.di

import com.afternote.afternote_fe.messaging.FirebaseDevicePushTargetProvider
import com.afternote.core.domain.push.DevicePushTargetProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 식별자 발급 수단(Firebase)은 `app` 에만 있으므로 코어 계약의 구현도 여기서 바인딩한다 (#1493).
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class PushTargetModule {
    @Binds
    @Singleton
    abstract fun bindDevicePushTargetProvider(impl: FirebaseDevicePushTargetProvider): DevicePushTargetProvider
}
