package com.afternote.feature.mindrecord.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `GET /receiver-auth/daily-question` 응답 (`data`). */
@Serializable
data class ReceiverDailyQuestionListResponse(
    @SerialName("dailyQuestions") val dailyQuestions: List<ReceiverDailyQuestionItem> = emptyList(),
)

@Serializable
data class ReceiverDailyQuestionItem(
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
data class ReceiverDiaryListResponse(
    @SerialName("diaries") val diaries: List<ReceiverDiaryItem> = emptyList(),
)

@Serializable
data class ReceiverDiaryItem(
    @SerialName("diaryId") val diaryId: Long,
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("isDraft") val isDraft: Boolean = false,
    @SerialName("imageUrl") val imageUrl: String? = null,
    @SerialName("todayMood") val todayMood: TodayMood? = null,
    // 작성일 (yyyy-MM-dd).
    @SerialName("date") val date: String = "",
    // "yyyy.MM.dd 요일" 형식.
    @SerialName("createdAt") val createdAt: String = "",
    @SerialName("updatedAt") val updatedAt: String = "",
)

/** `GET /receiver-auth/deep-thought` 응답 (`data`) — 필터용 카테고리/태그 목록을 함께 내려준다. */
@Serializable
data class ReceiverDeepThoughtListResponse(
    @SerialName("categories") val categories: List<String> = emptyList(),
    @SerialName("tagCounts") val tagCounts: List<ReceiverDeepThoughtTagCount> = emptyList(),
    @SerialName("deepThoughts") val deepThoughts: List<ReceiverDeepThoughtItem> = emptyList(),
)

@Serializable
data class ReceiverDeepThoughtTagCount(
    @SerialName("tag") val tag: String,
    @SerialName("count") val count: Int,
)

@Serializable
data class ReceiverDeepThoughtItem(
    @SerialName("deepThoughtId") val deepThoughtId: Long,
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("isDraft") val isDraft: Boolean = false,
    @SerialName("imageUrl") val imageUrl: String? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("tags") val tags: List<String> = emptyList(),
    // "yyyy.MM.dd 요일" 형식.
    @SerialName("createdAt") val createdAt: String = "",
    @SerialName("updatedAt") val updatedAt: String = "",
)
