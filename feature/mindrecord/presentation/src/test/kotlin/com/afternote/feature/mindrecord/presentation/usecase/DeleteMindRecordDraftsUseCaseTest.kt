package com.afternote.feature.mindrecord.presentation.usecase

import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDiaryRepository
import com.afternote.feature.mindrecord.presentation.usecase.DeleteMindRecordDraftsUseCase.Category
import com.afternote.feature.mindrecord.presentation.usecase.DeleteMindRecordDraftsUseCase.Outcome
import com.afternote.feature.mindrecord.presentation.usecase.DeleteMindRecordDraftsUseCase.Target
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * 임시저장 **일괄 삭제 정책** 가드 (#1693).
 *
 * 되돌릴 수 없는 동작이고 부분 실패가 정상 경로라, 「무엇을 실패로 칠 것인가」가 이 동작의
 * 핵심이다. 그 판정만 여기서 본다 — 화면이 그것을 어떤 문구로 보이는지는 ViewModel 몫이다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeleteMindRecordDraftsUseCaseTest {
    @Test
    fun `두 저장소에 걸친 대상을 각각의 저장소로 보낸다`() =
        runTest {
            val diaryRepository = FakeDiaryRepository()
            val dailyQuestionRepository = FakeDailyQuestionRepository()

            val outcome =
                DeleteMindRecordDraftsUseCase(diaryRepository, dailyQuestionRepository)
                    .delete(
                        targets = listOf(Target(Category.Diary, 1), Target(Category.DailyQuestion, 2)),
                        survivorsAfterDelete = { emptySet() },
                    )

            assertEquals(listOf(1L), diaryRepository.deletedIds)
            assertEquals(listOf(2L), dailyQuestionRepository.deletedIds)
            assertEquals(Outcome.Deleted(failures = emptyList(), remaining = emptyList()), outcome)
        }

    /**
     * **이 UseCase 의 존재 이유.** 지우려던 것이 이미 없어서 실패한 경우(404)는 사용자가 원한
     * 결과가 이뤄진 것이다. 그것까지 «다시 고르라» 고 하면 화면이 「목록은 비었는데 1개 선택」 이 된다.
     *
     * 다만 **계측에서는 사라지면 안 된다.** 화면에 안 보이는 실패일수록 콘솔이 유일한 흔적이라,
     * 대조로 걸러 내면 「왜 안 지워졌나」를 나중에 물을 곳이 없어진다 (#964·#1693 리뷰).
     * 그래서 화면이 보는 [Outcome.Deleted.remaining] 만 비고, [Outcome.failures] 에는 남는다.
     */
    @Test
    fun `재조회에서 사라진 실패는 화면에서 빠지되 계측에는 남는다`() =
        runTest {
            val gone = IOException("이미 없음")
            val useCase =
                DeleteMindRecordDraftsUseCase(
                    diaryRepository = FakeDiaryRepository(onDelete = { Result.failure(gone) }),
                    dailyQuestionRepository = FakeDailyQuestionRepository(),
                )

            val outcome =
                useCase.delete(
                    targets = listOf(Target(Category.Diary, 1)),
                    // 재조회 결과에 없다 = 서버에서도 사라졌다.
                    survivorsAfterDelete = { emptySet() },
                )

            assertEquals(emptyList<DeleteMindRecordDraftsUseCase.Failure>(), (outcome as Outcome.Deleted).remaining)
            assertEquals(listOf(Target(Category.Diary, 1)), outcome.failures.map { it.target })
            assertEquals(gone, outcome.failures.single().cause)
        }

    /** 반대로 **아직 남아 있는** 실패는 올려야 한다 — 그래야 다시 선택해 줄 수 있다. */
    @Test
    fun `실패하고 재조회에도 남아 있으면 실패로 올린다`() =
        runTest {
            val boom = IOException("서버 거절")
            val target = Target(Category.Diary, 1)
            val useCase =
                DeleteMindRecordDraftsUseCase(
                    diaryRepository = FakeDiaryRepository(onDelete = { Result.failure(boom) }),
                    dailyQuestionRepository = FakeDailyQuestionRepository(),
                )

            val outcome = useCase.delete(targets = listOf(target), survivorsAfterDelete = { setOf(target) })

            assertEquals(listOf(target), outcome.failures.map { it.target })
            assertEquals(boom, outcome.failures.single().cause)
            assertTrue(outcome is Outcome.Deleted)
            assertEquals(listOf(target), (outcome as Outcome.Deleted).remaining.map { it.target })
        }

    /**
     * 두 저장소의 id 는 서로 독립이라 숫자가 겹친다. 종류를 안 들고 **숫자만으로** 대조하면
     * 「사라진 데일리질문 3」의 실패가 「남아 있는 일기 3」 때문에 살아남는다 — 사용자는
     * 자기가 지운 것을 「실패했으니 다시 선택하라」는 말로 돌려받는다.
     *
     * 그래서 실패한 쪽은 사라지고 **같은 숫자의 다른 종류만 남은** 상황으로 판정한다.
     * 숫자만 보는 구현에서는 이 단언이 깨진다.
     */
    @Test
    fun `같은 숫자 id 라도 종류가 다르면 다른 항목이다`() =
        runTest {
            val useCase =
                DeleteMindRecordDraftsUseCase(
                    diaryRepository = FakeDiaryRepository(),
                    dailyQuestionRepository =
                        FakeDailyQuestionRepository(onDelete = { Result.failure(IOException("이미 없음")) }),
                )

            val outcome =
                useCase.delete(
                    targets = listOf(Target(Category.DailyQuestion, 3)),
                    // 실패한 데일리질문 3은 사라졌고, 지우지 않은 일기 3이 남아 있다.
                    survivorsAfterDelete = { setOf(Target(Category.Diary, 3)) },
                )

            // 화면이 보는 것은 remaining 이다 — 계측용 failures 에는 남는 것이 정상이다 (#1693 리뷰).
            assertEquals(
                "사라진 데일리질문 3이 남아 있는 일기 3 때문에 실패로 살아남았다",
                emptyList<Target>(),
                (outcome as Outcome.Deleted).remaining.map { it.target },
            )
            assertEquals(listOf(Target(Category.DailyQuestion, 3)), outcome.failures.map { it.target })
        }

    /**
     * 재조회가 실패하면 **무엇이 남았는지 알 수 없다.** 그때 「전부 지워졌다」로 닫으면
     * 서버에 남은 것을 없다고 말하게 된다.
     */
    @Test
    fun `재조회가 실패하면 판정하지 않고 Unknown 을 올린다`() =
        runTest {
            val boom = IOException("삭제 실패")
            val useCase =
                DeleteMindRecordDraftsUseCase(
                    diaryRepository = FakeDiaryRepository(onDelete = { Result.failure(boom) }),
                    dailyQuestionRepository = FakeDailyQuestionRepository(),
                )

            val outcome =
                useCase.delete(
                    targets = listOf(Target(Category.Diary, 1)),
                    survivorsAfterDelete = { null },
                )

            assertTrue("재조회 실패인데 $outcome 로 닫혔다", outcome is Outcome.Unknown)
            // 대조를 못 해도 계측할 실패는 그대로 실어 올린다.
            assertEquals(listOf(boom), outcome.failures.map { it.cause })
        }

    /** 지울 것이 없으면 저장소를 부르지 않는다 — 부르면 빈 재조회가 한 번 더 나간다. */
    @Test
    fun `대상이 비면 아무 것도 지우지 않는다`() =
        runTest {
            val diaryRepository = FakeDiaryRepository()
            var refreshed = false

            val outcome =
                DeleteMindRecordDraftsUseCase(diaryRepository, FakeDailyQuestionRepository())
                    .delete(targets = emptyList(), survivorsAfterDelete = {
                        refreshed = true
                        emptySet()
                    })

            assertEquals(Outcome.Deleted(failures = emptyList(), remaining = emptyList()), outcome)
            assertEquals(emptyList<Long>(), diaryRepository.deletedIds)
            assertTrue("대상이 없는데 재조회가 나갔다", !refreshed)
        }

    /** 순차 삭제면 100건이 100배 걸린다 — 실제로 겹치는지 본다. */
    @Test
    fun `여러 대상을 병렬로 지운다`() =
        runTest {
            val diaryStarted = CompletableDeferred<Unit>()
            val dailyQuestionStarted = CompletableDeferred<Unit>()
            val useCase =
                DeleteMindRecordDraftsUseCase(
                    diaryRepository =
                        FakeDiaryRepository(onDelete = {
                            diaryStarted.complete(Unit)
                            // 상대가 시작하지 않으면 여기서 멈춘다 — 순차 실행이면 교착이다.
                            dailyQuestionStarted.await()
                            Result.success(Unit)
                        }),
                    dailyQuestionRepository =
                        FakeDailyQuestionRepository(onDelete = {
                            dailyQuestionStarted.complete(Unit)
                            diaryStarted.await()
                            Result.success(Unit)
                        }),
                )

            val outcome =
                async {
                    useCase.delete(
                        targets = listOf(Target(Category.Diary, 1), Target(Category.DailyQuestion, 2)),
                        survivorsAfterDelete = { emptySet() },
                    )
                }
            advanceUntilIdle()

            assertTrue("두 삭제가 겹치지 않았다", outcome.isCompleted)
            assertEquals(Outcome.Deleted(failures = emptyList(), remaining = emptyList()), outcome.await())
        }
}
