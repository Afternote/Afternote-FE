package com.afternote.afternote_fe

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDiaryRepository
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.screen.sender.DailyQuestionWriteScreen
import com.afternote.feature.mindrecord.presentation.viewmodel.DailyQuestionWriteViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.MindRecordDraftLoader
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
                draftLoader = MindRecordDraftLoader(FakeDiaryRepository(), repository),
            )

        composeRule.setContent {
            AfternoteTheme {
                DailyQuestionWriteScreen(viewModel = viewModel, onSubmitSuccess = {})
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

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
