package com.afternote.feature.mindrecord.domain.model

/**
 * 깊은 생각 한 건 (`GET /deep-thought` 항목).
 *
 * 홈 `WeeklySummaryGrid` 의 "최근 깊은생각" 카드가 필요로 하는 값만 담는다 — 목록 응답이
 * 주는 나머지(본문·태그·수신자)는 이 화면의 관심사가 아니다 (#207).
 */
data class DeepThought(
    val id: Long,
    /** 서버 원본 표기 (`yyyy.MM.dd 요일`) — 카드에 그대로 노출한다. */
    val createdAt: String,
    val title: String,
)
