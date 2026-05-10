package com.afternote.feature.mindrecord.presentation.mapper

import com.afternote.feature.mindrecord.domain.model.DeepThought
import com.afternote.feature.mindrecord.domain.model.Diary
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.presentation.model.DailyDiary
import com.afternote.feature.mindrecord.presentation.model.DailyQuestion
import com.afternote.feature.mindrecord.presentation.model.DeepThoughtModel
import com.afternote.feature.mindrecord.presentation.model.Tag
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.afternote.feature.mindrecord.domain.model.DailyQuestion as DailyQuestionDomain

private val IsoDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_DATE

fun DailyQuestionDomain.toUi(): DailyQuestion =
    DailyQuestion(
        id = dailyQuestionId,
        title = title,
        date = parseLocalDate(createdAt),
        content = content,
    )

fun Diary.toUi(): DailyDiary =
    DailyDiary(
        id = diaryId,
        title = title,
        date = parseLocalDate(createdAt),
        content = content,
        emotion = todayMood.toEmoji(),
    )

fun DeepThought.toUi(): DeepThoughtModel =
    DeepThoughtModel(
        id = deepThoughtId,
        title = title,
        content = content,
        date = LocalDate.now(),
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

private fun parseLocalDate(raw: String): LocalDate =
    runCatching { LocalDate.parse(raw, IsoDateFormatter) }
        .getOrElse { LocalDate.now() }
