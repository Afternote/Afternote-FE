package com.afternote.core.domain.repository.push

import com.afternote.core.domain.push.PushPermission
import com.afternote.core.domain.push.PushRegistration

/**
 * 이 기기의 푸시 대상 식별자([com.afternote.core.domain.push.DevicePushTargetProvider])를
 * 서버에 알리고 지우는 계약 (#1493).
 *
 * 서버는 등록된 식별자로만 푸시를 보낸다. 등록이 없으면 알림 권한이 있든 없든 푸시는
 * 기기에 **도달조차 하지 않는다** — 수신부(#779)와 진입 인프라(#1310)가 다 갖춰져도 마찬가지다.
 *
 * [installationId] 는 앱 설치마다 한 번 만든 UUID 다 (#2321). 대상 식별자가 회전해도 그대로라
 * 서버가 같은 설치의 권한 보고와 토큰을 묶는다.
 *
 * 모든 연산을 서버가 멱등으로 처리하므로 재호출이 안전하다.
 */
interface PushTargetRepository {
    /** 로그인 확정·식별자 회전·권한 변경 시 등록한다. 같은 값 재등록은 갱신(upsert)이다. */
    suspend fun register(
        targetId: String,
        installationId: String,
        permission: PushPermission,
    ): Result<PushRegistration>

    /** 로그아웃 시 해제한다. 서버에 없는 값이어도 성공이다. */
    suspend fun unregister(targetId: String): Result<Unit>

    /**
     * 이 설치의 OS 알림 권한을 보고한다 (#2321). 대상 식별자가 아직 없어도 부를 수 있다 —
     * FCM 발급에 실패한 기기라도 서버가 「권한 거부」 를 알아야 대체 채널로 돌린다.
     */
    suspend fun reportPermission(
        installationId: String,
        permission: PushPermission,
    ): Result<Unit>
}
