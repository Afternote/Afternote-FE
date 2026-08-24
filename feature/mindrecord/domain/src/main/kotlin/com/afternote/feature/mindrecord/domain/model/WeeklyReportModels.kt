package com.afternote.feature.mindrecord.domain.model

data class WeeklyReport(
    val dailyQuestionAmount: Int,
    val diaryAmount: Int,
    val summaryText: String,
    val week: List<WeeklyReportDay>,
    val dailyQuestions: List<WeeklyReportDailyQuestion>,
    val emotions: List<WeeklyReportEmotion>,
)

data class WeeklyReportDay(
    val diaryId: Long,
    val day: Int,
    val isDiary: Boolean,
    /**
     * 기록일수에 세는 종류인지 (#590).
     *
     * `week[]` 는 일기 외의 종류도 싣는다. 데일리질문은 세지만 **깊은 생각은 기획에서
     * 제거된 기능이라 세지 않는다** — 서버가 계속 내려줘도 무시한다.
     */
    val countsAsRecord: Boolean,
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
