package com.afternote.feature.mindrecord.presentation.mapper

import com.afternote.feature.mindrecord.domain.model.Diary
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.presentation.model.DailyDiary
import com.afternote.feature.mindrecord.presentation.model.DailyQuestion
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.afternote.feature.mindrecord.domain.model.DailyQuestion as DailyQuestionDomain

// 서버는 보통 `"yyyy.MM.dd 요일"` (예: "2026.05.22 금") 형식으로 내려주고,
// 일부 경로는 ISO (`yyyy-MM-dd`) 도 가능하므로 두 포맷 모두 허용.
private val DateFormatters: List<DateTimeFormatter> =
    listOf(
        DateTimeFormatter.ofPattern("yyyy.MM.dd"),
        DateTimeFormatter.ISO_DATE,
    )

fun DailyQuestionDomain.toUi(): DailyQuestion =
    DailyQuestion(
        id = dailyQuestionId,
        title = title,
        date = parseLocalDate(createdAt),
        content = content,
        imageUrl = imageUrl,
    )

fun Diary.toUi(): DailyDiary =
    DailyDiary(
        id = diaryId,
        title = title,
        date = parseLocalDate(createdAt),
        content = content,
        emotion = todayMood.toEmoji(),
        imageUrl = imageUrl,
    )

fun TodayMood.toEmoji(): String =
    when (this) {
        TodayMood.HAPPY -> "😊"
        TodayMood.SOSO -> "😐"
        TodayMood.SAD -> "😢"
    }

private fun parseLocalDate(raw: String): LocalDate {
    // "2026.05.22 금" 처럼 뒤에 요일이 붙어오는 케이스 대응 — 첫 공백 앞부분만 파싱.
    val datePart = raw.substringBefore(' ').trim()
    for (formatter in DateFormatters) {
        runCatching { return LocalDate.parse(datePart, formatter) }
    }
    return LocalDate.now()
}
