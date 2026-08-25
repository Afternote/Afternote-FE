package com.afternote.feature.mindrecord.domain.model

/**
 * 수신자에게 전달된 마음의 기록 전체.
 *
 * 서버가 데일리질문/일기를 각각의 `receiver-auth` 엔드포인트로 분리 제공하므로
 * 두 목록을 병렬 조회해 하나로 묶는다.
 */
data class ReceiverMindRecords(
    val dailyQuestions: List<MindRecordSummary>,
    val diaries: List<MindRecordSummary>,
)

data class MindRecordSummary(
    val id: Long,
    val type: MindRecordType,
    val title: String,
    val content: String,
    /** 기록 날짜 (`yyyy-MM-dd`) — 기간 필터/정렬 기준으로 쓰는 정규화 값. */
    val recordDate: String,
    val isDraft: Boolean,
    /** 서버 원본 표기 (`yyyy.MM.dd 요일`) — 카드에 그대로 노출. */
    val createdAt: String,
    val imageUrl: String? = null,
    /**
     * 작성자가 고른 오늘의 기분. 일기에만 있고 데일리질문에는 없다.
     *
     * 종전에는 목록이 이 값을 버려서, 수신자가 본문과 함께 볼 수 없었다 (#618).
     */
    val todayMood: TodayMood? = null,
)
