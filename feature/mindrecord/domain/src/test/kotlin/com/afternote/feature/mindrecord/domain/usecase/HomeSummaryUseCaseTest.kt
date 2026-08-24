package com.afternote.feature.mindrecord.domain.usecase

import com.afternote.feature.mindrecord.domain.model.DeepThought
import com.afternote.feature.mindrecord.domain.model.EmotionAnalysis
import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.repository.DeepThoughtRepository
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `이번 주 기록 수는 세 종류의 합이다`() =
        runTest {
            val repository = FakeWeeklyReportRepository(report(daily = 2, diary = 3, deep = 1))

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

    @Test
    fun `최근 깊은 생각은 id 가 가장 큰 항목이다`() =
        runTest {
            // 목록 순서는 명세에 없다 — 서버 정렬이 바뀌어도 "최근" 이 흔들리지 않게 한다.
            val repository =
                FakeDeepThoughtRepository(
                    listOf(
                        DeepThought(id = 3, createdAt = "2026.08.22 토", title = "먼저 쓴 글"),
                        DeepThought(id = 9, createdAt = "2026.08.24 월", title = "최근 글"),
                        DeepThought(id = 7, createdAt = "2026.08.23 일", title = "중간 글"),
                    ),
                )

            val recent = GetRecentDeepThoughtUseCase(repository)()

            assertEquals(9L, recent.getOrNull()?.id)
            assertEquals("최근 글", recent.getOrNull()?.title)
        }

    @Test
    fun `한 건도 없으면 null 이다`() =
        runTest {
            val recent = GetRecentDeepThoughtUseCase(FakeDeepThoughtRepository(emptyList()))()

            assertTrue(recent.isSuccess)
            assertNull(recent.getOrNull())
        }

    @Test
    fun `조회 실패는 빈 결과로 덮지 않는다`() =
        runTest {
            // 실패를 null 로 내리면 호출부가 "깊은 생각이 없다" 로 그려 실패가 사라진다.
            val recent = GetRecentDeepThoughtUseCase(FakeDeepThoughtRepository(null))()

            assertTrue(recent.isFailure)
        }

    // ── 테스트 도구 ───────────────────────────────────────────────────────────

    private fun report(
        daily: Int = 0,
        diary: Int = 0,
        deep: Int = 0,
    ) = WeeklyReport(
        dailyQuestionAmount = daily,
        diaryAmount = diary,
        deepThoughtAmount = deep,
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

    private class FakeDeepThoughtRepository(
        private val list: List<DeepThought>?,
    ) : DeepThoughtRepository {
        override suspend fun getList(draftOnly: Boolean?): Result<List<DeepThought>> =
            list?.let { Result.success(it) } ?: Result.failure(IllegalStateException("timeout"))
    }
}
