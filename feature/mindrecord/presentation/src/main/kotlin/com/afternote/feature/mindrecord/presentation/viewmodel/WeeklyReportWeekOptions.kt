package com.afternote.feature.mindrecord.presentation.viewmodel

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 주차 드롭다운이 제공하는 과거 리포트 범위 (이번 주 포함 52주 = 1년).
 *
 * 종전 값 `5` 는 "한 화면에 보이는 항목 수" 를 그대로 선택지 개수로 쓴 것이라, 시안이
 * 표현한 추가 과거 리포트와 세로 스크롤을 쓸 수 없었다 (#729). **가시 항목 수는 메뉴
 * 높이가 정하고**(`WeeklyReportReviewCard`), 여기서는 선택 가능한 범위만 정한다.
 *
 * 서버에 가입일이 없어(`GET /users/me` 는 이름·이메일·전화·프로필만 준다) 계정 생성 주까지로
 * 좁힐 수 없다. 기록이 없는 주를 골라도 리포트는 0 건으로 정상 응답하므로 1년을 상한으로 뒀다.
 */
internal const val WEEK_OPTION_COUNT = 52

/**
 * 이번 주 월요일부터 과거로 [count] 주를 최신순으로 만든다.
 *
 * 첫 원소가 언제나 이번 주라 진입 시 로드 대상([WeeklyReportViewModel.init])과
 * 실패 시 폴백 기준이 된다.
 */
internal fun buildWeekOptions(
    today: LocalDate,
    count: Int = WEEK_OPTION_COUNT,
): List<WeekOption> {
    val thisMonday = today.with(DayOfWeek.MONDAY)
    return (0 until count).map { weeksAgo ->
        WeekOption(monday = thisMonday.minusWeeks(weeksAgo.toLong()))
    }
}
