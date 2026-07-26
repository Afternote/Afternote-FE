package com.afternote.feature.mindrecord.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `GET /receiver-auth/daily-question` 응답 (`data`). */
@Serializable
data class ReceiverDailyQuestionListDto(
    @SerialName("dailyQuestions") val dailyQuestions: List<ReceiverDailyQuestionItemDto> = emptyList(),
)

@Serializable
data class ReceiverDailyQuestionItemDto(
    @SerialName("userDailyQuestionId") val userDailyQuestionId: Long,
    // 원본 질문 내용이 title, 사용자가 작성한 답변이 content.
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    // "yyyy.MM.dd 요일" 형식.
    @SerialName("createdAt") val createdAt: String,
    @SerialName("imageUrl") val imageUrl: String? = null,
)

/** `GET /receiver-auth/diary` 응답 (`data`). */
@Serializable
data class ReceiverDiaryListDto(
    @SerialName("diaries") val diaries: List<ReceiverDiaryItemDto> = emptyList(),
)

@Serializable
data class ReceiverDiaryItemDto(
    @SerialName("diaryId") val diaryId: Long,
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("isDraft") val isDraft: Boolean = false,
    @SerialName("imageUrl") val imageUrl: String? = null,
    @SerialName("todayMood") val todayMood: TodayMoodDto? = null,
    // 작성일 (yyyy-MM-dd).
    @SerialName("date") val date: String = "",
    // "yyyy.MM.dd 요일" 형식.
    @SerialName("createdAt") val createdAt: String = "",
    @SerialName("updatedAt") val updatedAt: String = "",
)
