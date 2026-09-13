package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.afternote.core.ui.UiText
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.viewmodel.DailyQuestionWriteUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * 키보드가 떠도 본문 입력 칸이 남는지 (#1860).
 *
 * `enableEdgeToEdge()` 라 키보드가 떠도 윈도우는 줄지 않는다. 대신 IME inset 이 오고(#1849)
 * 그만큼이 화면 안에서 빠진다. 본문 칸은 질문 배너와 하단 툴바가 쓰고 **남는 것**을 받는
 * 마지막 자리라, 좁은 화면에서 글꼴 배율까지 올리면 남는 것이 없어져 0 높이로 눌렸다 —
 * 타이핑은 되는데 쓴 글이 보이지 않았다.
 *
 * 기준은 이슈의 실측 조합인 `720x1280 @320dpi · font_scale 1.30` 이고, 그때 키보드는 세로의
 * 46%(590px)다. Robolectric 에도 같은 inset 을 실어 같은 기하를 만든다 — 윈도우를 줄이는
 * 방식으로 흉내 내면 edge-to-edge 의 실제 배치와 달라져 결함이 재현되지 않는다.
 *
 * ### 좁은 화면만 보지 않는다
 *
 * 질문 배너를 늘 접어 두면 넉넉한 화면에서 질문 전문이 사라지고, 본문 자리를 고정 높이로
 * 떼어 두면 기본 설정에서 본문이 오히려 좁아진다. 그래서 **키보드가 뜬 좁은 화면에서 본문이
 * 남는 것**과 **키보드가 없을 때 질문 전문과 넓은 본문이 그대로인 것**을 함께 본다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class DailyQuestionWriteKeyboardSpaceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `키보드가 뜨면 본문 입력 칸이 남는다`() {
        render(fontScale = 1.3f)
        showKeyboard()

        composeRule
            .onNodeWithContentDescription(editorLabel())
            .assertIsDisplayed()
            .assertHeightIsAtLeast(MinimumEditorHeight)
    }

    /**
     * 이어쓰기 실패·저장 진행 안내는 질문 배너와 같은 자리에 쌓인다. 이 문구들이 붙은 채로
     * 키보드가 떠도 본문이 밀려나면 안 된다 — 안내 영역 쪽이 줄어야 한다.
     */
    @Test
    fun `안내 문구가 쌓여도 본문 입력 칸이 남는다`() {
        render(
            fontScale = 1.3f,
            state =
                baseState().copy(
                    draftResumeError = UiText.Dynamic("임시저장을 불러오지 못했어요. 다시 시도해 주세요."),
                    isUploadingImage = true,
                ),
        )
        showKeyboard()

        composeRule
            .onNodeWithContentDescription(editorLabel())
            .assertIsDisplayed()
            .assertHeightIsAtLeast(MinimumEditorHeight)
    }

    @Test
    fun `키보드가 없으면 질문 전문과 넓은 본문이 그대로다`() {
        render(fontScale = 1.0f)

        composeRule.onNodeWithText(QUESTION).assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription(editorLabel())
            .assertHeightIsAtLeast(RoomyEditorHeight)
    }

    private fun editorLabel(): String = composeRule.activity.getString(R.string.mindrecord_write_field_placeholder)

    /** 실기의 키보드 등장과 같게, 윈도우를 줄이는 대신 IME inset 을 싣는다. */
    private fun showKeyboard() {
        composeRule.runOnUiThread {
            val insets =
                WindowInsetsCompat
                    .Builder()
                    .setVisible(WindowInsetsCompat.Type.ime(), true)
                    .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, IME_HEIGHT_PX))
                    .setInsets(
                        WindowInsetsCompat.Type.systemBars(),
                        Insets.of(0, STATUS_BAR_PX, 0, NAVIGATION_BAR_PX),
                    ).build()
            ViewCompat.dispatchApplyWindowInsets(composeRule.activity.window.decorView, insets)
        }
        composeRule.waitForIdle()
    }

    private fun baseState() =
        DailyQuestionWriteUiState(
            questionContent = QUESTION,
            questionDay = 21,
            isQuestionLoading = false,
        )

    private fun render(
        fontScale: Float,
        state: DailyQuestionWriteUiState = baseState(),
    ) {
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = base.density, fontScale = fontScale),
            ) {
                AfternoteTheme {
                    DailyQuestionWriteScreenContent(
                        uiState = state,
                        date = LocalDate.of(2026, 9, 7),
                        onBackClick = {},
                        onSubmit = {},
                        onSaveDraft = {},
                        onDraftListClick = {},
                        onAnswerChanged = {},
                        onRetryResumeDraft = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private companion object {
        const val QUESTION = "오늘 하루, 누구에게 가장 고마웠나요?"

        /** 720x1280 @320dpi 에서 키보드가 먹는 세로 46%. */
        const val IME_HEIGHT_PX = 590

        /** 같은 기기의 상태바와 3버튼 내비 바. 둘 다 본문이 쓸 수 있는 높이를 줄인다. */
        const val STATUS_BAR_PX = 50
        const val NAVIGATION_BAR_PX = 96

        /**
         * 커서와 한 줄이 실제로 보이는 최소치. 에디터 안쪽 여백(위아래 16dp)에 배율 1.3 의
         * 본문 한 줄을 더한 값이다 — 이보다 작으면 여백만 남는다.
         */
        val MinimumEditorHeight = 64.dp

        /** 키보드가 없을 때의 본문 높이. 안내 영역 상한이 기본 배치를 좁히지 않았는지 본다. */
        val RoomyEditorHeight = 300.dp
    }
}
