package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.feature.mindrecord.domain.model.WeeklyReportDay
import java.time.LocalDate

internal const val WEEK_LENGTH = 7

/**
 * `week[]` 는 월~일 7칸이 아니라 **기록이 있는 날만** 담겨 오는 sparse 배열이라
 * index 가 요일 오프셋이 아니다. 각 원소는 일자(`day`, 1~31)만 들고 있으므로
 * 월 경계를 넘는 주에는 monday 의 월을 그대로 붙일 수도 없다.
 *
 * 해당 주(월~일) 안에서 일자가 일치하는 날짜를 찾는다 — 7일 창 안에서 일자는 유일하다.
 * 범위 밖 일자면 null 을 돌려 집계에서 제외한다.
 */
internal fun resolveDateInWeek(
    monday: LocalDate,
    dayOfMonth: Int,
): LocalDate? =
    (0 until WEEK_LENGTH)
        .map { monday.plusDays(it.toLong()) }
        .firstOrNull { it.dayOfMonth == dayOfMonth }

/**
 * 기록일수 = 일기가 있는 날 + 주간 범위 내 데일리질문 답변 날짜의 합집합(중복 제거).
 *
 * 두 출처를 모두 `LocalDate` 로 복원한 뒤 합쳐야 같은 날의 일기·데일리질문이
 * `distinct()` 로 1일로 접힌다.
 */
internal fun countRecordedDays(
    monday: LocalDate,
    week: List<WeeklyReportDay>,
    dailyQuestionDates: List<LocalDate>,
): Int {
    val sunday = monday.plusDays(WEEK_LENGTH - 1L)
    val diaryDates =
        week.mapNotNull { day ->
            day.takeIf { it.isDiary }?.let { resolveDateInWeek(monday, it.day) }
        }
    val questionDatesInWeek = dailyQuestionDates.filter { it in monday..sunday }
    return (diaryDates + questionDatesInWeek).distinct().size
}
