package com.afternote.feature.receiver.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 수신자 본인 확인(designs 2·3·4) 완료 여부 — 발신자 상세 → 열람 신청 진입 시 분기점.
 *
 * **검증 단위 = 사람 (이 폰의 수신자 본인) 1회**. 발신자별로 분리하지 않는 의도 —
 * 발신자 A 의 본인 확인을 완료했다면 발신자 B 진입 시에도 캐시 hit 으로 Intro 스킵.
 * 같은 폰의 같은 사람이 다른 발신자 흐름에서 다시 본인 확인 할 필요 없다는 설계.
 * 발신자별 isolation 이 필요해지면 캐시 자료구조를 `Map<SenderId, Boolean>` 으로 교체.
 *
 * 상태는 DataStore Preferences 로 영구 보관 — process death · 앱 재시작 후에도 유지.
 * (이전 구현은 `@Singleton` 메모리 캐시만 — 백그라운드 사망 후 복귀 시 Intro 재노출되는 비대칭이 있었음.)
 */
interface IdentityVerificationRepository {
    /** 본인 확인 완료 여부. DataStore 영구 저장이라 process restart 후에도 값 유지. */
    val isVerified: Flow<Boolean>

    /** 본인 확인 완료 표시. DataStore 에 영구 저장. */
    suspend fun markVerified()
}
