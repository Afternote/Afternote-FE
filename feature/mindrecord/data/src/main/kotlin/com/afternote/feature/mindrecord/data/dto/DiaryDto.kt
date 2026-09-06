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
    // imageUrl 은 계약에 없어 걷었다 — 보내도 서버가 버리고 응답에도 키가 없다. 목록
    // 썸네일은 본문 HTML 의 첫 img 에서 뽑는다 (#1024, 데일리질문 #549 와 같은 규칙).
    // 생성 API 에서 `null` 과 빈 목록은 모두 "수신자 없음" 으로 정규화되고, 작성 UI 는 항상
    // 목록을 갖고 있다. 빈 목록을 그대로 보내면 되므로 nullable 로 낮추지 않는다 (#789).
    @SerialName("receiverIds") val receiverIds: List<Long>,
    /**
     * 기록일 `yyyy-MM-dd`. 미전송이면 서버가 오늘(Asia/Seoul)로 채우고, **미래 날짜는
     * 400(code 2101)** 이다 (`Afternote-BE#244`, PR #262). 작성 화면이 항상 값을 갖고
     * 있어 nullable 로 낮추지 않는다 (#1008).
     */
    @SerialName("date") val date: String,
)

@Serializable
data class DiaryUpdateRequestDto(
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("isDraft") val isDraft: Boolean,
    @SerialName("todayMood") val todayMood: TodayMoodDto,
    /** null 이면 기존 수신자 유지, 빈 목록이면 전체 해제 (서버 규칙). */
    @SerialName("receiverIds") val receiverIds: List<Long>? = null,
    /** 기록일 `yyyy-MM-dd`. **null 이면 기존 기록일 유지** (서버 규칙). 미래 날짜는 400(code 2101). */
    @SerialName("date") val date: String? = null,
)

/**
 * `/diary` 목록 항목 (Swagger `DiaryResponse`, 실서버 응답 실측 2026-08-23).
 *
 * 식별자를 뺀 나머지에도 기본값을 두지 않는다. 이 필드들은 서버가 항상 채워 보내는 값이라,
 * 기본값이 있으면 응답 키 누락과 계약 변경이 파싱 실패가 아니라 **정상적인 빈 값**으로 바뀐다.
 * 특히 `isDraft` 가 `false` 로 접히면 임시저장 일기가 목록에 노출되고, `date` 가 빈 문자열이
 * 되면 캘린더에서 기록이 통째로 사라진다 (#789).
 *
 * (문서의 `required` 목록은 근거로 쓰지 않는다 — springdoc 은 `@Schema(requiredMode)` 가
 * 없으면 non-null 프로퍼티도 required 에 넣지 않아, 비어 있다는 사실이 서버가 그 필드를
 * 생략한다는 신호가 아니다. 판단은 실제 응답과 저장 컬럼 필수 여부를 기준으로 한다.)
 *
 * `id` 는 노션 명세("Diary 조회") 예시의 키다. Swagger 에는 없지만 두 문서가 갈려 있어
 * 대체 키로 함께 받는다 — 실제 응답에 `id` 가 없어 충돌하지 않는다.
 */
@Serializable
data class DiaryListItemDto(
    @SerialName("diaryId")
    @JsonNames("id")
    val diaryId: Long,
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    /**
     * 사용자가 고른 **일기의 날짜** (`format: date`, 예 `"2026-03-21"`).
     *
     * [createdAt] 과 **별개 필드**다 — 작성 화면의 날짜 선택 값이 여기 들어가고,
     * `createdAt` 은 레코드가 만들어진 시각이다. 캘린더에 찍어야 하는 쪽은 이 값이다.
     * 둘을 `@JsonNames` 로 한 프로퍼티에 묶으면 서버의 키 순서에 따라 값이 뒤바뀐다.
     */
    @SerialName("date") val date: String,
    @SerialName("createdAt") val createdAt: String,
    // Swagger `DiaryResponse` 에 없는 필드 — 서버가 주기 시작하면 쓰이고, 아니면 계속 null.
    @SerialName("imageUrl") val imageUrl: String? = null,
    // 저장 컬럼이 필수라 응답도 항상 채워진다. AI 가 매기는 `emotion` 과 달리 사용자가 직접
    // 고른 값이고, 한글 값이 관측된 쪽도 `emotion` 이지 이 필드가 아니다 (#591, #789).
    @SerialName("todayMood") val todayMood: TodayMoodDto,
    // 기본값을 두지 않는다 — 키가 빠지면 false 로 접혀 임시저장이 목록에 샌다 (#789).
    @SerialName("isDraft")
    @JsonNames("draft")
    val isDraft: Boolean,
    // 상세 화면이 "수신인 OOO" 로 보여준다. 서버가 늘 함께 내려주는데 종전에는 선언하지
    // 않아 버려졌다 (#759).
    @SerialName("receivers") val receivers: List<MindRecordReceiverDto>,
)

/** 기록에 지정된 수신자 요약 (OpenAPI `MindRecordReceiverSummaryResponse`). */
@Serializable
data class MindRecordReceiverDto(
    @SerialName("receiverId") val receiverId: Long,
    @SerialName("name") val name: String,
)

// `/diary` 응답의 `data` 는 객체 — `diaries` 외에 조회 대상 달의 비-임시 다이어리 수
// (`monthDiaryCount`)와 최근 7일 최빈 기분(`weeklyDominantMood`)이 함께 내려옴.
@Serializable
data class DiaryListDto(
    @SerialName("diaries") val diaries: List<DiaryListItemDto>,
    @SerialName("monthDiaryCount") val monthDiaryCount: Int,
    // 그 주에 기록이 없으면 서버가 `null` 을 **명시적으로** 보낸다. 값이 없다는 뜻이 실제로
    // 있으므로 nullable 은 유지하되, 키 자체는 계약이라 기본값은 두지 않는다 (#789).
    @SerialName("weeklyDominantMood")
    @Serializable(with = NullableTodayMoodSerializer::class)
    val weeklyDominantMood: TodayMoodDto?,
)
