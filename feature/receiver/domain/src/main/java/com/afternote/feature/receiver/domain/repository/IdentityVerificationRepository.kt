package com.afternote.feature.receiver.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 수신자 본인 확인(designs 2·3·4) 완료 여부 — 발신자 상세 → 열람 신청 진입 시 분기점.
 *
 * **검증 단위 = 발신자별 1회** (#597). 이메일 인증은 "이 발신자가 지정한 수신자 이메일" 검증이라
 * 발신자 A 의 인증 결과가 발신자 B 의 관문을 열어서는 안 된다 — 이전의 전역 boolean 하나짜리
 * 캐시는 한 발신자 인증만으로 모든 발신자의 Intro·이메일 단계를 건너뛰게 했다.
 *
 * 상태는 DataStore Preferences 로 발신자별 키에 영구 보관 — process death · 앱 재시작 후에도 유지.
 * (이전 구현은 `@Singleton` 메모리 캐시만 — 백그라운드 사망 후 복귀 시 Intro 재노출되는 비대칭이 있었음.)
 */
interface IdentityVerificationRepository {
    /** [senderId] 발신자에 대한 본인 확인 완료 여부. DataStore 영구 저장이라 process restart 후에도 값 유지. */
    fun isVerified(senderId: String): Flow<Boolean>

    /** [senderId] 발신자에 대한 본인 확인 완료 표시. DataStore 에 영구 저장. */
    suspend fun markVerified(senderId: String)
}
