package com.afternote.feature.mindrecord.data.mapper

import com.afternote.feature.mindrecord.data.dto.WeeklyReportDailyQuestionDto
import com.afternote.feature.mindrecord.data.dto.WeeklyReportDayDto
import com.afternote.feature.mindrecord.data.dto.WeeklyReportDto
import com.afternote.feature.mindrecord.data.dto.WeeklyReportEmotionDto
import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.model.WeeklyReportDailyQuestion
import com.afternote.feature.mindrecord.domain.model.WeeklyReportDay
import com.afternote.feature.mindrecord.domain.model.WeeklyReportEmotion

fun WeeklyReportDto.toDomain(): WeeklyReport =
    WeeklyReport(
        dailyQuestionAmount = dailyQuestionAmount,
        diaryAmount = diaryAmount,
        summaryText = summaryText,
        week = week.map { it.toDomain() },
        dailyQuestions = dailyQuestions.map { it.toDomain() },
        emotions = emotions.map { it.toDomain() },
    )

fun WeeklyReportDayDto.toDomain(): WeeklyReportDay =
    WeeklyReportDay(
        diaryId = diaryId,
        day = day,
        isDiary = isDiary,
        emotion = emotion?.toDomain(),
    )

fun WeeklyReportDailyQuestionDto.toDomain(): WeeklyReportDailyQuestion =
    WeeklyReportDailyQuestion(
        title = title,
        content = content,
        date = date,
    )

fun WeeklyReportEmotionDto.toDomain(): WeeklyReportEmotion =
    WeeklyReportEmotion(
        keyword = keyword,
        percentage = percentage,
    )
