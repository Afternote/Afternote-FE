package com.afternote.feature.mindrecord.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `GET /receiver-auth/daily-question` 응답 (`data`). */
@Serializable
data class ReceiverDailyQuestionListDto(
    @SerialName("dailyQuestions") val dailyQuestions: List<ReceiverDailyQuestionItemDto>,
)

@Serializable
data class ReceiverDailyQuestionItemDto(
    @SerialName("userDailyQuestionId") val userDailyQuestionId: Long,
    // 원본 질문 내용이 title, 사용자가 작성한 답변이 content.
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    // "yyyy.MM.dd 요일" 형식.
    @SerialName("createdAt") val createdAt: String,
    // Swagger `DailyQuestionListResponse` 계약에 없는 필드 — 기본값을 유지한다 (#789).
    @SerialName("imageUrl") val imageUrl: String? = null,
)

/** `GET /receiver-auth/diary` 응답 (`data`). */
@Serializable
data class ReceiverDiaryListDto(
    @SerialName("diaries") val diaries: List<ReceiverDiaryItemDto>,
)

/**
 * 수신자용 일기 항목. 발신자 목록과 **같은 `DiaryResponse` 계약**을 쓰므로
 * nullability 와 기본값도 [DiaryListItemDto] 와 동일하게 맞춘다 (#789).
 */
@Serializable
data class ReceiverDiaryItemDto(
    @SerialName("diaryId") val diaryId: Long,
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("isDraft") val isDraft: Boolean,
    // 응답 계약에 없는 필드 — 기본값을 유지한다.
    @SerialName("imageUrl") val imageUrl: String? = null,
    @SerialName("todayMood") val todayMood: TodayMoodDto,
    // 작성일 (yyyy-MM-dd).
    @SerialName("date") val date: String,
    // "yyyy.MM.dd 요일" 형식.
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String,
)
