package com.afternote.feature.mindrecord.presentation.usecase

import com.afternote.core.domain.repository.MyProfileRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.feature.mindrecord.domain.model.EmotionAnalysis
import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

/**
 * 주간 리포트 **조회·제한 폴링 정책** 가드 (#725 · #1693).
 *
 * 종전에는 이 규칙이 ViewModel 안에 있어 「몇 번·언제 조회하는가」를 화면을 띄우지 않고는
 * 볼 수 없었다. 폴링은 8초 간격이라 실시간으로 기다리면 한 번에 64초다 — `runTest` 의
 * 가상 시간으로 돌린다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveWeeklyReportUseCaseTest {
    private val thisMonday: LocalDate = LocalDate.of(2026, 3, 2)
    private val today: LocalDate = thisMonday.plusDays(3)

    @Test
    fun `첫 방출은 리포트와 프로필 이름을 함께 싣는다`() =
        runTest {
            val useCase = useCase(responses = listOf(Result.success(completed())), name = "채연")

            val first = useCase.observe(thisMonday, today).toList().first()

            assertEquals("채연", first.getOrNull()?.profileName)
            assertEquals(completed(), first.getOrNull()?.report)
        }

    /** 조회가 안 되는데 8초마다 다시 부를 이유가 없다. */
    @Test
    fun `첫 조회가 실패하면 폴링하지 않고 끝난다`() =
        runTest {
            val repository = QueuedWeeklyReportRepository(listOf(Result.failure(IOException("조회 실패"))))
            val useCase = ObserveWeeklyReportUseCase(repository, FakeUserRepository())

            val emissions = useCase.observe(thisMonday, today).toList()

            assertEquals(1, emissions.size)
            assertTrue(emissions.single().isFailure)
            assertEquals(1, repository.requestedDates.size)
        }

    /**
     * 분석은 완료 신호를 주는 채널이 없어 재조회가 유일한 확인 수단이다 (#725).
     * 완료된 순간 **바로 멈춰야** 한다 — 남은 시도를 마저 돌면 쓸데없는 조회가 나간다.
     */
    @Test
    fun `분석이 끝나면 그 즉시 폴링을 멈춘다`() =
        runTest {
            val repository =
                QueuedWeeklyReportRepository(
                    listOf(
                        Result.success(pending()),
                        Result.success(pending()),
                        Result.success(completed()),
                    ),
                )
            val useCase = ObserveWeeklyReportUseCase(repository, FakeUserRepository())

            val emissions = useCase.observe(thisMonday, today).toList()

            // 첫 조회 + 폴링 2회 = 3
            assertEquals(3, repository.requestedDates.size)
            assertEquals(3, emissions.size)
            assertEquals(completed(), emissions.last().getOrNull()?.report)
        }

    /**
     * 무한 폴링을 하지 않는다 — 상한을 넘기면 화면 이탈·복귀나 재시도에 맡긴다.
     *
     * 기대값을 **숫자로 적는다.** `1 + POLL_ATTEMPTS` 로 쓰면 상수가 바뀔 때 기대값도 함께
     * 움직여 어떤 값이든 통과한다 — 상한을 지키는 테스트가 상한을 안 보게 된다.
     * 8초 × 8회 = 64초가 「화면에 머무는 동안」의 상한이라는 판단이 여기 박혀 있어야,
     * 그 값을 바꾸는 사람이 이 테스트에서 한 번 멈춘다.
     */
    @Test
    fun `분석이 끝나지 않아도 여덟 번에서 멈춘다`() =
        runTest {
            val repository = QueuedWeeklyReportRepository(alwaysPending = true)
            val useCase = ObserveWeeklyReportUseCase(repository, FakeUserRepository())

            useCase.observe(thisMonday, today).toList()

            // 요청 횟수와 흐른 시간이 곧 상한이다 — 상수를 따로 단언하면 프로덕션 visibility 를
            // 테스트 때문에 넓히게 된다 (#1688·#1693 리뷰).
            assertEquals("첫 조회 1회 + 폴링 8회", 9, repository.requestedDates.size)
            assertEquals("8초 × 8회 = 64초", 64_000L, testScheduler.currentTime)
        }

    /**
     * 폴링의 근거는 「저장 직후 비동기 분석」이라 **이번 주에만** 성립한다. 지난 주를 보는
     * 동안 8초마다 조회가 나갈 이유가 없다.
     */
    @Test
    fun `지난 주는 진행 중이어도 폴링하지 않는다`() =
        runTest {
            val repository = QueuedWeeklyReportRepository(alwaysPending = true)
            val useCase = ObserveWeeklyReportUseCase(repository, FakeUserRepository())

            val emissions = useCase.observe(thisMonday.minusWeeks(1), today).toList()

            assertEquals(1, repository.requestedDates.size)
            assertEquals(1, emissions.size)
        }

    /**
     * `PENDING` 화면에는 재시도 수단이 없다(카드는 `FAILED` 전용). 한 번 실패했다고 남은
     * 시도를 버리면 **화면에 머무는 동안 복구할 길이 사라진다.**
     */
    @Test
    fun `폴링 중 한 번 실패해도 남은 시도를 계속한다`() =
        runTest {
            val repository =
                QueuedWeeklyReportRepository(
                    listOf(
                        Result.success(pending()),
                        Result.failure(IOException("일시 실패")),
                        Result.success(completed()),
                    ),
                )
            val useCase = ObserveWeeklyReportUseCase(repository, FakeUserRepository())

            val emissions = useCase.observe(thisMonday, today).toList()

            assertEquals(3, repository.requestedDates.size)
            // 실패한 시도는 방출하지 않는다 — 화면을 오류로 덮으면 진행 중 표시가 사라진다.
            assertEquals(2, emissions.size)
            assertEquals(completed(), emissions.last().getOrNull()?.report)
            assertTrue("일시 실패가 화면까지 올라갔다", emissions.none { it.isFailure })
        }

    /** 사용자가 다른 주를 고르면 수집이 끊긴다. 그것을 「조회 실패」로 바꾸면 안 된다. */
    @Test
    fun `취소는 실패 방출로 바뀌지 않는다`() =
        runTest {
            val blocked = CompletableDeferred<Unit>()
            val repository =
                object : WeeklyReportRepository {
                    override suspend fun getWeeklyReport(date: String): Result<WeeklyReport> {
                        blocked.await()
                        return Result.success(completed())
                    }
                }
            val emissions = mutableListOf<Result<ObserveWeeklyReportUseCase.Snapshot>>()
            var cancelled = false

            val job =
                launch {
                    try {
                        ObserveWeeklyReportUseCase(repository, FakeUserRepository())
                            .observe(thisMonday, today)
                            .toList(emissions)
                    } catch (e: CancellationException) {
                        cancelled = true
                        throw e
                    }
                }
            advanceUntilIdle()
            job.cancelAndJoin()

            assertTrue("취소가 CancellationException 으로 전파돼야 한다", cancelled)
            assertEquals("취소가 방출로 흡수됐다: $emissions", emptyList<Any>(), emissions)
        }

    private fun useCase(
        responses: List<Result<WeeklyReport>>,
        name: String,
    ): ObserveWeeklyReportUseCase =
        ObserveWeeklyReportUseCase(
            repository = QueuedWeeklyReportRepository(responses),
            userRepository = userRepositoryNamed(name),
        )

    private fun userRepositoryNamed(name: String): MyProfileRepository = FakeUserRepository().apply { profile = profile.copy(name = name) }

    /** 큐를 순서대로 돌려주고, 다 쓰면 마지막 응답을 계속 준다. */
    private class QueuedWeeklyReportRepository(
        private val responses: List<Result<WeeklyReport>> = emptyList(),
        private val alwaysPending: Boolean = false,
    ) : WeeklyReportRepository {
        val requestedDates = mutableListOf<String>()

        override suspend fun getWeeklyReport(date: String): Result<WeeklyReport> {
            requestedDates += date
            if (alwaysPending) return Result.success(pendingReport())
            return responses[minOf(requestedDates.lastIndex, responses.lastIndex)]
        }

        companion object {
            fun pendingReport(): WeeklyReport = report(EmotionAnalysis(total = 2, succeeded = 0, pending = 2, failed = 0))
        }
    }

    private fun pending(): WeeklyReport = QueuedWeeklyReportRepository.pendingReport()

    private fun completed(): WeeklyReport = report(EmotionAnalysis(total = 2, succeeded = 2, pending = 0, failed = 0))

    private companion object {
        fun report(analysis: EmotionAnalysis): WeeklyReport =
            WeeklyReport(
                dailyQuestionAmount = 0,
                diaryAmount = 1,
                summaryText = "요약",
                week = emptyList(),
                dailyQuestions = emptyList(),
                emotions = emptyList(),
                emotionAnalysis = analysis,
            )
    }
}
