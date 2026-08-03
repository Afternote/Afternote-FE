package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.domain.model.WeeklyReportDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class WeeklyReportRecordedDaysTest {
    @Test
    fun `sparse week 원소는 index 가 아니라 day 로 날짜가 복원된다`() {
        // week 는 기록이 있는 날만 담겨 오므로 index 0 이 월요일을 뜻하지 않는다.
        val monday = LocalDate.of(2026, 7, 27)
        val week = listOf(diaryDay(day = 28))

        val recordedDays =
            countRecordedDays(
                monday = monday,
                week = week,
                dailyQuestionDates = emptyList(),
            )

        assertEquals(1, recordedDays)
        assertEquals(LocalDate.of(2026, 7, 28), resolveDateInWeek(monday, 28))
    }

    @Test
    fun `같은 날 일기와 데일리질문은 1일로 합쳐진다`() {
        // 리뷰 #524 회귀 시나리오 — 2026-07-28(화) 일기 1건 + 데일리질문 1건 → 1일.
        val monday = LocalDate.of(2026, 7, 27)

        val recordedDays =
            countRecordedDays(
                monday = monday,
                week = listOf(diaryDay(day = 28)),
                dailyQuestionDates = listOf(LocalDate.of(2026, 7, 28)),
            )

        assertEquals(1, recordedDays)
    }

    @Test
    fun `서로 다른 날의 일기와 데일리질문은 각각 세어진다`() {
        val monday = LocalDate.of(2026, 7, 27)

        val recordedDays =
            countRecordedDays(
                monday = monday,
                week = listOf(diaryDay(day = 28), diaryDay(day = 30)),
                dailyQuestionDates = listOf(LocalDate.of(2026, 7, 29)),
            )

        assertEquals(3, recordedDays)
    }

    @Test
    fun `월 경계를 넘는 주에도 day 가 올바른 달로 복원된다`() {
        // 2026-06-29(월) ~ 2026-07-05(일) — day=1 은 6월 1일이 아니라 7월 1일이다.
        val monday = LocalDate.of(2026, 6, 29)

        assertEquals(LocalDate.of(2026, 6, 30), resolveDateInWeek(monday, 30))
        assertEquals(LocalDate.of(2026, 7, 1), resolveDateInWeek(monday, 1))
        assertEquals(LocalDate.of(2026, 7, 5), resolveDateInWeek(monday, 5))

        val recordedDays =
            countRecordedDays(
                monday = monday,
                week = listOf(diaryDay(day = 30), diaryDay(day = 1)),
                dailyQuestionDates = listOf(LocalDate.of(2026, 7, 1)),
            )

        assertEquals(2, recordedDays)
    }

    @Test
    fun `주 범위 밖 일자는 집계에서 제외된다`() {
        val monday = LocalDate.of(2026, 7, 27)

        assertNull(resolveDateInWeek(monday, 26))

        val recordedDays =
            countRecordedDays(
                monday = monday,
                week = listOf(diaryDay(day = 26), diaryDay(day = 28)),
                dailyQuestionDates = listOf(LocalDate.of(2026, 7, 20)),
            )

        assertEquals(1, recordedDays)
    }

    @Test
    fun `isDiary 가 false 인 원소는 일기 기록일로 세지 않는다`() {
        val monday = LocalDate.of(2026, 7, 27)

        val recordedDays =
            countRecordedDays(
                monday = monday,
                week = listOf(diaryDay(day = 28, isDiary = false)),
                dailyQuestionDates = emptyList(),
            )

        assertEquals(0, recordedDays)
    }

    @Test
    fun `빈 week 응답은 데일리질문만으로 집계된다`() {
        val monday = LocalDate.of(2026, 7, 27)

        val recordedDays =
            countRecordedDays(
                monday = monday,
                week = emptyList(),
                dailyQuestionDates = listOf(LocalDate.of(2026, 7, 28), LocalDate.of(2026, 7, 28)),
            )

        assertEquals(1, recordedDays)
    }

    @Test
    fun `sparse week 는 index 가 아니라 날짜에 매핑된다`() {
        // #563 회귀 — 2026-07-28(화) 기록 1건. index 매칭이면 월요일 칸에 들어갔다.
        val monday = LocalDate.of(2026, 7, 27)

        val byDate = indexWeekByDate(monday, listOf(diaryDay(day = 28)))

        assertEquals(setOf(LocalDate.of(2026, 7, 28)), byDate.keys)
        assertEquals(true, byDate.getValue(LocalDate.of(2026, 7, 28)).isDiary)
        assertNull(byDate[monday])
    }

    @Test
    fun `같은 일자가 여러 원소로 와도 한 칸으로 합쳐진다`() {
        // 명세 예시가 day=10 을 isDiary true·false 두 원소로 내려준다 — 칸은 하나뿐이다.
        val monday = LocalDate.of(2026, 7, 27)
        val week =
            listOf(
                WeeklyReportDay(diaryId = 1, day = 28, isDiary = false, emotion = null),
                WeeklyReportDay(diaryId = 2, day = 28, isDiary = true, emotion = TodayMood.SAD),
            )

        val byDate = indexWeekByDate(monday, week)

        val record = byDate.getValue(LocalDate.of(2026, 7, 28))
        assertEquals(1, byDate.size)
        assertEquals(true, record.isDiary)
        assertEquals(TodayMood.SAD, record.emotion)
    }

    @Test
    fun `주 범위 밖 일자는 어느 칸에도 붙지 않는다`() {
        val monday = LocalDate.of(2026, 7, 27)

        val byDate = indexWeekByDate(monday, listOf(diaryDay(day = 26), diaryDay(day = 28)))

        assertEquals(setOf(LocalDate.of(2026, 7, 28)), byDate.keys)
    }

    private fun diaryDay(
        day: Int,
        isDiary: Boolean = true,
    ): WeeklyReportDay =
        WeeklyReportDay(
            diaryId = day.toLong(),
            day = day,
            isDiary = isDiary,
            emotion = TodayMood.HAPPY,
        )
}
