package com.afternote.afternote_fe

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.afternote_fe.test.FakeErrorReporter
import com.afternote.afternote_fe.test.appTestUserRepository
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDiaryRepository
import com.afternote.feature.mindrecord.presentation.screen.sender.DailyQuestionWriteScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.DiaryWriteScreen
import com.afternote.feature.mindrecord.presentation.viewmodel.DailyQuestionWriteViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.DiaryWriteViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.MindRecordDraftLoader
import com.afternote.feature.mindrecord.presentation.viewmodel.SubmitState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MindRecordFlowAndroidTest {
    @get:Rule(order = 0)
    val composeRule = createComposeRule()

    @get:Rule(order = 1)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    @Test
    fun diaryFailureThenRetry_preservesInputAndSendsExactPayload() {
        val repository = FakeDiaryRepository()
        // 첫 저장은 실패, 재시도는 성공 — 입력이 남는지와 정확한 payload 를 본다.
        val createResults =
            ArrayDeque(
                listOf(
                    Result.failure<Unit>(IllegalStateException("temporary")),
                    Result.success(Unit),
                ),
            )
        repository.onCreate = { createResults.removeFirst() }
        val reporter = FakeErrorReporter()
        val viewModel = diaryViewModel(repository, reporter)
        var successCalls = 0

        composeRule.setContent {
            AfternoteTheme {
                DiaryWriteScreen(
                    viewModel = viewModel,
                    onSubmitSuccess = { successCalls += 1 },
                )
            }
        }
        composeRule.runOnIdle {
            viewModel.onTitleChanged("기억할 하루")
            viewModel.onContentChanged("<p>오늘의 본문</p>")
            viewModel.onMoodSelected(TodayMood.HAPPY)
            viewModel.onReceiverToggled(7L)
        }

        composeRule.onNodeWithText("기억할 하루").assertIsDisplayed()
        composeRule.onNodeWithText("김수신님에게").assertIsDisplayed()
        composeRule.onNode(hasText("등록") and hasClickAction()).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.submitState is SubmitState.Failed
        }
        composeRule.onNodeWithText("일기 등록에 실패했습니다.").assertIsDisplayed()

        composeRule.onNodeWithText("기억할 하루").assertIsDisplayed()
        composeRule.onNode(hasText("등록") and hasClickAction()).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { successCalls == 1 }

        assertEquals(2, repository.createdPayloads.size)
        val payload = repository.createdPayloads.last()
        assertEquals("기억할 하루", payload.title)
        assertEquals("<p>오늘의 본문</p>", payload.content)
        assertEquals(TodayMood.HAPPY, payload.todayMood)
        assertEquals(listOf(7L), payload.receiverIds)
        assertFalse(payload.isDraft)

        // 실패는 한 번뿐이었으므로 기록도 한 번이다. 성공한 재시도가 더 남기면 Crashlytics
        // 보관 한도(최근 8건)를 잡음으로 채운다 (#964).
        assertEquals(listOf("diary_submit"), reporter.mindRecordStages)
    }

    @Test
    fun receiverSheet_selectionIsReflectedBeforeSubmit() {
        val viewModel = diaryViewModel(FakeDiaryRepository())
        composeRule.setContent {
            AfternoteTheme {
                DiaryWriteScreen(viewModel = viewModel)
            }
        }

        composeRule.onNodeWithText("수신자 설정하기").performClick()
        composeRule.onNodeWithText("수신자 선택").assertIsDisplayed()
        composeRule.onNodeWithText("김수신").performClick()
        composeRule.onNodeWithText("확인").performClick()

        composeRule.onNodeWithText("김수신님에게").assertIsDisplayed()
        assertEquals(setOf(7L), viewModel.uiState.value.selectedReceiverIds)
    }

    @Test
    fun dailyQuestion_failureThenRetry_keepsAnswerAndAvoidsDuplicateSuccess() {
        val repository = FakeDailyQuestionRepository()
        // 첫 저장은 실패, 재시도는 성공 — 실패 후 답변이 남는지와 중복 성공이 없는지를 본다.
        val createResults =
            ArrayDeque(
                listOf(
                    Result.failure<Long>(IllegalStateException("offline")),
                    Result.success(FakeDailyQuestionRepository.FIRST_CREATED_ID),
                ),
            )
        repository.onCreate = { createResults.removeFirst() }
        val reporter = FakeErrorReporter()
        val viewModel =
            DailyQuestionWriteViewModel(
                savedStateHandle = SavedStateHandle(emptyMap()),
                repository = repository,
                photoUploadRepository =
                    FakePhotoUploadRepository(
                        uploadedUrl = "https://cdn.test/question.jpg",
                        uploadedKey = "mindrecords/1/question.jpg",
                    ),
                draftLoader = MindRecordDraftLoader(FakeDiaryRepository(), repository),
                errorReporter = reporter,
            )
        // 실제 작성 화면을 띄운다 — 빈 테마만 compose 하고 submit() 을 직접 부르면 화면과
        // ViewModel 사이 결선이 검증되지 않아, 계측 호출을 지워도 통과한다 (#964 리뷰).
        composeRule.setContent {
            AfternoteTheme {
                DailyQuestionWriteScreen(viewModel = viewModel, onSubmitSuccess = { })
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) { !viewModel.uiState.value.isQuestionLoading }

        composeRule.runOnIdle { viewModel.onAnswerChanged("오늘은 용기 냈다") }
        composeRule.onNode(hasText("저장") and hasClickAction()).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.submitState is SubmitState.Failed
        }
        // 실제 화면을 태우므로 에디터가 되돌려 주는 HTML 이 그대로 상태에 들어간다 —
        // 종전 테스트는 화면을 건너뛰어 원문 문자열이 남았다.
        assertEquals("<p>오늘은 용기 냈다</p>", viewModel.uiState.value.answer)
        assertEquals(listOf("daily_question_submit"), reporter.mindRecordStages)

        composeRule.runOnIdle { viewModel.consumeSubmitResult() }
        composeRule.onNode(hasText("저장") and hasClickAction()).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.submitState is SubmitState.Succeeded
        }

        assertEquals(2, repository.createdPayloads.size)
        assertEquals(repository.createdPayloads.first(), repository.createdPayloads.last())
        assertEquals("<p>오늘은 용기 냈다</p>", repository.createdPayloads.last().content)
        assertFalse(repository.createdPayloads.last().isDraft)
        // 성공한 재시도는 기록을 늘리지 않는다.
        assertEquals(listOf("daily_question_submit"), reporter.mindRecordStages)
    }

    private fun diaryViewModel(
        repository: FakeDiaryRepository,
        reporter: FakeErrorReporter = FakeErrorReporter(),
    ): DiaryWriteViewModel =
        DiaryWriteViewModel(
            savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        "draftId" to null,
                        "draftYearMonth" to null,
                    ),
                ),
            repository = repository,
            photoUploadRepository =
                FakePhotoUploadRepository(
                    uploadedUrl = "https://cdn.test/image.jpg",
                    uploadedKey = "mindrecords/1/image.jpg",
                ),
            userRepository = appTestUserRepository(),
            draftLoader = MindRecordDraftLoader(repository, FakeDailyQuestionRepository()),
            errorReporter = reporter,
        )
}
