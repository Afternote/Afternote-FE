package com.afternote.afternote_fe.messaging.di

import com.afternote.afternote_fe.messaging.FirebaseDevicePushTokenProvider
import com.afternote.core.domain.push.DevicePushTokenProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 토큰 발급 수단(Firebase)은 `app` 에만 있으므로 코어 계약의 구현도 여기서 바인딩한다 (#1493).
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class PushTokenModule {
    @Binds
    @Singleton
    abstract fun bindDevicePushTokenProvider(impl: FirebaseDevicePushTokenProvider): DevicePushTokenProvider
}
