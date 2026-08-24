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
        assertEquals(LocalDate.of(2026, 7, 28), resolveDateInWeekOrNull(monday, 28))
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

        assertEquals(LocalDate.of(2026, 6, 30), resolveDateInWeekOrNull(monday, 30))
        assertEquals(LocalDate.of(2026, 7, 1), resolveDateInWeekOrNull(monday, 1))
        assertEquals(LocalDate.of(2026, 7, 5), resolveDateInWeekOrNull(monday, 5))

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

        assertNull(resolveDateInWeekOrNull(monday, 26))

        val recordedDays =
            countRecordedDays(
                monday = monday,
                week = listOf(diaryDay(day = 26), diaryDay(day = 28)),
                dailyQuestionDates = listOf(LocalDate.of(2026, 7, 20)),
            )

        assertEquals(1, recordedDays)
    }

    @Test
    fun `일기가 아닌 기록도 기록일로 센다`() {
        // #590 회귀 — week[] 는 일기 외 종류도 싣는다. isDiary 로 거르면 그 날이 사라진다.
        val monday = LocalDate.of(2026, 7, 27)

        val recordedDays =
            countRecordedDays(
                monday = monday,
                week = listOf(diaryDay(day = 28, isDiary = false)),
                dailyQuestionDates = emptyList(),
            )

        assertEquals(1, recordedDays)
    }

    @Test
    fun `깊은 생각만 있는 날은 세지 않는다`() {
        // 기획에서 제거된 기능이라 서버가 week[] 에 계속 실어 보내도 앱은 없는 것으로 다룬다.
        val monday = LocalDate.of(2026, 7, 27)

        val recordedDays =
            countRecordedDays(
                monday = monday,
                week = listOf(diaryDay(day = 28, isDiary = false, countsAsRecord = false)),
                dailyQuestionDates = emptyList(),
            )

        assertEquals(0, recordedDays)
    }

    @Test
    fun `같은 날에 깊은 생각과 일기가 함께 있으면 센다`() {
        // 제외 대상은 "깊은 생각뿐인 날" 이지 그 날짜 자체가 아니다.
        val monday = LocalDate.of(2026, 7, 27)

        val recordedDays =
            countRecordedDays(
                monday = monday,
                week =
                    listOf(
                        diaryDay(day = 28, isDiary = false, countsAsRecord = false),
                        diaryDay(day = 28),
                    ),
                dailyQuestionDates = emptyList(),
            )

        assertEquals(1, recordedDays)
    }

    @Test
    fun `일기가 아닌 기록도 종류를 가리지 않고 날짜 단위로 센다`() {
        // 문구가 세는 것은 "일기를 쓴 날" 이 아니라 "마음을 기록한 날" 이다.
        val monday = LocalDate.of(2026, 7, 27)

        val recordedDays =
            countRecordedDays(
                monday = monday,
                week = listOf(diaryDay(day = 28, isDiary = false), diaryDay(day = 30)),
                dailyQuestionDates = emptyList(),
            )

        assertEquals(2, recordedDays)
    }

    @Test
    fun `week 의 비일기 기록과 같은 날 데일리질문은 1일로 접힌다`() {
        // 두 출처가 같은 날을 가리키면 중복 제거된다 — 종류를 안 가려도 이중 계산은 없다.
        val monday = LocalDate.of(2026, 7, 27)

        val recordedDays =
            countRecordedDays(
                monday = monday,
                week = listOf(diaryDay(day = 28, isDiary = false)),
                dailyQuestionDates = listOf(LocalDate.of(2026, 7, 28)),
            )

        assertEquals(1, recordedDays)
    }

    @Test
    fun `종류를 가리지 않아도 달력 점은 여전히 일기만 찍는다`() {
        // isDiary 는 집계에서 빠졌을 뿐 표시 규칙에는 그대로 남는다.
        val monday = LocalDate.of(2026, 7, 27)

        val byDate = aggregateWeekRecordsByDate(monday, listOf(diaryDay(day = 28, isDiary = false)))

        assertEquals(false, byDate.getValue(LocalDate.of(2026, 7, 28)).isDiary)
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

        val byDate = aggregateWeekRecordsByDate(monday, listOf(diaryDay(day = 28)))

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
                WeeklyReportDay(diaryId = 1, day = 28, isDiary = false, countsAsRecord = true, emotion = null),
                WeeklyReportDay(diaryId = 2, day = 28, isDiary = true, countsAsRecord = true, emotion = TodayMood.SAD),
            )

        val byDate = aggregateWeekRecordsByDate(monday, week)

        val record = byDate.getValue(LocalDate.of(2026, 7, 28))
        assertEquals(1, byDate.size)
        assertEquals(true, record.isDiary)
        assertEquals(TodayMood.SAD, record.emotion)
    }

    @Test
    fun `감정 병합은 응답 순서가 뒤집혀도 같은 결과를 낸다`() {
        // 명세에 week[] 순서 의미도 같은 날짜 복수 원소의 병합 규칙도 없다 (Afternote-BE#131).
        // 순서에 의존하면 서버가 정렬만 바꿔도 화면 이모지가 바뀐다 — diaryId 최대값으로 고정한다.
        val monday = LocalDate.of(2026, 7, 27)
        val older = WeeklyReportDay(diaryId = 1, day = 28, isDiary = true, countsAsRecord = true, emotion = TodayMood.HAPPY)
        val newer = WeeklyReportDay(diaryId = 2, day = 28, isDiary = true, countsAsRecord = true, emotion = TodayMood.SAD)

        val ascending = aggregateWeekRecordsByDate(monday, listOf(older, newer))
        val descending = aggregateWeekRecordsByDate(monday, listOf(newer, older))

        assertEquals(TodayMood.SAD, ascending.getValue(LocalDate.of(2026, 7, 28)).emotion)
        assertEquals(descending, ascending)
    }

    @Test
    fun `감정이 없는 원소가 먼저 와도 감정 있는 원소가 선택된다`() {
        val monday = LocalDate.of(2026, 7, 27)
        val week =
            listOf(
                WeeklyReportDay(diaryId = 9, day = 28, isDiary = true, countsAsRecord = true, emotion = null),
                WeeklyReportDay(diaryId = 1, day = 28, isDiary = false, countsAsRecord = true, emotion = TodayMood.HAPPY),
            )

        val byDate = aggregateWeekRecordsByDate(monday, week)

        assertEquals(TodayMood.HAPPY, byDate.getValue(LocalDate.of(2026, 7, 28)).emotion)
    }

    @Test
    fun `주 범위 밖 일자는 어느 칸에도 붙지 않는다`() {
        val monday = LocalDate.of(2026, 7, 27)

        val byDate = aggregateWeekRecordsByDate(monday, listOf(diaryDay(day = 26), diaryDay(day = 28)))

        assertEquals(setOf(LocalDate.of(2026, 7, 28)), byDate.keys)
    }

    private fun diaryDay(
        day: Int,
        isDiary: Boolean = true,
        countsAsRecord: Boolean = true,
    ): WeeklyReportDay =
        WeeklyReportDay(
            diaryId = day.toLong(),
            day = day,
            isDiary = isDiary,
            countsAsRecord = countsAsRecord,
            emotion = TodayMood.HAPPY,
        )
}
