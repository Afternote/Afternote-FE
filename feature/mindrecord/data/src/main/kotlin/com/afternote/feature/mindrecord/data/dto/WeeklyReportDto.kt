package com.afternote.feature.mindrecord.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `GET /mind-record` 응답 (`data`). 카운트·요약·목록 어느 것도 기본값을 두지 않는다 —
 * 수치가 `0` 으로, 목록이 빈 목록으로 접히면 잘못된 주간 리포트가 정상 화면으로 보인다 (#789).
 */
@Serializable
data class WeeklyReportDto(
    @SerialName("dailyQuestionAmount") val dailyQuestionAmount: Int,
    @SerialName("diaryAmount") val diaryAmount: Int,
    @SerialName("summaryText") val summaryText: String,
    @SerialName("week") val week: List<WeeklyReportDayDto>,
    // 서버 JSON 키가 kebab-case (`daily-question`).
    @SerialName("daily-question") val dailyQuestions: List<WeeklyReportDailyQuestionDto>,
    @SerialName("emotions") val emotions: List<WeeklyReportEmotionDto>,
)

/**
 * `week[]` 의 한 원소 (OpenAPI `WeekRecordItem` 실측, 2026-08-15).
 *
 * 기록 종류는 `isDiary` 불리언이 아니라 **`type` 문자열**로 온다.
 * ```
 * WeekRecordItem  required: [day, diaryId, type]
 *   type  string  enum=[DIARY, DAILY_QUESTION, DEEP_THOUGHT]
 * ```
 * 종전 DTO 는 존재하지 않는 `isDiary` 키를 읽어 항상 기본값 `false` 가 됐고, 그래서 모든
 * 일기가 non-diary 로 접혀 캘린더 점과 기록일수에서 빠졌다.
 *
 * 네 필드 모두 기본값을 두지 않는다 — 빠지면 실패해야 계약 누락이 드러난다. [emotion] 은
 * 값 자체가 조건부라 nullable 은 유지하지만, 서버가 키는 명시적으로 보내므로 기본값은
 * 없다 (#789). 다만 [type] 은 `enum` 대신 `String` 으로 받는다. 서버가 종류를 하나 더
 * 추가해도(현재 3종) 그 주 전체가 죽지 않고 매퍼에서 "일기 아님" 으로 접히면 되기
 * 때문이다 — 값 집합은 닫혀 있지 않지만 키의 존재는 계약이다.
 */
@Serializable
data class WeeklyReportDayDto(
    @SerialName("diaryId") val diaryId: Long,
    @SerialName("day") val day: Int,
    @SerialName("type") val type: String,
    @SerialName("emotion")
    @Serializable(with = NullableTodayMoodSerializer::class)
    val emotion: TodayMoodDto?,
)

@Serializable
data class WeeklyReportDailyQuestionDto(
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("date") val date: String,
)

@Serializable
data class WeeklyReportEmotionDto(
    @SerialName("keyword") val keyword: String,
    @SerialName("percentage") val percentage: Int,
)
