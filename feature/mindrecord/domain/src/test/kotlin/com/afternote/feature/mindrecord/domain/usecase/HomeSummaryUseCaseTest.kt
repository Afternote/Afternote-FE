package com.afternote.feature.mindrecord.domain.usecase

import com.afternote.feature.mindrecord.domain.model.EmotionAnalysis
import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 발신자 홈 `WeeklySummaryGrid` 데이터 소스 (#207).
 *
 * 세 카드가 모두 목데이터였고 mindrecord 도메인에 UseCase 가 0건이었다. 여기서 고정하는
 * 계약은 두 가지다 — **주간 기록 수는 세 종류의 합**, **최근 깊은 생각은 1건 또는 없음**.
 */
class HomeSummaryUseCaseTest {
    @Test
    fun `이번 주 기록 수는 일기와 데일리질문의 합이다`() =
        runTest {
            val repository = FakeWeeklyReportRepository(report(daily = 2, diary = 4))

            val count = GetWeeklyRecordCountUseCase(repository)(today = LocalDate.of(2026, 8, 24))

            assertEquals(6, count.getOrNull())
        }

    @Test
    fun `조회 기준일은 그 주 월요일이다`() =
        runTest {
            // 서버는 주어진 날이 속한 주를 돌려준다 — 일요일에 열어도 같은 주를 봐야 한다.
            val repository = FakeWeeklyReportRepository(report())

            GetWeeklyRecordCountUseCase(repository)(today = LocalDate.of(2026, 8, 30))

            assertEquals("2026-08-24", repository.requestedDate)
        }

    @Test
    fun `주간 조회 실패는 0 으로 덮지 않는다`() =
        runTest {
            // 0 으로 내리면 "이번 주에 아무것도 안 썼다" 와 구분되지 않는다.
            val repository = FakeWeeklyReportRepository(null)

            val count = GetWeeklyRecordCountUseCase(repository)()

            assertTrue(count.isFailure)
        }

    // ── 테스트 도구 ───────────────────────────────────────────────────────────

    private fun report(
        daily: Int = 0,
        diary: Int = 0,
    ) = WeeklyReport(
        dailyQuestionAmount = daily,
        diaryAmount = diary,
        summaryText = "",
        week = emptyList(),
        dailyQuestions = emptyList(),
        emotions = emptyList(),
        emotionAnalysis = EmotionAnalysis(total = 0, succeeded = 0, pending = 0, failed = 0),
    )

    private class FakeWeeklyReportRepository(
        private val report: WeeklyReport?,
    ) : WeeklyReportRepository {
        var requestedDate: String? = null

        override suspend fun getWeeklyReport(date: String): Result<WeeklyReport> {
            requestedDate = date
            return report?.let { Result.success(it) } ?: Result.failure(IllegalStateException("timeout"))
        }
    }
}
