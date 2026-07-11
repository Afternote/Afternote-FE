package com.afternote.feature.mindrecord.domain.model

/**
 * 수신자에게 전달된 마음의 기록 전체.
 *
 * 서버가 데일리질문/일기/깊은생각을 각각의 `receiver-auth` 엔드포인트로 분리 제공하므로
 * 세 목록을 병렬 조회해 하나로 묶는다. [deepThoughtCategories]는 깊은생각 응답에 포함된
 * 필터용 카테고리 목록.
 */
data class ReceiverMindRecords(
    val dailyQuestions: List<MindRecordSummary>,
    val diaries: List<MindRecordSummary>,
    val deepThoughts: List<MindRecordSummary>,
    val deepThoughtCategories: List<String>,
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
    /** 깊은생각 카테고리 (다른 타입은 null). */
    val category: String? = null,
)
