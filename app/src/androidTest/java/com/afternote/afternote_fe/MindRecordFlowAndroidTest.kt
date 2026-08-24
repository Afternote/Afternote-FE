package com.afternote.afternote_fe

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.afternote_fe.test.FakeDiaryRepository
import com.afternote.afternote_fe.test.FakeUserRepository
import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.presentation.screen.sender.DiaryWriteScreen
import com.afternote.feature.mindrecord.presentation.viewmodel.DailyQuestionWriteViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.DiaryWriteViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.SubmitState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
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
        repository.createResults.addLast(Result.failure(IllegalStateException("temporary")))
        repository.createResults.addLast(Result.success(Unit))
        val viewModel = diaryViewModel(repository)
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
        composeRule.waitUntilAtLeastOneExists(hasText("temporary"), timeoutMillis = 5_000)

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
        val repository =
            com.afternote.afternote_fe.test
                .FakeDailyQuestionRepository()
        repository.createResults.addLast(Result.failure(IllegalStateException("offline")))
        repository.createResults.addLast(Result.success(Unit))
        val viewModel =
            DailyQuestionWriteViewModel(
                repository = repository,
                photoUploadRepository = PhotoUploadRepository { _, _ -> Result.success("https://cdn.test/question.jpg") },
            )
        composeRule.setContent { AfternoteTheme {} }
        composeRule.waitUntil(timeoutMillis = 5_000) { !viewModel.uiState.value.isQuestionLoading }

        composeRule.runOnIdle {
            viewModel.onAnswerChanged("오늘은 용기 냈다")
            viewModel.submit()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.submitState is SubmitState.Failed
        }
        assertEquals("오늘은 용기 냈다", viewModel.uiState.value.answer)

        composeRule.runOnIdle {
            viewModel.consumeSubmitResult()
            viewModel.submit()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.submitState is SubmitState.Succeeded
        }

        assertEquals(2, repository.createdPayloads.size)
        assertEquals(repository.createdPayloads.first(), repository.createdPayloads.last())
        assertEquals("오늘은 용기 냈다", repository.createdPayloads.last().content)
        assertFalse(repository.createdPayloads.last().isDraft)
    }

    private fun diaryViewModel(repository: FakeDiaryRepository): DiaryWriteViewModel =
        DiaryWriteViewModel(
            savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        "draftId" to null,
                        "draftYearMonth" to null,
                    ),
                ),
            repository = repository,
            photoUploadRepository = PhotoUploadRepository { _, _ -> Result.success("https://cdn.test/image.jpg") },
            userRepository = FakeUserRepository(),
        )
}
