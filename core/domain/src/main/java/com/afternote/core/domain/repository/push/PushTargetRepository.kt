package com.afternote.core.domain.repository.push

/**
 * 이 기기의 푸시 대상 식별자([com.afternote.core.domain.push.DevicePushTargetProvider])를
 * 서버에 알리고 지우는 계약 (#1493).
 *
 * 서버는 등록된 식별자로만 푸시를 보낸다. 등록이 없으면 알림 권한이 있든 없든 푸시는
 * 기기에 **도달조차 하지 않는다** — 수신부(#779)와 진입 인프라(#1310)가 다 갖춰져도 마찬가지다.
 *
 * 두 연산 모두 서버가 멱등으로 처리하므로 재호출이 안전하다.
 */
interface PushTargetRepository {
    /** 로그인 확정·식별자 회전 시 등록한다. 같은 값 재등록은 갱신(upsert)이다. */
    suspend fun register(targetId: String): Result<Unit>

    /** 로그아웃 시 해제한다. 서버에 없는 값이어도 성공이다. */
    suspend fun unregister(targetId: String): Result<Unit>
}
