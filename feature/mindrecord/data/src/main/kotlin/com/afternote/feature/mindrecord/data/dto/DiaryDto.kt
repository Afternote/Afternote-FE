package com.afternote.feature.mindrecord.data.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

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
    @SerialName("receiverIds") val receiverIds: List<Long> = emptyList(),
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

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DiaryListItem(
    // 명세서 예시 키는 `id` — 기존 서버 키 `diaryId` 와 함께 허용.
    @SerialName("diaryId")
    @JsonNames("id")
    val diaryId: Long,
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    // 작성일 (`yyyy-MM-dd`). createdAt 은 생성 시각 타임스탬프.
    @SerialName("date") val date: String? = null,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("isDraft") val isDraft: Boolean = false,
    @SerialName("imageUrl") val imageUrl: String? = null,
    @SerialName("todayMood") val todayMood: TodayMood,
)

// `/diary` 응답의 `data` 는 객체 — `diaries` 외에 조회 대상 달의 비-임시 다이어리 수
// (`monthDiaryCount`), 최근 7일 최빈 기분(`weeklyDominantMood`), 수신자 목록이 함께 내려옴.
@Serializable
data class DiaryListResponse(
    @SerialName("diaries") val diaries: List<DiaryListItem> = emptyList(),
    @SerialName("monthDiaryCount") val monthDiaryCount: Int = 0,
    @SerialName("weeklyDominantMood") val weeklyDominantMood: TodayMood? = null,
    @SerialName("receivers") val receivers: List<MindRecordReceiverDto> = emptyList(),
)
