package com.afternote.feature.mindrecord.data.mapper

import android.util.Log
import com.afternote.feature.mindrecord.data.dto.EmotionAnalysisSummaryDto
import com.afternote.feature.mindrecord.data.dto.WeeklyReportDailyQuestionDto
import com.afternote.feature.mindrecord.data.dto.WeeklyReportDayDto
import com.afternote.feature.mindrecord.data.dto.WeeklyReportDto
import com.afternote.feature.mindrecord.data.dto.WeeklyReportEmotionDto
import com.afternote.feature.mindrecord.domain.model.EmotionAnalysis
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
        // 날짜를 해석하지 못한 항목은 여기서 뺀다. 종전에는 집계 경로가 버리고 표시 경로가
        // `LocalDate.now()` 로 메워, 같은 목록을 두고 한쪽은 제외·한쪽은 왜곡이었다 (#547).
        dailyQuestions = dailyQuestions.mapNotNull { it.toDomainOrNull() },
        emotions = emotions.map { it.toDomain() },
        emotionAnalysis = emotionAnalysis?.toDomain(),
    )

/** `week[].type` 중 일기를 뜻하는 값. 나머지(`DAILY_QUESTION`·`DEEP_THOUGHT`·미래 종류)는 일기가 아니다. */
private const val WEEK_RECORD_TYPE_DIARY = "DIARY"

/**
 * 기록일수에서 제외할 종류.
 *
 * 깊은 생각은 기획에서 제거됐다. 서버는 `week[]` 에 계속 실어 보내지만 앱은 이 기능을
 * 없는 것으로 다루므로 기록일수에도 세지 않는다 (#590).
 */
private val WEEK_RECORD_TYPES_NOT_COUNTED = setOf("DEEP_THOUGHT")

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
        countsAsRecord = WEEK_RECORD_TYPES_NOT_COUNTED.none { it.equals(type, ignoreCase = true) },
        emotion = emotion?.toDomain(),
    )

/**
 * 날짜를 해석하지 못하면 null 을 돌려 목록에서 제외한다.
 *
 * 오늘로 메우지 않는다 — 그러면 파싱 못 한 기록이 **오늘 작성한 것처럼** HISTORY 카드에
 * 앉아 필드 이상을 감춘다. 로그도 에러 표시도 없는 조용한 오표시였다 (#547).
 */
fun WeeklyReportDailyQuestionDto.toDomainOrNull(): WeeklyReportDailyQuestion? {
    val parsedDate =
        parseServerDateOrNull(date) ?: run {
            Log.w(TAG, "주간리포트 데일리질문 날짜를 해석하지 못해 목록에서 제외한다: raw=$date")
            return null
        }
    return WeeklyReportDailyQuestion(
        title = title,
        content = content,
        date = parsedDate,
    )
}

private const val TAG = "WeeklyReportMapper"

fun EmotionAnalysisSummaryDto.toDomain(): EmotionAnalysis =
    EmotionAnalysis(
        total = total,
        succeeded = succeeded,
        pending = pending,
        failed = failed,
    )

fun WeeklyReportEmotionDto.toDomain(): WeeklyReportEmotion =
    WeeklyReportEmotion(
        keyword = keyword,
        percentage = percentage,
    )
