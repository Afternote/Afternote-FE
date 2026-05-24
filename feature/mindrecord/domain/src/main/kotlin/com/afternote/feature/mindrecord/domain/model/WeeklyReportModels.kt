package com.afternote.feature.mindrecord.domain.model

data class WeeklyReport(
    val dailyQuestionAmount: Int,
    val diaryAmount: Int,
    val deepThoughtAmount: Int,
    val summaryText: String,
    val week: List<WeeklyReportDay>,
    val dailyQuestions: List<WeeklyReportDailyQuestion>,
    val emotions: List<WeeklyReportEmotion>,
)

data class WeeklyReportDay(
    val diaryId: Long,
    val day: Int,
    val isDiary: Boolean,
    val emotion: TodayMood?,
)

data class WeeklyReportDailyQuestion(
    val title: String,
    val content: String,
    val date: String,
)

data class WeeklyReportEmotion(
    val keyword: String,
    val percentage: Int,
)
