package com.afternote.feature.mindrecord.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeeklyReportResponse(
    @SerialName("dailyQuestionAmount") val dailyQuestionAmount: Int = 0,
    @SerialName("diaryAmount") val diaryAmount: Int = 0,
    @SerialName("summaryText") val summaryText: String = "",
    @SerialName("week") val week: List<WeeklyReportDayDto> = emptyList(),
    // 서버 JSON 키가 kebab-case (`daily-question`).
    @SerialName("daily-question") val dailyQuestions: List<WeeklyReportDailyQuestionDto> = emptyList(),
    @SerialName("emotions") val emotions: List<WeeklyReportEmotionDto> = emptyList(),
)

@Serializable
data class WeeklyReportDayDto(
    @SerialName("diaryId") val diaryId: Long = 0L,
    @SerialName("day") val day: Int = 0,
    @SerialName("isDiary") val isDiary: Boolean = false,
    @SerialName("emotion") val emotion: TodayMood? = null,
)

@Serializable
data class WeeklyReportDailyQuestionDto(
    @SerialName("title") val title: String = "",
    @SerialName("content") val content: String = "",
    @SerialName("date") val date: String = "",
)

@Serializable
data class WeeklyReportEmotionDto(
    @SerialName("keyword") val keyword: String = "",
    @SerialName("percentage") val percentage: Int = 0,
)
