package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.Diary
import com.afternote.feature.mindrecord.domain.model.DiaryList
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDiaryRepository
import com.afternote.feature.mindrecord.presentation.reporting.RecordingErrorReporter
import com.afternote.feature.mindrecord.presentation.usecase.LoadMindRecordDraftsUseCase
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 늦게 도착한 수정 프리필이 **작성 중 입력을 덮지 않는지** (#2031).
 *
 * 입력창은 프리필을 기다리는 동안에도 편집할 수 있다. 두 작성 화면의 프리필 성공 경로가
 * 사용자가 손댔는지 보지 않고 값을 실어, 응답이 늦게 오면 방금 친 글이 서버 값으로 되돌아갔다.
 *
 * 같은 VM 의 오늘 초안 이어쓰기(`resumeDraft`)에는 이미 그 보호가 있었다 — **대상 ID 로 들어온
 * 수정 프리필만 빠져 있었다.** 그래서 이 파일은 「사용자가 손댄 칸은 그대로」와 「손대지 않은 칸은
 * 원본으로 채워진다」를 함께 본다. 한쪽만 보면 덮어쓰기를 막다가 프리필 자체를 죽인다.
 *
 * Robolectric 은 `SavedStateHandle.toRoute()` 가 `android.os.Bundle` 을 지나기 때문에만 필요하다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LatePrefillOverwriteTest {
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
    fun `일기 - 로딩 중 입력한 값이 늦은 원본으로 되돌아가지 않는다`() =
        runTest(dispatcher) {
            val gate = CompletableDeferred<Unit>()
            val repository =
                FakeDiaryRepository().apply {
                    onGetList = { _, _ ->
                        gate.await()
                        Result.success(DiaryList(listOf(serverDiary()), monthDiaryCount = 1, weeklyDominantMood = null))
                    }
                }
            val viewModel = diaryViewModel(repository)
            advanceUntilIdle()

            viewModel.onTitleChanged("내가 쓴 제목")
            viewModel.onContentChanged("<p>내가 쓴 본문</p>")
            viewModel.onMoodSelected(TodayMood.HAPPY)

            gate.complete(Unit)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("내가 쓴 제목", state.title)
            assertEquals("<p>내가 쓴 본문</p>", state.content)
            assertEquals(TodayMood.HAPPY, state.mood)
            assertTrue("프리필 도착이 기록되지 않아 저장이 막힌다", state.draftLoaded)
        }

    @Test
    fun `일기 - 손대지 않은 칸은 원본으로 채워진다`() =
        runTest(dispatcher) {
            val repository =
                FakeDiaryRepository().apply {
                    onGetList = { _, _ ->
                        Result.success(DiaryList(listOf(serverDiary()), monthDiaryCount = 1, weeklyDominantMood = null))
                    }
                }
            val viewModel = diaryViewModel(repository)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("원본 제목", state.title)
            assertEquals("<p>원본 본문</p>", state.content)
            assertEquals(TodayMood.SOSO, state.mood)
        }

    /** 한 칸만 손댄 경우 — 그 칸만 지키고 나머지는 원본으로 채운다. */
    @Test
    fun `일기 - 제목만 입력했으면 본문과 기분은 원본으로 채워진다`() =
        runTest(dispatcher) {
            val gate = CompletableDeferred<Unit>()
            val repository =
                FakeDiaryRepository().apply {
                    onGetList = { _, _ ->
                        gate.await()
                        Result.success(DiaryList(listOf(serverDiary()), monthDiaryCount = 1, weeklyDominantMood = null))
                    }
                }
            val viewModel = diaryViewModel(repository)
            advanceUntilIdle()

            viewModel.onTitleChanged("내가 쓴 제목")
            gate.complete(Unit)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("내가 쓴 제목", state.title)
            assertEquals("<p>원본 본문</p>", state.content)
            assertEquals(TodayMood.SOSO, state.mood)
        }

    @Test
    fun `데일리답변 - 로딩 중 입력한 본문이 늦은 원본으로 되돌아가지 않는다`() =
        runTest(dispatcher) {
            val gate = CompletableDeferred<Unit>()
            val repository =
                FakeDailyQuestionRepository().apply {
                    onGetList = { _, _ ->
                        gate.await()
                        Result.success(listOf(serverAnswer()))
                    }
                }
            val viewModel = dailyQuestionViewModel(repository)
            advanceUntilIdle()

            viewModel.onAnswerChanged("<p>내가 쓴 본문</p>")

            gate.complete(Unit)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("<p>내가 쓴 본문</p>", state.answer)
            // 질문 문구와 대상 ID 는 서버가 정하는 값이라 언제나 실려야 한다.
            assertEquals("오늘의 질문", state.questionContent)
            assertEquals(EDIT_ID, state.draftId)
            assertTrue("프리필 도착이 기록되지 않아 저장이 막힌다", state.contentLoaded)
        }

    @Test
    fun `데일리답변 - 손대지 않았으면 원본 본문이 채워진다`() =
        runTest(dispatcher) {
            val repository =
                FakeDailyQuestionRepository().apply {
                    onGetList = { _, _ -> Result.success(listOf(serverAnswer())) }
                }
            val viewModel = dailyQuestionViewModel(repository)
            advanceUntilIdle()

            assertEquals("<p>원본 본문</p>", viewModel.uiState.value.answer)
        }

    private fun serverDiary() =
        Diary(
            diaryId = EDIT_ID,
            title = "원본 제목",
            content = "<p>원본 본문</p>",
            date = "2026-09-13",
            createdAt = "2026-09-13T09:00:00",
            todayMood = TodayMood.SOSO,
            isDraft = false,
        )

    private fun serverAnswer() =
        DailyQuestion(
            dailyQuestionId = EDIT_ID,
            title = "오늘의 질문",
            content = "<p>원본 본문</p>",
            createdAt = "2026-09-13T09:00:00",
            isDraft = false,
        )

    private fun diaryViewModel(repository: FakeDiaryRepository) =
        DiaryWriteViewModel(
            savedStateHandle =
                SavedStateHandle(
                    mapOf("recordId" to EDIT_ID, "yearMonth" to "2026-09", "isDraft" to false),
                ),
            repository = repository,
            photoUploadRepository = FakePhotoUploadRepository.strict(),
            userRepository =
                FakeUserRepository.strict().apply {
                    onReceiverListFlow = { flowOf(emptyList()) }
                    onGetReceivers = { emptyList() }
                },
            draftLoader = LoadMindRecordDraftsUseCase(FakeDiaryRepository(), FakeDailyQuestionRepository()),
            errorReporter = RecordingErrorReporter(),
        )

    private fun dailyQuestionViewModel(repository: FakeDailyQuestionRepository) =
        DailyQuestionWriteViewModel(
            SavedStateHandle(mapOf("answerId" to EDIT_ID, "isDraft" to false)),
            repository,
            FakePhotoUploadRepository.strict(),
            LoadMindRecordDraftsUseCase(FakeDiaryRepository(), FakeDailyQuestionRepository()),
            RecordingErrorReporter(),
        )

    private companion object {
        const val EDIT_ID = 42L
    }
}
