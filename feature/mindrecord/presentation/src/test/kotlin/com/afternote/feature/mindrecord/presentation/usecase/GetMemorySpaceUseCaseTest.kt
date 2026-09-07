package com.afternote.feature.mindrecord.presentation.usecase

import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.Diary
import com.afternote.feature.mindrecord.domain.model.DiaryList
import com.afternote.feature.mindrecord.domain.model.MindRecordType
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDiaryRepository
import com.afternote.feature.mindrecord.presentation.model.memoryspace.MemoryRecordId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.time.YearMonth

/**
 * 추억 공간 집계 정책 (#1693).
 *
 * 종전에는 이 넷이 `MemorySpaceViewModel` 안에 있었다 — 비대칭 조회 범위, 부분 실패, 초안
 * 제외, 정렬·상한. ViewModel 을 통해서만 볼 수 있어서 「어느 달을 요청했는가」 같은 판정은
 * 벽시계에 기대야 했다. 기준 달을 인자로 받는 지금은 고정 날짜로 단언할 수 있다.
 *
 * 본문 이미지 추출이 `HtmlCompat` 을 타서 Robolectric 이 필요하다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GetMemorySpaceUseCaseTest {
    @Test
    fun `일기는 최근 3개월만, 데일리질문은 전체 기간을 조회한다`() =
        runTest {
            val diaryRepository = FakeDiaryRepository()
            val questionRepository = FakeDailyQuestionRepository()

            useCase(diaryRepository, questionRepository).invoke(currentMonth = YearMonth.of(2026, 9))

            assertEquals(
                listOf("2026-09", "2026-08", "2026-07"),
                diaryRepository.listQueries.map { it.yearMonth },
            )
            // 일기 조회에는 draftOnly 를 싣지 않는다 — 그 달 전체를 받아 초안은 아래에서 거른다.
            assertTrue(diaryRepository.listQueries.all { it.draftOnly == null })
        }

    @Test
    fun `두 출처를 최신순 한 줄로 세워 상한까지만 남긴다`() =
        runTest {
            val records =
                useCase(
                    diaryRepositoryOf(diary(1L, "2026-09-01"), diary(2L, "2026-09-05")),
                    FakeDailyQuestionRepository(
                        initialAnswers = listOf(question(3L, "2026-09-03"), question(4L, "2026-09-07")),
                    ),
                ).invoke(currentMonth = YearMonth.of(2026, 9), limit = 3).getOrThrow()

            assertEquals(
                listOf(
                    MemoryRecordId(MindRecordType.DAILY_QUESTION, 4L),
                    MemoryRecordId(MindRecordType.DIARY, 2L),
                    MemoryRecordId(MindRecordType.DAILY_QUESTION, 3L),
                ),
                records.map { it.id },
            )
        }

    /**
     * 두 출처의 숫자 ID 공간이 겹친다. 종전에는 데일리질문을 음수로 접어 갈랐는데, 그 우회가
     * 사라진 뒤에도 같은 숫자가 서로 다른 기록으로 남는지 본다 — 여기서 접히면 카드 하나가
     * 사라지거나 탭이 엉뚱한 기록을 연다.
     */
    @Test
    fun `같은 숫자 ID 라도 종류가 다르면 다른 기록이다`() =
        runTest {
            val records =
                useCase(
                    diaryRepositoryOf(diary(12L, "2026-09-01")),
                    FakeDailyQuestionRepository(initialAnswers = listOf(question(12L, "2026-09-02"))),
                ).invoke(currentMonth = YearMonth.of(2026, 9)).getOrThrow()

            assertEquals(
                listOf(
                    MemoryRecordId(MindRecordType.DAILY_QUESTION, 12L),
                    MemoryRecordId(MindRecordType.DIARY, 12L),
                ),
                records.map { it.id },
            )
            assertTrue("두 기록의 식별자가 같아졌다", records[0].id != records[1].id)
        }

    @Test
    fun `임시저장 일기는 전시하지 않는다`() =
        runTest {
            val records =
                useCase(
                    diaryRepositoryOf(diary(1L, "2026-09-01"), diary(2L, "2026-09-02", isDraft = true)),
                    FakeDailyQuestionRepository(),
                ).invoke(currentMonth = YearMonth.of(2026, 9)).getOrThrow()

            assertEquals(listOf(MemoryRecordId(MindRecordType.DIARY, 1L)), records.map { it.id })
        }

    @Test
    fun `한쪽이 실패해도 카드가 채워지면 성공이다`() =
        runTest {
            val records =
                useCase(
                    FakeDiaryRepository(onGetList = { _, _ -> Result.failure(IOException("일기 실패")) }),
                    FakeDailyQuestionRepository(initialAnswers = listOf(question(3L, "2026-09-03"))),
                ).invoke(currentMonth = YearMonth.of(2026, 9)).getOrThrow()

            assertEquals(listOf(MemoryRecordId(MindRecordType.DAILY_QUESTION, 3L)), records.map { it.id })
        }

    /**
     * 합친 결과가 비었는데 실패한 출처가 있으면 0건으로 확정하지 않는다 — 실패한 쪽에 기록이
     * 있었을 수 있어, 「아직 담긴 기록이 없어요」 로 오인하면 재시도 경로까지 사라진다.
     */
    @Test
    fun `결과가 비었는데 실패한 출처가 있으면 실패로 올린다`() =
        runTest {
            val result =
                useCase(
                    FakeDiaryRepository(onGetList = { _, _ -> Result.failure(IOException("일기 실패")) }),
                    FakeDailyQuestionRepository(),
                ).invoke(currentMonth = YearMonth.of(2026, 9))

            assertTrue(result.isFailure)
            assertEquals("일기 실패", result.exceptionOrNull()?.message)
        }

    @Test
    fun `모든 출처가 성공하고 기록이 없으면 빈 목록이다`() =
        runTest {
            val records =
                useCase(FakeDiaryRepository(), FakeDailyQuestionRepository())
                    .invoke(currentMonth = YearMonth.of(2026, 9))
                    .getOrThrow()

            assertTrue(records.isEmpty())
        }

    private fun useCase(
        diaryRepository: FakeDiaryRepository,
        questionRepository: FakeDailyQuestionRepository,
    ) = GetMemorySpaceUseCase(diaryRepository = diaryRepository, dailyQuestionRepository = questionRepository)

    /**
     * 달마다 그 달의 일기만 돌려주는 저장소. 기본 fake 는 `yearMonth` 를 무시하고 같은 목록을
     * 그대로 주는데, 이 UseCase 는 세 달을 따로 조회하므로 같은 일기가 세 번 실려 실기와
     * 다른 결과가 된다.
     */
    private fun diaryRepositoryOf(vararg diaries: Diary) =
        FakeDiaryRepository(
            onGetList = { yearMonth, _ ->
                Result.success(
                    DiaryList(
                        diaries = diaries.filter { it.date.startsWith(yearMonth) },
                        monthDiaryCount = diaries.size,
                        weeklyDominantMood = null,
                    ),
                )
            },
        )

    private fun diary(
        id: Long,
        date: String,
        isDraft: Boolean = false,
    ) = Diary(
        diaryId = id,
        title = "일기 $id",
        content = "<p>본문</p>",
        date = date,
        createdAt = date,
        todayMood = TodayMood.HAPPY,
        isDraft = isDraft,
    )

    private fun question(
        id: Long,
        date: String,
    ) = DailyQuestion(
        dailyQuestionId = id,
        title = "질문 $id",
        content = "<p>답변</p>",
        createdAt = date,
    )
}
