package com.afternote.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 카카오톡 스킴으로 들어온 초대 토큰을 수락·처분할 때까지 보관한다 (#944).
 *
 * 스킴 진입 뒤 로그인·가입을 거치는 동안 Activity 가 재생성되고 프로세스도 죽을 수 있어 메모리에
 * 둘 수 없다. 기기 수명 저장소에 두며 **로그아웃에 지우지 않는다** — 초대는 계정이 아니라 기기로
 * 들어온 것이고, 로그인 전에 받은 초대를 로그인 뒤에 수락하는 것이 정상 경로다.
 */
interface PendingReceiverInvitationStore {
    /** 보관 중인 토큰. 없으면 null. */
    val pendingToken: Flow<String?>

    /** 새 토큰을 보관한다. 이미 있던 토큰은 새 것으로 바뀐다 — 가장 최근에 열린 초대가 사용자의 의도다. */
    suspend fun save(token: String)

    /** 수락 완료·만료·거절처럼 더는 쓸 수 없는 토큰을 지운다. */
    suspend fun clear()
}
