package com.afternote.feature.mindrecord.presentation.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import com.afternote.feature.mindrecord.domain.model.DailyQuestion as DailyQuestionDomain

/**
 * 날짜 파싱 실패가 **오늘 날짜로 둔갑하지 않는지** 고정한다 (#751).
 *
 * 종전에는 `parseLocalDateOrNull(raw) ?: LocalDate.now()` 라, 서버가 형식을 바꾸면 지난 주
 * 답변이 오늘 날짜로 표시되고 실패 신호가 어디에도 남지 않았다. "날짜가 이상하다" 는 제보를
 * 받아도 재현 지점을 찾을 수 없는 종류다.
 *
 * `Log.w` 를 타므로 Robolectric 으로 돌린다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DailyQuestionDateFallbackTest {
    @Test
    fun `날짜를 해석할 수 없으면 오늘로 메우지 않고 항목을 버린다`() {
        val ui = dailyQuestion(createdAt = "언젠가").toUi()

        assertNull(ui)
    }

    @Test
    fun `빈 날짜도 오늘로 메우지 않는다`() {
        val ui = dailyQuestion(createdAt = "").toUi()

        assertNull(ui)
    }

    @Test
    fun `요일이 붙은 서버 형식은 그대로 해석한다`() {
        val ui = dailyQuestion(createdAt = "2026.07.29 수").toUi()

        assertNotNull(ui)
        assertEquals(LocalDate.of(2026, 7, 29), ui!!.date)
    }

    @Test
    fun `ISO 형식도 해석한다`() {
        val ui = dailyQuestion(createdAt = "2026-07-29").toUi()

        assertEquals(LocalDate.of(2026, 7, 29), ui!!.date)
    }

    private fun dailyQuestion(createdAt: String) =
        DailyQuestionDomain(
            dailyQuestionId = 1L,
            title = "질문",
            content = "답변",
            createdAt = createdAt,
            isDraft = false,
        )
}
