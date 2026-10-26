package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDiaryRepository
import com.afternote.feature.mindrecord.presentation.reporting.RecordingErrorReporter
import com.afternote.feature.mindrecord.presentation.usecase.LoadMindRecordDraftsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * 수정 진입의 **대상이 바뀌지 않는지** (#2028).
 *
 * `submit()` 은 「대상 레코드도 없고 오늘 질문도 없다」를 만나면 조회를 다시 걸어 사용자가
 * 재시도할 수 있게 한다 (#565). 그 복구가 **진입을 보지 않고 언제나 오늘 질문을 부르는** 것이
 * 결함이다 — 과거 답변 수정에서 프리필이 실패하면, 두 번째 저장이 원래 답변의 PATCH 가 아니라
 * **오늘 질문의 신규 POST** 로 나간다. 사용자는 고치던 답변이 아니라 다른 질문에 글을 남긴다.
 *
 * 이 파일이 보는 것은 **어디로 나갔는가**다 — 요청이 갔는지가 아니라 그 요청의 대상이다.
 * 조회 재시도 자체는 유지한다: 복구는 없애는 것이 아니라 **원래 대상으로 되돌리는 것**이다.
 *
 * Robolectric 은 `SavedStateHandle.toRoute()` 가 `android.os.Bundle` 을 지나기 때문에만 필요하다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DailyQuestionEditTargetGuardTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `프리필이 실패한 답변 수정은 오늘 질문으로 대상을 바꾸지 않는다`() {
        val repository = failingPrefillRepository()
        val viewModel = editViewModel(repository, answerId = EDIT_ID)

        viewModel.onAnswerChanged("<p>대체 본문</p>")
        viewModel.submit()
        viewModel.submit()

        assertTrue("오늘 질문을 불러 대상을 바꿨다", repository.getTodayCalls == 0)
        assertTrue("못 읽은 원본 대신 새 답변이 생성됐다", repository.createdPayloads.isEmpty())
        assertTrue("못 읽은 원본을 덮는 PATCH 가 나갔다", repository.updatedPayloads.isEmpty())
        assertFalse("못 읽은 원본 위로 저장이 열렸다", viewModel.uiState.value.canSubmit)
    }

    /** 복구는 없어지지 않는다 — 같은 대상을 다시 조회한다. */
    @Test
    fun `프리필이 실패한 답변 수정의 재저장은 같은 대상을 다시 조회한다`() {
        val repository = failingPrefillRepository()
        val viewModel = editViewModel(repository, answerId = EDIT_ID)
        val afterEntry = repository.listQueries.size

        viewModel.onAnswerChanged("<p>대체 본문</p>")
        viewModel.submit()

        assertEquals("대상 재조회가 걸리지 않았다", afterEntry + 1, repository.listQueries.size)
    }

    /** 재조회가 성공하면 원래 대상으로 PATCH 된다. */
    @Test
    fun `재조회가 성공하면 원래 답변이 수정된다`() {
        var failNext = true
        val repository =
            FakeDailyQuestionRepository().apply {
                onGetList = { _, _ ->
                    if (failNext) {
                        failNext = false
                        Result.failure(IOException("프리필 조회 실패"))
                    } else {
                        Result.success(listOf(answer(EDIT_ID, "<p>원본 본문</p>")))
                    }
                }
            }
        val viewModel = editViewModel(repository, answerId = EDIT_ID)

        viewModel.onAnswerChanged("<p>고친 본문</p>")
        // 첫 저장은 막히고 대상 재조회를 건다.
        viewModel.submit()
        // 재조회가 성공한 뒤의 저장이 원래 대상으로 나간다.
        viewModel.submit()

        assertEquals(EDIT_ID, repository.updatedPayloads.single().first)
        assertTrue("신규 답변이 함께 생성됐다", repository.createdPayloads.isEmpty())
    }

    /** 신규 작성의 오늘 질문 재시도는 그대로다. */
    @Test
    fun `신규 작성은 오늘 질문 조회 실패 뒤 저장에서 다시 조회한다`() {
        val repository =
            FakeDailyQuestionRepository().apply {
                onGetToday = { Result.failure(IOException("오늘 질문 조회 실패")) }
            }
        val viewModel = editViewModel(repository, answerId = null)
        val afterEntry = repository.getTodayCalls

        viewModel.onAnswerChanged("<p>새 답변</p>")
        viewModel.submit()

        assertEquals("신규 작성의 오늘 질문 재시도가 사라졌다", afterEntry + 1, repository.getTodayCalls)
    }

    private fun failingPrefillRepository() =
        FakeDailyQuestionRepository().apply {
            onGetList = { _, _ -> Result.failure(IOException("프리필 조회 실패")) }
            onGetToday = { error("수정 진입에서 오늘 질문을 부르면 안 된다") }
        }

    private fun editViewModel(
        repository: FakeDailyQuestionRepository,
        answerId: Long?,
    ): DailyQuestionWriteViewModel {
        val handle =
            SavedStateHandle(
                if (answerId == null) emptyMap() else mapOf("answerId" to answerId, "isDraft" to false),
            )
        return DailyQuestionWriteViewModel(
            handle,
            repository,
            FakePhotoUploadRepository.strict(),
            // 툴바 카운트 조회가 프리필 조회 기록에 섞이지 않게 다른 저장소를 넘긴다 (#769·#770).
            LoadMindRecordDraftsUseCase(
                diaryRepository = FakeDiaryRepository(),
                dailyQuestionRepository = FakeDailyQuestionRepository(),
            ),
            RecordingErrorReporter(),
        )
    }

    private fun answer(
        id: Long,
        content: String,
    ) = DailyQuestion(
        dailyQuestionId = id,
        title = "오늘의 질문",
        content = content,
        createdAt = "2026-09-13T09:00:00",
        isDraft = false,
    )

    private companion object {
        const val EDIT_ID = 42L
    }
}
