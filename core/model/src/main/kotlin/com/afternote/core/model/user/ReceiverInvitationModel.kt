package com.afternote.core.model.user

/**
 * 카카오톡 수신자 초대 (`POST receiver-invitations`) 발급 결과 (#944).
 *
 * 서버는 `invitationId`·`invitationUrl`·`expiresAt` 도 함께 내려주지만 여기엔 올리지 않는다 —
 * 공유는 카카오 메시지 템플릿에 [token] 만 싣고, 만료 안내 문구는 «7일» 고정 캡션이라 읽는 곳이 없다.
 * 소비처가 생기면 그때 올린다.
 *
 * @property token 초대를 식별하는 비밀값(43자 base64url). 로그·리포팅에 원문을 남기지 않는다.
 */
data class ReceiverInvitationCreated(
    val token: String,
)

/**
 * 초대 조회 (`GET receiver-invitations/{token}`) 결과 — 랜딩 화면이 그리는 값만 담는다.
 *
 * 서버 `status`(PENDING·ACCEPTED)는 올리지 않는다. ACCEPTED 여도 본인이 수락한 것인지 남이 수락한
 * 것인지 조회만으로는 갈리지 않고, 그 판정은 수락 요청의 실패 코드가 한다.
 *
 * @property isExpired 만료된 초대. 수락 버튼을 보이지 않고 안내로 끝낸다.
 */
data class ReceiverInvitationLookup(
    val inviterName: String,
    val isExpired: Boolean,
)

/**
 * 초대 수락 (`POST receiver-invitations/{token}/accept`) 결과.
 *
 * 서버가 주는 `receiverId` 는 올리지 않는다 — 완료 화면은 이름만 쓰고, 받은 기록함은 자기 조회로 목록을 만든다.
 */
data class ReceiverInvitationAccepted(
    val inviterName: String,
)
