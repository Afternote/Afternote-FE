package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.domain.model.UploadedFile
import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDiaryRepository
import com.afternote.feature.mindrecord.presentation.reporting.RecordingErrorReporter
import com.afternote.feature.mindrecord.presentation.usecase.LoadMindRecordDraftsUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 첨부 업로드 잠금이 **진행 중인 것을 빠짐없이 세는지** (#2029 · #2030).
 *
 * 잠금이 Boolean 하나였을 때 두 가지가 어긋났다.
 *
 * 1. **먼저 끝난 하나가 잠금을 통째로 풀었다** — 첨부 A·B 를 잇따라 고르고 A 만 끝나면
 *    B 가 아직 올라가는 중인데 저장이 열렸다. 그 저장에는 B 가 빠진 본문이 실린다.
 * 2. **취소에는 내려놓을 자리가 없었다** — 업로드 코루틴은 작성 화면의 `rememberCoroutineScope()`
 *    가 소유하므로 구성 변경이면 코루틴만 끊기고 `Result` 는 오지 않는다. 잠금이 참으로 굳어,
 *    이미 쓴 내용을 저장하려면 관계없는 첨부를 한 번 더 성공시켜야 했다.
 *
 * 두 작성 화면이 같은 코드 모양을 복제하고 있어 **둘을 같은 단언으로** 본다 — 한쪽만 고치면
 * 다른 쪽이 그대로 남는 것이 이 결함의 모양이다.
 *
 * 업로드를 붙잡아야 창이 열리므로 [CompletableDeferred] 로 응답 시점을 쥔다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WriteUploadLockTest {
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
    fun `일기 - 앞선 첨부가 끝나도 뒤 첨부가 올라가는 중이면 저장이 닫혀 있다`() =
        runTest(dispatcher) {
            val uploader = GatedUploader()
            val viewModel = diaryViewModel(uploader)
            advanceUntilIdle()
            viewModel.onTitleChanged("제목")
            viewModel.onContentChanged("<p>본문</p>")

            val first = launch { viewModel.uploadMedia("content://a") }
            val second = launch { viewModel.uploadMedia("content://b") }
            advanceUntilIdle()

            uploader.complete("content://a")
            advanceUntilIdle()

            assertTrue("뒤 첨부가 남았는데 업로드 표시가 사라졌다", viewModel.uiState.value.isUploadingImage)
            assertFalse("뒤 첨부가 남았는데 저장이 열렸다", viewModel.uiState.value.canSaveDraft)

            uploader.complete("content://b")
            advanceUntilIdle()
            assertFalse("둘 다 끝났는데 업로드 표시가 남았다", viewModel.uiState.value.isUploadingImage)
            first.cancelAndJoin()
            second.cancelAndJoin()
        }

    @Test
    fun `데일리답변 - 앞선 첨부가 끝나도 뒤 첨부가 올라가는 중이면 저장이 닫혀 있다`() =
        runTest(dispatcher) {
            val uploader = GatedUploader()
            val viewModel = dailyQuestionViewModel(uploader)
            advanceUntilIdle()
            viewModel.onAnswerChanged("<p>본문</p>")

            val first = launch { viewModel.uploadMedia("content://a") }
            val second = launch { viewModel.uploadMedia("content://b") }
            advanceUntilIdle()

            uploader.complete("content://a")
            advanceUntilIdle()

            assertTrue("뒤 첨부가 남았는데 업로드 표시가 사라졌다", viewModel.uiState.value.isUploadingImage)
            assertFalse("뒤 첨부가 남았는데 저장이 열렸다", viewModel.uiState.value.canSubmit)

            uploader.complete("content://b")
            advanceUntilIdle()
            assertFalse("둘 다 끝났는데 업로드 표시가 남았다", viewModel.uiState.value.isUploadingImage)
            first.cancelAndJoin()
            second.cancelAndJoin()
        }

    @Test
    fun `일기 - 화면이 사라져 첨부가 취소되면 저장 잠금이 풀린다`() =
        runTest(dispatcher) {
            val uploader = GatedUploader()
            val viewModel = diaryViewModel(uploader)
            advanceUntilIdle()
            viewModel.onTitleChanged("제목")
            viewModel.onContentChanged("<p>본문</p>")

            val screenScope = CoroutineScope(Job() + dispatcher)
            screenScope.launch { viewModel.uploadMedia("content://a") }
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isUploadingImage)

            screenScope.cancel()
            advanceUntilIdle()

            assertFalse("취소된 첨부의 잠금이 남았다", viewModel.uiState.value.isUploadingImage)
            assertTrue("취소 뒤 저장이 열리지 않았다", viewModel.uiState.value.canSaveDraft)
        }

    @Test
    fun `일기 - 하나가 취소돼도 남은 첨부의 잠금은 유지된다`() =
        runTest(dispatcher) {
            val uploader = GatedUploader()
            val viewModel = diaryViewModel(uploader)
            advanceUntilIdle()
            viewModel.onTitleChanged("제목")
            viewModel.onContentChanged("<p>본문</p>")

            val cancelled = CoroutineScope(Job() + dispatcher)
            cancelled.launch { viewModel.uploadMedia("content://a") }
            val surviving = launch { viewModel.uploadMedia("content://b") }
            advanceUntilIdle()

            cancelled.cancel()
            advanceUntilIdle()

            assertTrue("남은 첨부가 있는데 잠금이 풀렸다", viewModel.uiState.value.isUploadingImage)
            uploader.complete("content://b")
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isUploadingImage)
            surviving.cancelAndJoin()
        }

    /** 응답 시점을 테스트가 쥐는 업로더 — 두 첨부가 겹치는 창을 만든다. */
    private class GatedUploader : PhotoUploadRepository {
        private val gates = mutableMapOf<String, CompletableDeferred<Unit>>()

        override suspend fun upload(
            uriString: String,
            directory: String,
        ): Result<UploadedFile> {
            gates.getOrPut(uriString) { CompletableDeferred() }.await()
            return Result.success(
                UploadedFile(fileUrl = "https://cdn.test/$uriString.jpg", fileKey = "mindrecords/$uriString.jpg"),
            )
        }

        fun complete(uriString: String) {
            gates.getOrPut(uriString) { CompletableDeferred() }.complete(Unit)
        }
    }

    private fun diaryViewModel(uploader: PhotoUploadRepository) =
        DiaryWriteViewModel(
            savedStateHandle = SavedStateHandle(emptyMap()),
            repository = FakeDiaryRepository(),
            photoUploadRepository = uploader,
            userRepository =
                FakeUserRepository.strict().apply {
                    onReceiverListFlow = { flowOf(emptyList()) }
                    onGetReceivers = { emptyList() }
                },
            draftLoader = LoadMindRecordDraftsUseCase(FakeDiaryRepository(), FakeDailyQuestionRepository()),
            errorReporter = RecordingErrorReporter(),
        )

    private fun dailyQuestionViewModel(uploader: PhotoUploadRepository) =
        DailyQuestionWriteViewModel(
            SavedStateHandle(emptyMap()),
            FakeDailyQuestionRepository(),
            uploader,
            LoadMindRecordDraftsUseCase(FakeDiaryRepository(), FakeDailyQuestionRepository()),
            RecordingErrorReporter(),
        )
}
