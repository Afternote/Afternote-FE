package com.afternote.feature.receiver.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 수신자 본인 확인(designs 2·3·4) 완료 여부 — 발신자 상세 → 열람 신청 진입 시 분기점.
 *
 * **검증 단위 = 발신자별 1회** (#597). 이메일 인증은 "이 발신자가 지정한 수신자 이메일" 검증이라
 * 발신자 A 의 인증 결과가 발신자 B 의 관문을 열어서는 안 된다 — 이전의 전역 boolean 하나짜리
 * 캐시는 한 발신자 인증만으로 모든 발신자의 Intro·이메일 단계를 건너뛰게 했다.
 *
 * 저장 수명 = **로그인 SESSION**. 발신자 카드 저장소가 로컬 `senderId`를 프로세스 재시작 뒤에도
 * 그대로 복원하므로, 같은 ID에 귀속된 본인 확인 완료 상태도 앱 재기동 뒤 다시 찾을 수 있다.
 * 로그아웃·회원 탈퇴 때 SESSION 저장소가 함께 지워져 다음 계정으로 상태가 넘어가지 않는다.
 */
interface IdentityVerificationRepository {
    /** [senderId] 발신자에 대한 본인 확인 완료 여부. 저장소 읽기 실패는 안전하게 `false`로 본다. */
    fun isVerified(senderId: String): Flow<Boolean>

    /** [senderId] 발신자에 대한 본인 확인 완료 표시. 로그인 SESSION에 영속한다. */
    suspend fun markVerified(senderId: String)
}
