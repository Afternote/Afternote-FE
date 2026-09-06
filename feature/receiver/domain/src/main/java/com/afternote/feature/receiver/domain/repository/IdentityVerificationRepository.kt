package com.afternote.feature.receiver.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 수신자 본인 확인(designs 2·3·4) 완료 여부 — 발신자 상세 → 열람 신청 진입 시 분기점.
 *
 * **검증 단위 = 발신자별 1회** (#597). 이메일 인증은 "이 발신자가 지정한 수신자 이메일" 검증이라
 * 발신자 A 의 인증 결과가 발신자 B 의 관문을 열어서는 안 된다 — 이전의 전역 boolean 하나짜리
 * 캐시는 한 발신자 인증만으로 모든 발신자의 Intro·이메일 단계를 건너뛰게 했다.
 *
 * 캐시 수명 = **프로세스** (#597 리뷰 반영). 키인 `senderId` 자체가 in-memory `SenderRegistry` 발급
 * UUID 라 프로세스를 넘어 같은 발신자를 가리키지 못한다 — 디스크에 영속해도 재시작 후 재조회가
 * 불가능해 (#912 가 도입했던 DataStore 영속은) 죽은 키 누적 비용만 남아, 저장을 키 수명에 맞췄다.
 * process death · 앱 재시작 후에는 발신자 재등록과 함께 본인 확인도 다시 진행한다.
 * 프로세스를 넘는 발신자 식별자가 생기면 그때 영속을 되살린다.
 */
interface IdentityVerificationRepository {
    /** [senderId] 발신자에 대한 본인 확인 완료 여부. 프로세스 수명 캐시 — 앱 재시작 시 재인증. */
    fun isVerified(senderId: String): Flow<Boolean>

    /** [senderId] 발신자에 대한 본인 확인 완료 표시. 프로세스 수명 캐시에 기록한다. */
    suspend fun markVerified(senderId: String)
}
