package com.afternote.core.domain.repository.push

/**
 * 이 기기의 FCM 토큰을 서버에 알리고 지우는 계약 (#1493).
 *
 * 서버는 등록된 토큰으로만 푸시를 보낸다. 등록이 없으면 알림 권한이 있든 없든 푸시는
 * 기기에 **도달조차 하지 않는다** — 수신부(#779)와 진입 인프라(#1310)가 다 갖춰져도 마찬가지다.
 *
 * 두 연산 모두 서버가 멱등으로 처리하므로 재호출이 안전하다.
 */
interface PushTokenRepository {
    /** 로그인 확정·토큰 회전 시 등록한다. 같은 토큰 재등록은 갱신(upsert)이다. */
    suspend fun register(token: String): Result<Unit>

    /** 로그아웃 시 해제한다. 서버에 없는 토큰이어도 성공이다. */
    suspend fun unregister(token: String): Result<Unit>
}
