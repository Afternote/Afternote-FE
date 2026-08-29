package com.afternote.core.domain.push

/**
 * 이 기기의 현재 FCM 등록 토큰을 돌려준다 (#1493).
 *
 * 토큰 발급은 Firebase SDK 몫이고 그 의존은 `app` 에만 있다. 코어가 «토큰을 어떻게 얻는가» 를
 * 모른 채 «누구에게 등록하는가» 만 다루도록 이 경계를 둔다.
 */
fun interface DevicePushTokenProvider {
    /** 발급에 실패했거나 Google Play 서비스가 없는 기기면 null. */
    suspend fun currentToken(): String?
}
