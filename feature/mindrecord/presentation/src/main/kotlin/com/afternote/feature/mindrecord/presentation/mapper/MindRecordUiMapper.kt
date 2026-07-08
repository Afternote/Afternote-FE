package com.afternote.feature.mindrecord.presentation.mapper

import androidx.compose.ui.graphics.Color
import com.afternote.feature.mindrecord.domain.model.DeepThought
import com.afternote.feature.mindrecord.domain.model.DeepThoughtCategory
import com.afternote.feature.mindrecord.domain.model.Diary
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.presentation.model.CategoryUiModel
import com.afternote.feature.mindrecord.presentation.model.DailyDiary
import com.afternote.feature.mindrecord.presentation.model.DailyQuestion
import com.afternote.feature.mindrecord.presentation.model.DeepThoughtModel
import com.afternote.feature.mindrecord.presentation.model.Tag
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
        receiverNames = receivers.map { it.name },
        imageUrl = imageUrl,
    )

fun Diary.toUi(): DailyDiary =
    DailyDiary(
        id = diaryId,
        title = title,
        // 명세의 `date` (작성일) 우선, 없으면 createdAt 으로 폴백.
        date = parseLocalDate(date ?: createdAt),
        content = content,
        emotion = todayMood.toEmoji(),
    )

fun DeepThought.toUi(): DeepThoughtModel =
    DeepThoughtModel(
        id = deepThoughtId,
        title = title,
        content = content,
        date = parseLocalDate(createdAt),
        tag = tags.map { Tag(name = it, count = 0) },
        category = category,
        isDraft = isDraft,
    )

fun TodayMood.toEmoji(): String =
    when (this) {
        TodayMood.HAPPY -> "😊"
        TodayMood.SOSO -> "😐"
        TodayMood.SAD -> "😢"
    }

private val CategoryColorPalette: List<Color> =
    listOf(
        Color(0xFF1A1A1A),
        Color(0xFFFFB3A7),
        Color(0xFFA8C8E8),
        Color(0xFFB7E1B7),
        Color(0xFFFFE7A3),
        Color(0xFFD7BDE2),
    )

fun DeepThoughtCategory.toUi(): CategoryUiModel =
    CategoryUiModel(
        id = categoryId.toString(),
        name = title,
        color = CategoryColorPalette[(categoryId.toInt().mod(CategoryColorPalette.size))],
    )

private fun parseLocalDate(raw: String): LocalDate {
    // "2026.05.22 금" 처럼 뒤에 요일이 붙어오는 케이스 대응 — 첫 공백 앞부분만 파싱.
    val datePart = raw.substringBefore(' ').trim()
    for (formatter in DateFormatters) {
        runCatching { return LocalDate.parse(datePart, formatter) }
    }
    return LocalDate.now()
}
