package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.DiaryList
import com.afternote.feature.mindrecord.domain.model.TodayDailyQuestion
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDiaryRepository
import com.afternote.feature.mindrecord.presentation.reporting.RecordingErrorReporter
import com.afternote.feature.mindrecord.presentation.usecase.LoadMindRecordDraftsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [DailyQuestionWriteViewModel] 의 today 재조회 경로 가드 (#565).
 *
 * 계약 — `submit()` 이 `questionId` 부재로 재조회를 걸 때, 이미 입력돼 있는 답변을
 * 서버 임시저장본으로 덮지 않는다. 화면 진입 시점(답변이 빈 상태)에서는 종전대로 이어쓴다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DailyQuestionWriteViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `today 조회 실패 후 저장을 누르면 재조회가 입력한 답변을 덮지 않는다`() {
        // 1회차 조회 실패 → questionId null. 사용자가 답변을 입력한 뒤 저장을 누르면
        // submit() 이 재조회를 거는데, 이번엔 성공 + isDraft=true 라 resumeDraft() 가 돈다.
        var todayCalls = 0
        val repository =
            FakeDailyQuestionRepository(
                onGetToday = {
                    todayCalls += 1
                    if (todayCalls == 1) {
                        Result.failure(IllegalStateException("네트워크 실패"))
                    } else {
                        Result.success(todayQuestion(isDraft = true))
                    }
                },
                onGetList = { _, _ -> Result.success(listOf(draft(content = "서버에 남아 있던 옛 임시저장본"))) },
            )
        val viewModel =
            DailyQuestionWriteViewModel(
                SavedStateHandle(emptyMap()),
                repository,
                FakePhotoUploadRepository.strict(),
                noopLoadDraftsUseCase(),
                RecordingErrorReporter(),
            )

        viewModel.onAnswerChanged("사용자가 방금 입력한 답변")
        viewModel.submit()

        assertEquals(2, todayCalls)
        assertEquals("사용자가 방금 입력한 답변", viewModel.uiState.value.answer)
        // 재조회로 얻은 draftId 는 채워져야 이어쓰기(update)가 가능하다.
        assertEquals(7L, viewModel.uiState.value.draftId)
    }

    @Test
    fun `화면 진입 시 답변이 비어 있으면 임시저장본을 그대로 이어쓴다`() {
        val repository =
            FakeDailyQuestionRepository(
                onGetToday = { Result.success(todayQuestion(isDraft = true)) },
                onGetList = { _, _ -> Result.success(listOf(draft(content = "이어쓸 본문"))) },
            )

        val viewModel =
            DailyQuestionWriteViewModel(
                SavedStateHandle(emptyMap()),
                repository,
                FakePhotoUploadRepository.strict(),
                noopLoadDraftsUseCase(),
                RecordingErrorReporter(),
            )

        assertEquals("이어쓸 본문", viewModel.uiState.value.answer)
        assertEquals(7L, viewModel.uiState.value.draftId)
    }

    @Test
    fun `재조회가 사용자가 방금 고른 이미지도 옛 draft 이미지로 되돌리지 않는다`() {
        var todayCalls = 0
        val repository =
            FakeDailyQuestionRepository(
                onGetToday = {
                    todayCalls += 1
                    if (todayCalls == 1) {
                        Result.failure(IllegalStateException("네트워크 실패"))
                    } else {
                        Result.success(todayQuestion(isDraft = true))
                    }
                },
                onGetList = { _, _ -> Result.success(listOf(draft(content = "옛 본문"))) },
            )
        val viewModel =
            DailyQuestionWriteViewModel(
                SavedStateHandle(emptyMap()),
                repository,
                FakePhotoUploadRepository(
                    uploadedUrl = "https://cdn/just-picked.jpg",
                    uploadedKey = "mindrecords/1/just-picked.jpg",
                ),
                noopLoadDraftsUseCase(),
                RecordingErrorReporter(),
            )

        viewModel.onAnswerChanged("사용자가 방금 입력한 답변")
        runBlocking { viewModel.uploadMedia("content://just-picked") }
        viewModel.submit()

        // 업로드 URL 은 에디터가 본문에 <img> 로 넣는다 — payload 필드로는 나가지 않는다 (#549).
        assertEquals("사용자가 방금 입력한 답변", viewModel.uiState.value.answer)
    }

    @Test
    fun `업로드한 URL 은 에디터에 돌려줄 뿐 상태에 따로 담지 않는다`() {
        // 서버 계약에 `imageUrl` 이 없어 payload 로 보내 봐야 무시된다. 이미지가 남는
        // 유일한 경로는 본문 HTML 의 <img> 뿐이라, ViewModel 은 URL 을 돌려주기만 한다 (#549).
        val repository =
            FakeDailyQuestionRepository(
                onGetToday = { Result.success(todayQuestion(isDraft = false)) },
                onGetList = { _, _ -> Result.success(emptyList()) },
            )
        val viewModel =
            DailyQuestionWriteViewModel(
                SavedStateHandle(emptyMap()),
                repository,
                FakePhotoUploadRepository(
                    uploadedUrl = "https://cdn/picked.jpg",
                    uploadedKey = "mindrecords/staging/13/picked.jpg",
                ),
                noopLoadDraftsUseCase(),
                RecordingErrorReporter(),
            )

        val url = runBlocking { viewModel.uploadMedia("content://picked") }

        assertEquals("https://cdn/picked.jpg", url)
    }

    @Test
    fun `이어쓰기는 본문에 실린 이미지를 그대로 가져온다`() {
        // 이미지가 본문 안에 있으므로 이어쓰기가 본문만 복원하면 이미지도 따라온다.
        // 여기서 고정하는 것은 ViewModel 단까지다 — 화면은 에디터가 외부 value 를 첫
        // 컴포지션 1회만 시드해서 아직 성립하지 않는다 (#923 소관).
        val html = "<p>이어쓸 본문</p><img src=\"https://cdn/old.jpg\" />"
        val repository =
            FakeDailyQuestionRepository(
                onGetToday = { Result.success(todayQuestion(isDraft = true)) },
                onGetList = { _, _ -> Result.success(listOf(draft(content = html))) },
            )

        val viewModel =
            DailyQuestionWriteViewModel(
                SavedStateHandle(emptyMap()),
                repository,
                FakePhotoUploadRepository.strict(),
                noopLoadDraftsUseCase(),
                RecordingErrorReporter(),
            )

        assertEquals(html, viewModel.uiState.value.answer)
    }

    @Test
    fun `today 조회가 계속 실패하면 저장은 사유를 남기고 요청을 보내지 않는다`() {
        val repository =
            FakeDailyQuestionRepository(
                onGetToday = { Result.failure(IllegalStateException("네트워크 실패")) },
                onGetList = { _, _ -> Result.success(emptyList()) },
            )
        val viewModel =
            DailyQuestionWriteViewModel(
                SavedStateHandle(emptyMap()),
                repository,
                FakePhotoUploadRepository.strict(),
                noopLoadDraftsUseCase(),
                RecordingErrorReporter(),
            )

        viewModel.onAnswerChanged("답변")
        viewModel.submit()

        assertTrue(viewModel.uiState.value.submitState is SubmitState.Failed)
        assertEquals(0, repository.createdPayloads.size)
    }

    private fun todayQuestion(isDraft: Boolean) =
        TodayDailyQuestion(
            questionId = 1L,
            day = 13,
            content = "오늘의 질문",
            isAnswered = false,
            isDraft = isDraft,
        )

    private fun draft(content: String) =
        DailyQuestion(
            dailyQuestionId = 7L,
            title = "오늘의 질문",
            content = content,
            createdAt = "2026-08-13",
            isDraft = true,
        )

    @Test
    fun `저장 시 방금 업로드한 이미지의 src 는 fileKey 로 나간다`() {
        // 서버는 본문 img src 에서 fileKey 를 받아 staging→permanent 로 옮기고 전체 URL 로
        // 재작성한다. 전체 URL 을 그대로 보내면 그 앞에 호스트를 한 번 더 붙여 403 이 된다
        // (실서버 실측 2026-08-23, #549).
        var sentContent: String? = null
        val repository =
            FakeDailyQuestionRepository(
                onGetToday = { Result.success(todayQuestion(isDraft = false)) },
                onCreate = { payload ->
                    sentContent = payload.content
                    Result.success(FakeDailyQuestionRepository.FIRST_CREATED_ID)
                },
            )
        val viewModel =
            DailyQuestionWriteViewModel(
                SavedStateHandle(emptyMap()),
                repository,
                FakePhotoUploadRepository(
                    uploadedUrl = "https://cdn.example.net/mindrecords/staging/13/a.png",
                    uploadedKey = "mindrecords/staging/13/a.png",
                ),
                noopLoadDraftsUseCase(),
                RecordingErrorReporter(),
            )

        val previewUrl = runBlocking { viewModel.uploadMedia("content://picked") }
        viewModel.onAnswerChanged("<p>본문</p><img src=\"$previewUrl\" />")
        viewModel.submit()

        assertEquals("<p>본문</p><img src=\"mindrecords/staging/13/a.png\" />", sentContent)
    }

    @Test
    fun `이미 저장된 영구 URL 은 그대로 내보낸다`() {
        // 서버가 영구 URL 은 통과시킨다 — 키로 바꾸면 이미 옮겨진 파일을 다시 옮기려다 실패한다.
        val permanent = "https://cdn.example.net/mindrecords/permanent/13/a.png"
        var sentContent: String? = null
        val repository =
            FakeDailyQuestionRepository(
                onGetToday = { Result.success(todayQuestion(isDraft = false)) },
                onCreate = { payload ->
                    sentContent = payload.content
                    Result.success(FakeDailyQuestionRepository.FIRST_CREATED_ID)
                },
            )
        val viewModel =
            DailyQuestionWriteViewModel(
                SavedStateHandle(emptyMap()),
                repository,
                FakePhotoUploadRepository.strict(),
                noopLoadDraftsUseCase(),
                RecordingErrorReporter(),
            )

        viewModel.onAnswerChanged("<p>수정</p><img src=\"$permanent\" />")
        viewModel.submit()

        assertEquals("<p>수정</p><img src=\"$permanent\" />", sentContent)
    }
}

/** 툴바 카운트는 이 테스트의 관심사가 아니다 — 0건으로 고정한다 (#769). */
internal fun noopLoadDraftsUseCase() =
    LoadMindRecordDraftsUseCase(
        diaryRepository =
            FakeDiaryRepository.strict().apply {
                onGetList = { _, _ ->
                    Result.success(DiaryList(diaries = emptyList(), monthDiaryCount = 0, weeklyDominantMood = null))
                }
            },
        dailyQuestionRepository =
            FakeDailyQuestionRepository.strict().apply {
                onGetList = { _, _ -> Result.success(emptyList()) }
            },
    )
