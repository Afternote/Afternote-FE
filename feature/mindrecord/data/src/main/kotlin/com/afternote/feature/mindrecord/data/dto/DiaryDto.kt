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
 * `/diary` 목록 항목 (Swagger `DiaryResponse` 실측, 2026-08-03).
 *
 * 식별자를 뺀 나머지에 기본값을 두는 이유는 **실패의 폭**이다. 이 DTO 는 목록 원소라,
 * 필드 하나가 비어 `MissingFieldException` 이 나면 그 항목이 아니라 **그 달 목록 전체**가
 * 날아간다. 반면 값이 없을 때 잃는 것은 카드 한 장의 표시 요소뿐이다.
 *
 * `todayMood` 를 nullable 로 둔 것도 같은 맥락이다. non-null 이면 클라가 모르는 기분 값
 * 하나에 목록이 통째로 사라진다 (`coerceInputValues` 는 기본값이 있어야 동작한다).
 * 실제로 `emotion` 은 한글로 내려오는 중이다 (#591).
 *
 * (문서의 `required` 목록은 근거로 쓰지 않는다 — springdoc 은 `@Schema(requiredMode)` 가
 * 없으면 non-null 프로퍼티도 required 에 넣지 않아, 비어 있다는 사실이 서버가 그 필드를
 * 생략한다는 신호가 아니다. 응답 필드의 nullable 판단 기준 자체는 #676 소관.)
 *
 * `id` 는 노션 명세("Diary 조회") 예시의 키다. Swagger 에는 없지만 두 문서가 갈려 있어
 * 대체 키로 함께 받는다 — 실제 응답에 `id` 가 없어 충돌하지 않는다.
 */
@Serializable
data class DiaryListItemDto(
    @SerialName("diaryId")
    @JsonNames("id")
    val diaryId: Long,
    @SerialName("title") val title: String = "",
    @SerialName("content") val content: String = "",
    /**
     * 사용자가 고른 **일기의 날짜** (`format: date`, 예 `"2026-03-21"`).
     *
     * [createdAt] 과 **별개 필드**다 — 작성 화면의 날짜 선택 값이 여기 들어가고,
     * `createdAt` 은 레코드가 만들어진 시각이다. 캘린더에 찍어야 하는 쪽은 이 값이다.
     * 둘을 `@JsonNames` 로 한 프로퍼티에 묶으면 서버의 키 순서에 따라 값이 뒤바뀐다.
     */
    @SerialName("date") val date: String? = null,
    @SerialName("createdAt") val createdAt: String = "",
    // Swagger `DiaryResponse` 에 없는 필드 — 서버가 주기 시작하면 쓰이고, 아니면 계속 null.
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
