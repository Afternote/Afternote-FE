package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.feature.mindrecord.domain.model.TodayMood
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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.lang.reflect.Proxy

/**
 * 작성 실패가 텔레메트리에 남는지 (#964).
 *
 * 이 모듈은 종전에 `Log.e` 도 `ErrorReporter` 도 0건이라, 사용자가 방금 쓴 글이 서버에 닿지
 * 못해도 릴리즈에서 알 방법이 없었다. 재현이 어려워 실기 QA 로는 잡히지 않는 자리다.
 *
 * **stage 값까지 고정한다.** 콘솔 필터가 그 문자열이라, 바뀌면 그 시점 이후 기록이 기존 이슈
 * 그룹과 끊겨 추이를 잃는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WriteFailureReportingTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `일기 저장 실패가 diary_submit 으로 기록된다`() =
        runTest(dispatcher) {
            val reporter = RecordingErrorReporter()
            val repository =
                FakeDiaryRepository().apply {
                    onCreate = { Result.failure(IOException("offline")) }
                }
            val viewModel = diaryViewModel(repository, reporter)
            advanceUntilIdle()

            viewModel.onTitleChanged("제목")
            viewModel.onContentChanged("<p>본문</p>")
            viewModel.onMoodSelected(TodayMood.SOSO)
            viewModel.submit(isDraft = false)
            advanceUntilIdle()

            assertEquals(listOf("diary_submit"), reporter.stages)
        }

    @Test
    fun `일기 저장 성공은 기록하지 않는다`() =
        runTest(dispatcher) {
            // 성공까지 남기면 Crashlytics 보관 한도(최근 8건)를 잡음으로 채워 실제 장애를 민다.
            val reporter = RecordingErrorReporter()
            val viewModel = diaryViewModel(FakeDiaryRepository(), reporter)
            advanceUntilIdle()

            viewModel.onTitleChanged("제목")
            viewModel.onContentChanged("<p>본문</p>")
            viewModel.onMoodSelected(TodayMood.SOSO)
            viewModel.submit(isDraft = false)
            advanceUntilIdle()

            assertEquals(emptyList<String>(), reporter.stages)
        }

    @Test
    fun `데일리질문 저장 실패가 daily_question_submit 으로 기록된다`() =
        runTest(dispatcher) {
            val reporter = RecordingErrorReporter()
            val repository =
                FakeDailyQuestionRepository().apply {
                    onCreate = { Result.failure(IOException("offline")) }
                }
            val viewModel =
                DailyQuestionWriteViewModel(
                    savedStateHandle = SavedStateHandle(emptyMap()),
                    repository = repository,
                    photoUploadRepository = failingUpload(),
                    draftLoader = LoadMindRecordDraftsUseCase(FakeDiaryRepository(), repository),
                    errorReporter = reporter,
                )
            advanceUntilIdle()

            viewModel.onAnswerChanged("<p>답변</p>")
            viewModel.submit()
            advanceUntilIdle()

            assertEquals(listOf("daily_question_submit"), reporter.stages)
        }

    @Test
    fun `미디어 업로드 실패가 media_upload 으로 기록된다`() =
        runTest(dispatcher) {
            val reporter = RecordingErrorReporter()
            val viewModel = diaryViewModel(FakeDiaryRepository(), reporter)
            advanceUntilIdle()

            viewModel.uploadMedia("content://media/1")
            advanceUntilIdle()

            assertEquals(listOf("media_upload"), reporter.stages)
        }

    @Test
    fun `데일리질문 미디어 업로드 실패도 media_upload 으로 기록된다`() =
        runTest(dispatcher) {
            // 같은 stage 라도 호출부가 둘이다 — 일기 쪽만 보면 데일리질문 계측 호출을 지워도
            // 통과한다 (#964 리뷰).
            val reporter = RecordingErrorReporter()
            val repository = FakeDailyQuestionRepository()
            val viewModel =
                DailyQuestionWriteViewModel(
                    savedStateHandle = SavedStateHandle(emptyMap()),
                    repository = repository,
                    photoUploadRepository = failingUpload(),
                    draftLoader = LoadMindRecordDraftsUseCase(FakeDiaryRepository(), repository),
                    errorReporter = reporter,
                )
            advanceUntilIdle()

            viewModel.uploadMedia("content://media/1")
            advanceUntilIdle()

            assertEquals(listOf("media_upload"), reporter.stages)
            // 기록만 남기고 화면 안내를 빠뜨리면 사용자는 첨부가 붙은 줄 안다 (#716).
            assertNotNull(viewModel.uiState.value.imageUploadError)
        }

    private fun diaryViewModel(
        repository: FakeDiaryRepository,
        reporter: RecordingErrorReporter,
    ): DiaryWriteViewModel =
        DiaryWriteViewModel(
            savedStateHandle = SavedStateHandle(emptyMap()),
            repository = repository,
            photoUploadRepository = failingUpload(),
            userRepository = emptyReceiverRepository(),
            draftLoader = LoadMindRecordDraftsUseCase(repository, FakeDailyQuestionRepository()),
            errorReporter = reporter,
        )

    private fun failingUpload(): PhotoUploadRepository = PhotoUploadRepository { _, _ -> Result.failure(IOException("upload offline")) }

    /** UserRepository 는 표면이 넓다 — 이 시나리오가 타는 호출만 답한다. */
    private fun emptyReceiverRepository(): UserReceiverRepository =
        Proxy.newProxyInstance(
            UserReceiverRepository::class.java.classLoader,
            arrayOf(UserReceiverRepository::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getReceivers" -> emptyList<Any>()
                "getReceiverListFlow" -> flowOf(emptyList<Any>())
                else -> error("Unexpected call: ${method.name}")
            }
        } as UserReceiverRepository
}
