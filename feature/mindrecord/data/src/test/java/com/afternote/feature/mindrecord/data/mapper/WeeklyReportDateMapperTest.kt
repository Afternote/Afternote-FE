package com.afternote.feature.mindrecord.data.mapper

import com.afternote.feature.mindrecord.data.dto.WeeklyReportDailyQuestionDto
import com.afternote.feature.mindrecord.data.dto.WeeklyReportDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 서버 날짜 해석과 실패 정책이 DTO→도메인 경계에 있다는 계약 (#547).
 *
 * 종전에는 이 지식이 `WeeklyReportViewModel` 에 있었고, 실패 처리도 소비처마다 갈렸다 —
 * 집계 경로는 버리고 표시 경로는 `LocalDate.now()` 로 메워 파싱 못 한 기록이 **오늘
 * 작성한 것처럼** 카드에 앉았다. 이제 한 곳에서 정한다: **해석 못 하면 제외.**
 *
 * 단건 매퍼(`toDomainOrNull`)가 아니라 **공개 소유자인 [WeeklyReportDto.toDomain] 을 통해**
 * 본다. 날짜 해석은 그 자체가 목적이 아니라 «주간 리포트의 데일리질문 목록에 무엇이 남는가»
 * 의 일부라, 목록에서 빠지는 것까지가 계약이다 (#1674).
 */
class WeeklyReportDateMapperTest {
    @Test
    fun `요일이 붙은 서버 기본 포맷을 해석한다`() {
        // 실서버가 이 형태로 내려준다 (2026-08-23 실측).
        assertEquals(listOf(LocalDate.of(2026, 8, 23)), dates("2026.08.23 일"))
    }

    @Test
    fun `ISO 날짜도 해석한다`() {
        assertEquals(listOf(LocalDate.of(2026, 3, 21)), dates("2026-03-21"))
    }

    @Test
    fun `시각이 붙은 ISO 도 해석한다`() {
        // ISO_DATE 만 두면 뒤가 남아 실패한다 — 세 포맷을 모두 허용하는 이유다.
        assertEquals(listOf(LocalDate.of(2026, 3, 25)), dates("2026-03-25T20:13:42"))
    }

    @Test
    fun `해석하지 못한 날짜는 오늘로 메우지 않고 목록에서 뺀다`() {
        // 오늘로 메우면 그 기록이 오늘 작성한 것처럼 HISTORY 카드에 앉아 이상을 감춘다.
        assertTrue(report("알 수 없음").dailyQuestions.isEmpty())
        assertTrue(report("").dailyQuestions.isEmpty())
    }

    @Test
    fun `제외돼도 나머지 항목은 살아남는다`() {
        // 실패의 폭이 목록 전체가 되면 안 된다.
        assertEquals(
            listOf(LocalDate.of(2026, 8, 23), LocalDate.of(2026, 8, 22)),
            dates("2026.08.23 일", "깨진 값", "2026-08-22"),
        )
    }

    @Test
    fun `제목과 본문은 그대로 통과한다`() {
        val question = report("2026.08.23 일").dailyQuestions.single()

        assertEquals("질문", question.title)
        assertEquals("답변", question.content)
    }

    private fun dates(vararg rawDates: String) = report(*rawDates).dailyQuestions.map { it.date }

    private fun report(vararg rawDates: String) =
        WeeklyReportDto(
            dailyQuestionAmount = rawDates.size,
            diaryAmount = 0,
            summaryText = "요약",
            week = emptyList(),
            dailyQuestions =
                rawDates.map { date ->
                    WeeklyReportDailyQuestionDto(title = "질문", content = "답변", date = date)
                },
            emotions = emptyList(),
        ).toDomain()
}
