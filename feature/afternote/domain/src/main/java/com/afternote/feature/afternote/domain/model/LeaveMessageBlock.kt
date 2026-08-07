package com.afternote.feature.afternote.domain.model

/**
 * "남기실 말씀" 한 덩어리 — 서버 `leaveMessage` 배열의 원소다.
 *
 * 서버는 [body] 를 필수(공백 불가)로, [title] 을 선택으로 검증한다. 발신자 작성분과 수신자 열람분이
 * 같은 구조라 `model/author`·`model/receiver` 어느 쪽에도 두지 않는다.
 */
data class LeaveMessageBlock(
    val title: String?,
    val body: String,
)
