package com.afternote.afternote_fe.messaging

import com.afternote.core.domain.push.DevicePushTokenProvider
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Firebase 가 이 기기에 발급한 FCM 등록 토큰을 돌려준다 (#1493).
 *
 * 실패(Google Play 서비스 부재·네트워크·SERVICE_NOT_AVAILABLE)는 예외로 올리지 않고 null 로 접는다.
 * 부르는 쪽이 로그인·로그아웃 흐름이라, 토큰을 못 얻었다고 그 흐름을 막을 이유가 없다.
 */
@Singleton
class FirebaseDevicePushTokenProvider
    @Inject
    constructor() : DevicePushTokenProvider {
        override suspend fun currentToken(): String? =
            suspendCancellableCoroutine { continuation ->
                FirebaseMessaging
                    .getInstance()
                    .token
                    .addOnSuccessListener { token -> continuation.resume(token) }
                    .addOnFailureListener { continuation.resume(null) }
                    .addOnCanceledListener { continuation.resume(null) }
            }
    }
