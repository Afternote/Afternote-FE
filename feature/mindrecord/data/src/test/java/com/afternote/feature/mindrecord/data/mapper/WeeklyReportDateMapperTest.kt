package com.afternote.feature.mindrecord.data.mapper

import com.afternote.feature.mindrecord.data.dto.WeeklyReportDailyQuestionDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * 서버 날짜 해석과 실패 정책이 DTO→도메인 경계에 있다는 계약 (#547).
 *
 * 종전에는 이 지식이 `WeeklyReportViewModel` 에 있었고, 실패 처리도 소비처마다 갈렸다 —
 * 집계 경로는 버리고 표시 경로는 `LocalDate.now()` 로 메워 파싱 못 한 기록이 **오늘
 * 작성한 것처럼** 카드에 앉았다. 이제 한 곳에서 정한다: **해석 못 하면 제외.**
 */
class WeeklyReportDateMapperTest {
    private fun dto(date: String) = WeeklyReportDailyQuestionDto(title = "질문", content = "답변", date = date)

    @Test
    fun `요일이 붙은 서버 기본 포맷을 해석한다`() {
        // 실서버가 이 형태로 내려준다 (2026-08-23 실측).
        assertEquals(LocalDate.of(2026, 8, 23), dto("2026.08.23 일").toDomainOrNull()?.date)
    }

    @Test
    fun `ISO 날짜도 해석한다`() {
        assertEquals(LocalDate.of(2026, 3, 21), dto("2026-03-21").toDomainOrNull()?.date)
    }

    @Test
    fun `시각이 붙은 ISO 도 해석한다`() {
        // ISO_DATE 만 두면 뒤가 남아 실패한다 — 세 포맷을 모두 허용하는 이유다.
        assertEquals(LocalDate.of(2026, 3, 25), dto("2026-03-25T20:13:42").toDomainOrNull()?.date)
    }

    @Test
    fun `해석하지 못한 날짜는 오늘로 메우지 않고 제외한다`() {
        // 오늘로 메우면 그 기록이 오늘 작성한 것처럼 HISTORY 카드에 앉아 이상을 감춘다.
        assertNull(dto("알 수 없음").toDomainOrNull())
        assertNull(dto("").toDomainOrNull())
    }

    @Test
    fun `제외돼도 나머지 항목은 살아남는다`() {
        // 실패의 폭이 목록 전체가 되면 안 된다.
        val kept =
            listOf(dto("2026.08.23 일"), dto("깨진 값"), dto("2026-08-22"))
                .mapNotNull { it.toDomainOrNull() }

        assertEquals(
            listOf(LocalDate.of(2026, 8, 23), LocalDate.of(2026, 8, 22)),
            kept.map { it.date },
        )
    }

    @Test
    fun `제목과 본문은 그대로 통과한다`() {
        val domain = dto("2026.08.23 일").toDomainOrNull()

        assertEquals("질문", domain?.title)
        assertEquals("답변", domain?.content)
    }
}
