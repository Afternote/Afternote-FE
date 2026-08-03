@file:OptIn(ExperimentalSerializationApi::class)

package com.afternote.feature.mindrecord.data.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

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
    @SerialName("date") val date: String,
    @SerialName("imageUrl") val imageUrl: String? = null,
)

/**
 * `/diary` 목록 항목.
 *
 * 와이어 키가 명세와 실서버 사이에서 갈린다 — 명세("Diary 조회")의 예시는 `id`·`date` 이고
 * 실서버는 `diaryId`·`createdAt` 로 관측됐다. 필수 프로퍼티가 하나라도 비면
 * `MissingFieldException` 으로 **목록 전체**가 날아가므로, 양쪽 키를 함께 받고
 * 식별자 외에는 기본값을 둔다.
 *
 * `todayMood` 를 nullable 로 둔 것도 같은 이유다. 명세 예시에는 클라 enum 에 없는
 * `"SMILE"` 이 적혀 있는데, non-null 이면 값 하나가 어긋난 순간 그 달 일기가 통째로
 * 사라진다 (`coerceInputValues` 는 기본값이 있어야 동작한다).
 */
@Serializable
data class DiaryListItemDto(
    @SerialName("diaryId")
    @JsonNames("id")
    val diaryId: Long,
    @SerialName("title") val title: String = "",
    @SerialName("content") val content: String = "",
    @SerialName("createdAt")
    @JsonNames("date")
    val createdAt: String = "",
    @SerialName("imageUrl") val imageUrl: String? = null,
    @SerialName("todayMood") val todayMood: TodayMoodDto? = null,
    @SerialName("isDraft")
    @JsonNames("draft")
    val isDraft: Boolean = false,
)

// `/diary` 응답의 `data` 는 객체 — `diaries` 외에 조회 대상 달의 비-임시 다이어리 수
// (`monthDiaryCount`)와 최근 7일 최빈 기분(`weeklyDominantMood`)이 함께 내려옴.
@Serializable
data class DiaryListDto(
    @SerialName("diaries") val diaries: List<DiaryListItemDto> = emptyList(),
    @SerialName("monthDiaryCount") val monthDiaryCount: Int = 0,
    @SerialName("weeklyDominantMood") val weeklyDominantMood: TodayMoodDto? = null,
)
