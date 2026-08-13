package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.feature.mindrecord.domain.model.TodayMood
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
internal fun resolveDateInWeekOrNull(
    monday: LocalDate,
    dayOfMonth: Int,
): LocalDate? =
    (0 until WEEK_LENGTH)
        .map { monday.plusDays(it.toLong()) }
        .firstOrNull { it.dayOfMonth == dayOfMonth }

/**
 * 한 날짜에 해당하는 `week[]` 원소들을 하나로 접은 결과. 주 정보는 담지 않는다 —
 * 어느 주에 속하는지는 이 타입을 담는 `Map<LocalDate, _>` 의 키가 안다.
 *
 * 서버는 같은 일자를 여러 원소로 내려줄 수 있다 (명세 예시의 `day:10` 2건 — 하나는
 * `isDiary:true`, 하나는 `false`). 날짜 하나에 칸도 하나이므로 여기서 합쳐 둔다.
 */
internal data class DailyRecordSummary(
    val isDiary: Boolean,
    val emotion: TodayMood?,
)

/**
 * sparse 한 `week[]` 를 **일자 매칭**으로 날짜별 레코드에 접는다.
 *
 * 배열 index 는 요일 오프셋이 아니므로 위치로 매칭하면 기록이 엉뚱한 요일에 찍히고,
 * 나머지 칸이 달력 날짜로 채워지며 같은 날짜가 두 번 보인다 (#563).
 * 주 범위 밖 일자는 [resolveDateInWeekOrNull] 이 null 을 돌려 자연히 제외된다.
 *
 * 병합 규칙은 **응답 순서에 의존하지 않는다**. 같은 날짜에 감정이 둘 실려 와도 서버가
 * 정렬을 바꾸는 것만으로 화면 이모지가 바뀌면 안 되므로, `diaryId` 가 가장 큰 원소의
 * 감정을 고정 규칙으로 쓴다 — 같은 응답이면 항상 같은 결과가 나온다.
 * (같은 일자 복수 원소의 병합 규칙은 명세에 없어 Afternote-BE#131 로 확정을 요청해 둔 상태다.)
 */
internal fun aggregateWeekRecordsByDate(
    monday: LocalDate,
    week: List<WeeklyReportDay>,
): Map<LocalDate, DailyRecordSummary> =
    week
        .mapNotNull { day -> resolveDateInWeekOrNull(monday, day.day)?.let { date -> date to day } }
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })
        .mapValues { (_, days) ->
            DailyRecordSummary(
                isDiary = days.any { it.isDiary },
                emotion =
                    days
                        .filter { it.emotion != null }
                        .maxByOrNull { it.diaryId }
                        ?.emotion,
            )
        }

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
        aggregateWeekRecordsByDate(monday, week)
            .filterValues { it.isDiary }
            .keys
    val questionDatesInWeek = dailyQuestionDates.filter { it in monday..sunday }
    return (diaryDates + questionDatesInWeek).distinct().size
}
