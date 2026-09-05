package com.afternote.feature.mindrecord.presentation.usecase

import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.Diary
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDiaryRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth
import kotlin.coroutines.cancellation.CancellationException

/**
 * 임시저장 **조회 범위 정책** 가드 (#1693).
 *
 * 이 정책은 화면 둘(임시저장 목록·작성 툴바 카운트)이 공유하므로, 어느 한 화면의 테스트가
 * 아니라 정책 자체를 여기서 고정한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoadMindRecordDraftsUseCaseTest {
    /**
     * 서버 계약이 갈려 범위가 비대칭이다 — 일기는 달 단위, 데일리질문은 전체 기간.
     * 한쪽이 조용히 바뀌면 두 화면의 건수가 어긋나는 형태로만 드러나므로 여기서 못 박는다.
     */
    @Test
    fun `일기는 넘긴 달만, 데일리질문은 전체 기간을 조회한다`() =
        runTest {
            val diaryRepository = FakeDiaryRepository()
            val dailyQuestionRepository = FakeDailyQuestionRepository()

            LoadMindRecordDraftsUseCase(diaryRepository, dailyQuestionRepository)
                .load(currentMonth = YearMonth.of(2026, 3))

            assertEquals(listOf("2026-03"), diaryRepository.listQueries.map { it.yearMonth })
            assertEquals(listOf(true), diaryRepository.listQueries.map { it.draftOnly })
            // 데일리질문은 date 를 싣지 않는다 — 실으면 그 날짜 것만 온다.
            assertEquals(listOf<String?>(null), dailyQuestionRepository.listQueries.map { it.date })
            assertEquals(listOf(true), dailyQuestionRepository.listQueries.map { it.draftOnly })
        }

    /**
     * **벽시계에 기대지 않는다.** 종전에는 `YearMonth.now()` 를 구현이 직접 불러, 기대값을
     * 만들려면 테스트도 같은 `now()` 를 불러야 했다 — 구현이 «지난 달» 을 조회하도록 잘못
     * 바뀌어도 기대값이 함께 틀려 통과한다.
     */
    @Test
    fun `기본값은 이번 달이지만 다른 달을 넘기면 그 달을 조회한다`() =
        runTest {
            val diaryRepository = FakeDiaryRepository()
            val useCase = LoadMindRecordDraftsUseCase(diaryRepository, FakeDailyQuestionRepository())

            useCase.load()
            useCase.load(currentMonth = YearMonth.of(2019, 12))

            assertEquals(
                listOf(YearMonth.now().toString(), "2019-12"),
                diaryRepository.listQueries.map { it.yearMonth },
            )
        }

    /** 빈 목록으로 흡수하면 「실패」와 「0건」이 같은 화면이 되어 복구 수단이 사라진다. */
    @Test
    fun `일기 조회가 실패하면 전체가 실패한다`() =
        runTest {
            val boom = IOException("일기 조회 실패")
            val useCase =
                LoadMindRecordDraftsUseCase(
                    diaryRepository = FakeDiaryRepository(onGetList = { _, _ -> Result.failure(boom) }),
                    dailyQuestionRepository = FakeDailyQuestionRepository(initialAnswers = listOf(draftQuestion(1))),
                )

            val result = useCase.load(currentMonth = YearMonth.of(2026, 3))

            assertTrue(result.isFailure)
            // `assertSame` 을 쓸 수 없다 — 코루틴 stack-trace 복원이 `async` 경계에서 예외를
            // `(String)` 생성자로 복제하므로 인스턴스가 달라진다. 종류와 메시지로 본다.
            assertEquals(boom.message, result.exceptionOrNull()?.message)
            assertTrue("$boom 이 아닌 것이 올라왔다", result.exceptionOrNull() is IOException)
        }

    /** 반대쪽도 같다 — 한쪽만 막아 두면 다른 쪽 실패가 0건으로 새어 나간다. */
    @Test
    fun `데일리질문 조회가 실패하면 전체가 실패한다`() =
        runTest {
            val boom = IOException("데일리질문 조회 실패")
            val useCase =
                LoadMindRecordDraftsUseCase(
                    diaryRepository = FakeDiaryRepository(initialDiaries = listOf(draftDiary(1))),
                    dailyQuestionRepository = FakeDailyQuestionRepository(onGetList = { _, _ -> Result.failure(boom) }),
                )

            val result = useCase.load(currentMonth = YearMonth.of(2026, 3))

            assertTrue(result.isFailure)
            // `assertSame` 을 쓸 수 없다 — 코루틴 stack-trace 복원이 `async` 경계에서 예외를
            // `(String)` 생성자로 복제하므로 인스턴스가 달라진다. 종류와 메시지로 본다.
            assertEquals(boom.message, result.exceptionOrNull()?.message)
            assertTrue("$boom 이 아닌 것이 올라왔다", result.exceptionOrNull() is IOException)
        }

    /**
     * 취소를 실패로 바꾸면 **호출부가 「조회 실패」 화면을 그린다** — 화면을 떠나며 취소된
     * 것뿐인데 오류가 뜬다. `runCatchingCancellable` 이 그것을 막는지 본다.
     */
    @Test
    fun `취소는 실패 결과로 바뀌지 않는다`() =
        runTest {
            val blocked = CompletableDeferred<Unit>()
            val useCase =
                LoadMindRecordDraftsUseCase(
                    diaryRepository =
                        FakeDiaryRepository(onGetList = { _, _ ->
                            blocked.await()
                            Result.success(emptyDiaryList())
                        }),
                    dailyQuestionRepository = FakeDailyQuestionRepository(),
                )
            var outcome: Result<LoadMindRecordDraftsUseCase.Drafts>? = null
            var cancelled = false

            val job =
                launch {
                    try {
                        outcome = useCase.load(currentMonth = YearMonth.of(2026, 3))
                    } catch (e: CancellationException) {
                        cancelled = true
                        throw e
                    }
                }
            advanceUntilIdle()
            job.cancelAndJoin()

            assertTrue("취소가 CancellationException 으로 전파돼야 한다", cancelled)
            assertEquals("취소가 Result 로 흡수됐다: $outcome", null, outcome)
        }

    /** 두 조회가 순차면 느린 쪽이 합계 대기시간이 된다 — 병렬인지 실제로 겹쳐 본다. */
    @Test
    fun `두 조회는 병렬로 나간다`() =
        runTest {
            val diaryStarted = CompletableDeferred<Unit>()
            val dailyQuestionStarted = CompletableDeferred<Unit>()
            val useCase =
                LoadMindRecordDraftsUseCase(
                    diaryRepository =
                        FakeDiaryRepository(onGetList = { _, _ ->
                            diaryStarted.complete(Unit)
                            // 상대가 시작하지 않으면 여기서 영원히 멈춘다 — 순차 실행이면 교착이다.
                            dailyQuestionStarted.await()
                            Result.success(emptyDiaryList())
                        }),
                    dailyQuestionRepository =
                        FakeDailyQuestionRepository(onGetList = { _, _ ->
                            dailyQuestionStarted.complete(Unit)
                            diaryStarted.await()
                            Result.success(emptyList())
                        }),
                )

            val result = async { useCase.load(currentMonth = YearMonth.of(2026, 3)) }
            advanceUntilIdle()

            assertTrue("두 조회가 겹치지 않았다", result.isCompleted)
            assertFalse(result.await().isFailure)
        }

    /** 툴바 카운트는 목록과 **같은 조회**를 써야 건수가 어긋나지 않는다 (#769). */
    @Test
    fun `count 는 두 종류의 합이다`() =
        runTest {
            val useCase =
                LoadMindRecordDraftsUseCase(
                    diaryRepository = FakeDiaryRepository(initialDiaries = listOf(draftDiary(1), draftDiary(2))),
                    dailyQuestionRepository = FakeDailyQuestionRepository(initialAnswers = listOf(draftQuestion(1))),
                )

            assertEquals(3, useCase.count(currentMonth = YearMonth.of(2026, 3)).getOrNull())
        }

    private fun emptyDiaryList() =
        com.afternote.feature.mindrecord.domain.model
            .DiaryList(diaries = emptyList(), monthDiaryCount = 0, weeklyDominantMood = null)

    private fun draftDiary(id: Long) =
        Diary(
            diaryId = id,
            title = "일기 $id",
            content = "본문",
            date = LocalDate.of(2026, 3, 4).toString(),
            createdAt = LocalDate.of(2026, 3, 4).toString(),
            todayMood = TodayMood.entries.first(),
            isDraft = true,
        )

    private fun draftQuestion(id: Long) =
        DailyQuestion(
            dailyQuestionId = id,
            title = "질문 $id",
            content = "답변",
            createdAt = LocalDate.of(2026, 3, 4).toString(),
            isDraft = true,
        )
}
