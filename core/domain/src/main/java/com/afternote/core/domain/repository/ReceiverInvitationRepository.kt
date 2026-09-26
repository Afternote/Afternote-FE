package com.afternote.core.domain.repository

import com.afternote.core.model.user.ReceiverInvitationAccepted
import com.afternote.core.model.user.ReceiverInvitationCreated
import com.afternote.core.model.user.ReceiverInvitationLookup

/**
 * 카카오톡 수신자 초대 계약 (#944, BE main 3593aa30).
 *
 * 발신자(설정 > 수신자 등록)가 [create] 로 초대를 만들어 카카오톡으로 보내고, 수신자(스킴으로 앱에
 * 들어온 사람)가 [lookup] 으로 초대자를 확인한 뒤 [accept] 로 수신자가 된다. 두 역할이 같은 초대를
 * 다루므로 발신자 쪽 [UserReceiverRepository] 와 수신자 쪽 `feature:receiver:domain` 어느 한쪽에
 * 두지 않고 core 에 둔다.
 *
 * **실패 계약** — 서버 거절·전송 실패는 전부
 * [com.afternote.core.domain.error.ReceiverInvitationFailure] 로 온다.
 */
interface ReceiverInvitationRepository {
    /** 로그인한 사용자의 새 초대를 만든다. 유효 기간은 서버가 정한다(7일). */
    suspend fun create(): Result<ReceiverInvitationCreated>

    /** 비로그인도 가능한 조회 — 랜딩 화면이 초대자 이름과 만료 여부를 그린다. */
    suspend fun lookup(token: String): Result<ReceiverInvitationLookup>

    /** 로그인한 사용자를 초대자의 수신자로 등록한다. 같은 사용자의 재수락은 멱등 성공이다. */
    suspend fun accept(token: String): Result<ReceiverInvitationAccepted>
}
