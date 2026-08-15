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

/** `week[].type` 중 일기를 뜻하는 값. 나머지(`DAILY_QUESTION`·`DEEP_THOUGHT`·미래 종류)는 일기가 아니다. */
private const val WEEK_RECORD_TYPE_DIARY = "DIARY"

/**
 * 와이어의 `type` 문자열을 도메인 의미(`isDiary`)로 접는다.
 *
 * 대소문자는 가리지 않는다 — 명세 enum 은 대문자지만, 종류 판별이 표기 하나로 뒤집혀
 * 캘린더에서 일기가 통째로 사라지는 실패는 폭이 너무 크다.
 */
fun WeeklyReportDayDto.toDomain(): WeeklyReportDay =
    WeeklyReportDay(
        diaryId = diaryId,
        day = day,
        isDiary = type.equals(WEEK_RECORD_TYPE_DIARY, ignoreCase = true),
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
