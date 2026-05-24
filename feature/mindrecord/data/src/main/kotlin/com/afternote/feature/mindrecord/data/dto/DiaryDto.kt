package com.afternote.feature.mindrecord.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TodayMood {
    @SerialName("HAPPY")
    HAPPY,

    @SerialName("SOSO")
    SOSO,

    @SerialName("SAD")
    SAD,
}

@Serializable
data class DiaryCreateRequest(
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("isDraft") val isDraft: Boolean,
    @SerialName("todayMood") val todayMood: TodayMood,
    @SerialName("imageUrl") val imageUrl: String? = null,
)

@Serializable
data class DiaryUpdateRequest(
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("isDraft") val isDraft: Boolean,
    @SerialName("todayMood") val todayMood: TodayMood,
    @SerialName("date") val date: String,
    @SerialName("imageUrl") val imageUrl: String? = null,
)

@Serializable
data class DiaryListItem(
    @SerialName("diaryId") val diaryId: Long,
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("imageUrl") val imageUrl: String? = null,
    @SerialName("todayMood") val todayMood: TodayMood,
)

// `/diary` 응답의 `data` 는 배열이 아니라 객체 — `diaries` 외에도
// `yearMonth`, `monthDiaryCount`, `weeklyDominantMood` 가 함께 내려오지만
// 현재는 목록만 사용. (Json 글로벌 설정에 `ignoreUnknownKeys = true` 적용됨)
@Serializable
data class DiaryListResponse(
    @SerialName("diaries") val diaries: List<DiaryListItem> = emptyList(),
)
