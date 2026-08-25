package com.afternote.feature.mindrecord.presentation.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 주차 선택지 범위 회귀 가드 (#729).
 *
 * 종전에는 "한 화면에 보이는 항목 수" 5 를 그대로 선택지 개수로 써서, 시안이 표현한
 * 추가 과거 리포트와 세로 스크롤을 쓸 수 없었다. 가시 항목 수와 선택 가능한 범위를
 * 분리했다는 사실을 여기서 고정한다.
 */
class WeeklyReportWeekOptionsTest {
    @Test
    fun `선택지는 한 화면 가시 항목 수보다 많다`() {
        // 5 로 되돌리면 여기서 먼저 깨진다 — 스크롤할 항목이 없어지는 지점이다.
        assertTrue("선택지가 가시 항목 수(5)를 넘어야 스크롤이 생긴다", WEEK_OPTION_COUNT > 5)
        assertEquals(52, WEEK_OPTION_COUNT)
    }

    @Test
    fun `이번 주 월요일부터 과거로 최신순으로 만든다`() {
        // 2026-08-23 은 일요일 — 그 주 월요일은 2026-08-17 이다.
        val options = buildWeekOptions(today = LocalDate.of(2026, 8, 23))

        assertEquals(WEEK_OPTION_COUNT, options.size)
        assertEquals(LocalDate.of(2026, 8, 17), options.first().monday)
        assertTrue(options.all { it.monday.dayOfWeek == DayOfWeek.MONDAY })
        assertEquals(
            options.map { it.monday }.sortedDescending(),
            options.map { it.monday },
        )
    }

    @Test
    fun `최하단 선택지는 1년 전 주다`() {
        val options = buildWeekOptions(today = LocalDate.of(2026, 8, 23))

        assertEquals(LocalDate.of(2026, 8, 17).minusWeeks(51), options.last().monday)
    }

    @Test
    fun `월요일에 들어와도 그 주가 첫 선택지다`() {
        // `with(DayOfWeek.MONDAY)` 는 그 주의 월요일이라 월요일 당일이면 자기 자신이다.
        val options = buildWeekOptions(today = LocalDate.of(2026, 8, 17))

        assertEquals(LocalDate.of(2026, 8, 17), options.first().monday)
    }
}
