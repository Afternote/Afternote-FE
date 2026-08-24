package com.afternote.feature.mindrecord.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TodayMoodDto {
    @SerialName("HAPPY")
    HAPPY,

    @SerialName("SOSO")
    SOSO,

    @SerialName("SAD")
    SAD,
}

@Serializable
data class DiaryCreateRequestDto(
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("isDraft") val isDraft: Boolean,
    @SerialName("todayMood") val todayMood: TodayMoodDto,
    @SerialName("imageUrl") val imageUrl: String? = null,
    @SerialName("receiverIds") val receiverIds: List<Long>? = null,
)

@Serializable
data class DiaryUpdateRequestDto(
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("isDraft") val isDraft: Boolean,
    @SerialName("todayMood") val todayMood: TodayMoodDto,
    /** null 이면 기존 수신자 유지, 빈 목록이면 전체 해제 (서버 규칙). */
    @SerialName("receiverIds") val receiverIds: List<Long>? = null,
)

@Serializable
data class DiaryListItemDto(
    @SerialName("diaryId") val diaryId: Long,
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("imageUrl") val imageUrl: String? = null,
    @SerialName("todayMood") val todayMood: TodayMoodDto,
)

// `/diary` 응답의 `data` 는 객체 — `diaries` 외에 조회 대상 달의 비-임시 다이어리 수
// (`monthDiaryCount`)와 최근 7일 최빈 기분(`weeklyDominantMood`)이 함께 내려옴.
@Serializable
data class DiaryListDto(
    @SerialName("diaries") val diaries: List<DiaryListItemDto> = emptyList(),
    @SerialName("monthDiaryCount") val monthDiaryCount: Int = 0,
    @SerialName("weeklyDominantMood") val weeklyDominantMood: TodayMoodDto? = null,
)
