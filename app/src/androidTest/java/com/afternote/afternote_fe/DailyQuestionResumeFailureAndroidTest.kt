package com.afternote.afternote_fe

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.afternote_fe.test.FakeErrorReporter
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDiaryRepository
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.screen.sender.DailyQuestionWriteScreen
import com.afternote.feature.mindrecord.presentation.usecase.LoadMindRecordDraftsUseCase
import com.afternote.feature.mindrecord.presentation.viewmodel.DailyQuestionWriteViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * 이어쓰기 조회 실패가 **실제 화면까지** 도달하고 저장을 막는지 (#1018).
 *
 * ViewModel 단위 테스트는 `UiText.Resource` 상태까지만 본다. 화면의 그 분기가 지워지거나
 * `asString()` 결선이 끊겨도 그 테스트는 통과하므로, 문구가 눈에 보이는 것과 저장이 실제로
 * 잠기는 것은 여기서 고정한다.
 *
 * 재현 조건은 **getToday 는 성공하고 getList 만 실패**하는 창이다. 전체 오프라인은 getToday
 * 부터 실패해 이 경로에 닿지 않는다(그쪽은 questionId 부재로 이미 저장이 막힌다).
 */
@RunWith(AndroidJUnit4::class)
class DailyQuestionResumeFailureAndroidTest {
    @get:Rule(order = 0)
    val composeRule = createComposeRule()

    @get:Rule(order = 1)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    @Test
    fun resumeFailure_showsWarningAndBlocksSave() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository =
            FakeDailyQuestionRepository().apply {
                today = today.copy(isDraft = true)
                onGetList = { _, _ -> Result.failure(IOException("offline")) }
            }
        val viewModel =
            DailyQuestionWriteViewModel(
                savedStateHandle = SavedStateHandle(emptyMap()),
                repository = repository,
                photoUploadRepository = FakePhotoUploadRepository.strict(),
                draftLoader = LoadMindRecordDraftsUseCase(FakeDiaryRepository(), repository),
                // #964 텔레메트리가 필수 인자로 들어왔다. 이 테스트는 기록 내용을 보지 않으므로
                // 받아만 두는 fake 를 넘긴다.
                errorReporter = FakeErrorReporter(),
            )

        composeRule.setContent {
            AfternoteTheme {
                DailyQuestionWriteScreen(
                    viewModel = viewModel,
                    onSubmitSuccess = {},
                    onBackClick = {},
                    onDraftListClick = {},
                )
            }
        }

        val warning = context.getString(R.string.mindrecord_error_daily_question_draft_load_failed)
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            composeRule.onAllNodesWithText(warning).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(warning).assertIsDisplayed()

        // 답변을 채워도 저장이 열리면 안 된다 — 빈 에디터가 내보내는 `<p></p>` 도 isNotBlank() 라
        // 이 차단이 없으면 버튼이 살아 있고 기존 임시저장이 빈 본문으로 덮인다.
        //
        // 본문은 리치 에디터라 플레이스홀더가 별도 Text 이고 입력 타깃이 아니다. 여기서 보려는
        // 것은 «답변이 있는 상태에서도 잠기는가» 이므로 상태로 채운다.
        composeRule.runOnIdle { viewModel.onAnswerChanged("<p>사용자가 쓴 답변</p>") }
        composeRule.waitForIdle()

        composeRule
            .onNode(hasText(context.getString(R.string.mindrecord_action_save)) and hasClickAction())
            .assertIsNotEnabled()

        assertEquals(emptyList<Any>(), repository.createdPayloads)
        assertEquals(emptyList<Any>(), repository.updatedPayloads)
    }

    /**
     * 화면의 «다시 시도» 가 실제로 ViewModel 에 닿는지 (#1018).
     *
     * 위 테스트는 **차단만** 본다. 재시도 버튼이 화면에 그려지기만 하고 결선이 끊겨도 통과하므로,
     * 사용자는 잠긴 화면에서 빠져나올 수 없는 채로 남는다. 실제로 #1359 의 Content 추출을 병합할
     * 때 이 결선이 조용히 no-op 이 될 뻔했고 어떤 테스트도 잡지 못했다 — 그래서 여기 둔다.
     */
    @Test
    fun resumeFailure_retryButtonReachesViewModelAndReopensSave() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository =
            FakeDailyQuestionRepository().apply {
                today = today.copy(isDraft = true)
                onGetList = { _, _ -> Result.failure(IOException("offline")) }
            }
        val viewModel =
            DailyQuestionWriteViewModel(
                savedStateHandle = SavedStateHandle(emptyMap()),
                repository = repository,
                photoUploadRepository = FakePhotoUploadRepository.strict(),
                draftLoader = LoadMindRecordDraftsUseCase(FakeDiaryRepository(), repository),
                // #964 텔레메트리가 필수 인자로 들어왔다. 이 테스트는 기록 내용을 보지 않으므로
                // 받아만 두는 fake 를 넘긴다.
                errorReporter = FakeErrorReporter(),
            )

        composeRule.setContent {
            AfternoteTheme {
                DailyQuestionWriteScreen(
                    viewModel = viewModel,
                    onSubmitSuccess = {},
                    onBackClick = {},
                    onDraftListClick = {},
                )
            }
        }

        val warning = context.getString(R.string.mindrecord_error_daily_question_draft_load_failed)
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            composeRule.onAllNodesWithText(warning).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.runOnIdle { viewModel.onAnswerChanged("<p>사용자가 쓴 답변</p>") }
        composeRule.waitForIdle()

        // 재시도가 성공하도록 서버를 되돌린 뒤 **화면의 버튼을 누른다.**
        //
        // 이어쓸 draft 를 실제로 넣어 둔다 — 빈 목록은 이 PR 이 «있는 걸 못 찾았다» 로 보고
        // 그대로 차단하므로(진입 자체가 today.isDraft=true 일 때만 돈다), 목록을 비워 두면
        // 재시도가 성공해도 잠금이 안 풀린다.
        composeRule.runOnIdle {
            repository.answers +=
                DailyQuestion(
                    dailyQuestionId = 7L,
                    title = repository.today.content,
                    content = "<p>이어쓸 본문</p>",
                    createdAt = "2026.08.29 토",
                    isDraft = true,
                )
            repository.onGetList = null
        }
        composeRule
            .onNode(hasText(context.getString(R.string.mindrecord_error_retry)) and hasClickAction())
            .performClick()

        // 안내가 걷히고 저장이 다시 열려야 한다 — 버튼이 ViewModel 에 닿지 않으면 둘 다 그대로다.
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            composeRule.onAllNodesWithText(warning).fetchSemanticsNodes().isEmpty()
        }
        composeRule
            .onNode(hasText(context.getString(R.string.mindrecord_action_save)) and hasClickAction())
            .assertIsEnabled()
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
