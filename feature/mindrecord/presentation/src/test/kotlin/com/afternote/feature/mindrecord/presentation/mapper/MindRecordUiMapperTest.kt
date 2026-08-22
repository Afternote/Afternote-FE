package com.afternote.feature.mindrecord.presentation.mapper

import com.afternote.feature.mindrecord.domain.model.Diary
import com.afternote.feature.mindrecord.domain.model.TodayMood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * [Diary.toUi] 날짜 해석 계약 가드 (#699).
 *
 * 캘린더에 찍히는 값은 사용자가 고른 `date` 이고 `createdAt`(생성 시각)은 폴백이다.
 * 둘 다 없으면 오늘로 메우지 않고 항목을 뺀다 — 메우면 날짜 없는 항목이 오늘 칸에
 * 정상 기록처럼 앉아 서버 필드 누락을 감춘다.
 */
class MindRecordUiMapperTest {
    @Test
    fun `date 가 있으면 createdAt 이 아니라 date 를 쓴다`() {
        val ui = diary(date = "2026-03-21", createdAt = "2026-03-25T20:13:42").toUi()

        assertEquals(LocalDate.of(2026, 3, 21), ui?.date)
    }

    @Test
    fun `date 가 없으면 createdAt 으로 폴백한다`() {
        val ui = diary(date = null, createdAt = "2026.03.25 수").toUi()

        assertEquals(LocalDate.of(2026, 3, 25), ui?.date)
    }

    @Test
    fun `ISO 날짜시각 createdAt 도 파싱된다`() {
        // Swagger DiaryResponse 예시가 `createdAt: "2026-03-25T20:13:42"` 형태다.
        val ui = diary(date = null, createdAt = "2026-03-25T20:13:42").toUi()

        assertEquals(LocalDate.of(2026, 3, 25), ui?.date)
    }

    @Test
    fun `두 날짜가 모두 없으면 오늘로 메우지 않고 항목을 뺀다`() {
        // createdAt 은 이 PR 에서 기본값 ""를 얻었으므로 둘 다 비는 응답이 실제로 가능하다.
        assertNull(diary(date = null, createdAt = "").toUi())
    }

    @Test
    fun `해석할 수 없는 날짜 문자열도 항목을 뺀다`() {
        assertNull(diary(date = "알 수 없음", createdAt = "").toUi())
    }

    private fun diary(
        date: String?,
        createdAt: String,
    ) = Diary(
        diaryId = 1L,
        title = "제목",
        content = "본문",
        date = date,
        createdAt = createdAt,
        todayMood = TodayMood.HAPPY,
    )
}
