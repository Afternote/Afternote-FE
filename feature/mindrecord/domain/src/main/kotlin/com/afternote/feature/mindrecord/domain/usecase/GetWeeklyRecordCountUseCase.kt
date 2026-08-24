package com.afternote.feature.mindrecord.domain.usecase

import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * 이번 주 기록 수 — 홈 `WeeklySummaryGrid` 의 THIS WEEK 카드 (#207).
 *
 * 일기·데일리질문·깊은 생각을 **각각 세지 않는다.** 주간리포트 API 가 두 카운트를 한
 * 응답에 담아 주므로 그 합을 쓴다 — 종류별로 목록을 부르면 요청이 셋으로 늘고, 화면이
 * 쓰지도 않을 본문까지 받아 온다.
 *
 * 조회 기준일은 **이번 주 월요일**이다. 서버가 그 날이 속한 주를 돌려준다.
 */
class GetWeeklyRecordCountUseCase
    @Inject
    constructor(
        private val weeklyReportRepository: WeeklyReportRepository,
    ) {
        suspend operator fun invoke(today: LocalDate = LocalDate.now()): Result<Int> =
            weeklyReportRepository
                .getWeeklyReport(date = today.with(DayOfWeek.MONDAY).format(API_DATE_FORMATTER))
                .map { it.totalRecordAmount }

        private companion object {
            val API_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        }
    }
