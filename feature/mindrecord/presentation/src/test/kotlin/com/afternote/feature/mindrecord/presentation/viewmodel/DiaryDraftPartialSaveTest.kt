package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDiaryRepository
import com.afternote.feature.mindrecord.presentation.reporting.RecordingErrorReporter
import com.afternote.feature.mindrecord.presentation.usecase.LoadMindRecordDraftsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 미완성 일기가 실제로 저장되는지 (#1065).
 *
 * 임시저장의 목적이 미완성 보존인데 종전에는 제목·본문·기분이 모두 있어야 저장됐다.
 * 서버가 `isDraft=true` 의 필수 검증을 걷어(`Afternote-BE#243` → PR #267) 이제 열린다.
 *
 * **화면 조건만 보지 않고 저장소에 닿은 payload 까지 본다** — 조건을 열어도 미선택 기분을
 * 어딘가에서 값으로 채우면 고른 적 없는 기분이 사용자 데이터가 된다. 그 폴백이 없는 것이
 * 이 이슈의 절반이다.
 *
 * 본문 HTML 판정이 `HtmlCompat` 을 타서 Robolectric 이 필요하다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DiaryDraftPartialSaveTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `제목만 적어도 임시저장이 서버까지 나간다`() =
        runTest(dispatcher) {
            val repository = FakeDiaryRepository()
            val viewModel = viewModel(repository)
            advanceUntilIdle()

            viewModel.onTitleChanged("쓰다 만 제목")
            viewModel.submit(isDraft = true)
            advanceUntilIdle()

            val payload = repository.createdPayloads.single()
            assertEquals("쓰다 만 제목", payload.title)
            assertTrue(payload.isDraft)
        }

    @Test
    fun `기분을 안 골랐으면 지어내지 않고 null 로 보낸다`() =
        runTest(dispatcher) {
            val repository = FakeDiaryRepository()
            val viewModel = viewModel(repository)
            advanceUntilIdle()

            viewModel.onTitleChanged("쓰다 만 제목")
            viewModel.onContentChanged("<p>쓰다 만 본문</p>")
            viewModel.submit(isDraft = true)
            advanceUntilIdle()

            // 종전에는 SOSO 로 채웠다 — 이어쓰기로 열면 「그냥그래」가 이미 골라져 보이고
            // 주간리포트 집계와 감정 분석에도 그 값이 들어갔다.
            assertNull(repository.createdPayloads.single().todayMood)
        }

    @Test
    fun `제목도 본문도 없으면 임시저장을 보내지 않는다`() =
        runTest(dispatcher) {
            val repository = FakeDiaryRepository()
            val viewModel = viewModel(repository)
            advanceUntilIdle()

            viewModel.submit(isDraft = true)
            advanceUntilIdle()

            assertTrue("남길 것이 없는데 빈 임시저장이 나갔다", repository.createdPayloads.isEmpty())
        }

    @Test
    fun `정식 등록은 기분 없이 나가지 않는다`() =
        runTest(dispatcher) {
            val repository = FakeDiaryRepository()
            val viewModel = viewModel(repository)
            advanceUntilIdle()

            viewModel.onTitleChanged("제목")
            viewModel.onContentChanged("<p>본문</p>")
            viewModel.submit(isDraft = false)
            advanceUntilIdle()

            assertTrue("기분 없이 정식 등록이 나갔다", repository.createdPayloads.isEmpty())
        }

    private fun viewModel(repository: FakeDiaryRepository) =
        DiaryWriteViewModel(
            savedStateHandle = SavedStateHandle(mapOf("draftId" to null, "draftYearMonth" to null)),
            repository = repository,
            photoUploadRepository =
                FakePhotoUploadRepository(
                    uploadedUrl = "https://cdn.test/image.jpg",
                    uploadedKey = "mindrecords/1/image.jpg",
                ),
            userRepository =
                FakeUserRepository.strict().apply {
                    onReceiverListFlow = { flowOf(emptyList()) }
                    onGetReceivers = { emptyList() }
                },
            draftLoader = LoadMindRecordDraftsUseCase(repository, FakeDailyQuestionRepository()),
            errorReporter = RecordingErrorReporter(),
        )
}
