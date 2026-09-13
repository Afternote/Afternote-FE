package com.afternote.feature.setting.presentation.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.afternote.core.ui.UiText
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.viewmodel.PasswordChangeIntent
import com.afternote.feature.setting.presentation.viewmodel.PasswordChangeUiState
import com.afternote.feature.setting.presentation.viewmodel.PasswordChanged
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 비밀번호 변경 화면의 배선 회귀 가드 (#564).
 *
 * 화면 쪽에서만 깨질 수 있는 것을 잡는다 — 입력이 Intent 로 나가는지, 제출 버튼이 규칙 미충족에서
 * 실제로 눌리지 않는지(색만 바뀌는 비활성은 「눌러도 아무 일 없는 버튼」이다), 완료 신호가 소비
 * Intent 와 짝을 이뤄 한 번만 나가는지.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class PasswordChangeContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val resources get() = ApplicationProvider.getApplicationContext<android.content.Context>().resources

    /**
     * 두 칸이 각자의 Intent 로 나가는지 본다 — 같은 Intent 에 둘을 배선하면 새 비밀번호가 현재
     * 비밀번호 칸을 덮어써도 컴파일은 통과한다.
     *
     * 최초 빈 문자열 방출은 `snapshotFlow` 의 첫 값이라 정상이다. 마지막 값으로 단언한다.
     */
    @Test
    fun typedPasswords_areForwardedAsIntents() {
        val intents = mutableListOf<PasswordChangeIntent>()
        setContent(PasswordChangeUiState(), intents)

        typeInto(R.string.settings_password_change_current_placeholder, "OldPass1!")
        typeInto(R.string.settings_password_change_new_placeholder, "NewPass1!")

        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            intents.filterIsInstance<PasswordChangeIntent.UpdateNewPassword>().lastOrNull()?.value == "NewPass1!"
        }
        assertEquals(
            "OldPass1!",
            intents.filterIsInstance<PasswordChangeIntent.UpdateCurrentPassword>().last().value,
        )
    }

    @Test
    fun submitButton_isDisabledUntilStateAllowsIt() {
        val intents = mutableListOf<PasswordChangeIntent>()
        setContent(PasswordChangeUiState(currentPassword = "OldPass1!", newPassword = "weakpassword"), intents)

        val submit = resources.getString(R.string.settings_password_change_submit)
        composeRule.onNode(hasText(submit) and hasClickAction()).assertIsNotEnabled().performClick()

        assertEquals(emptyList<PasswordChangeIntent>(), intents.filterIsInstance<PasswordChangeIntent.Submit>())
    }

    @Test
    fun submitButton_sendsSubmitIntentWhenStateAllowsIt() {
        val intents = mutableListOf<PasswordChangeIntent>()
        setContent(PasswordChangeUiState(currentPassword = "OldPass1!", newPassword = "NewPass1!"), intents)

        val submit = resources.getString(R.string.settings_password_change_submit)
        composeRule.onNode(hasText(submit) and hasClickAction()).assertIsEnabled().performClick()

        assertEquals(listOf(PasswordChangeIntent.Submit), intents.filterIsInstance<PasswordChangeIntent.Submit>())
    }

    @Test
    fun errorMessage_isRenderedFromResourceNotServerText() {
        setContent(
            PasswordChangeUiState(errorMessage = UiText.Resource(R.string.settings_password_change_unchanged)),
            mutableListOf(),
        )

        composeRule
            .onNodeWithText(resources.getString(R.string.settings_password_change_unchanged))
            .assertIsDisplayed()
    }

    @Test
    fun changedSignal_notifiesOnceAndIsConsumed() {
        val intents = mutableListOf<PasswordChangeIntent>()
        var changedCount = 0
        setContent(PasswordChangeUiState(changed = PasswordChanged), intents, onChanged = { changedCount++ })

        composeRule.waitForIdle()

        assertEquals(1, changedCount)
        assertEquals(
            listOf(PasswordChangeIntent.ConsumeChanged),
            intents.filterIsInstance<PasswordChangeIntent.ConsumeChanged>(),
        )
    }

    private fun typeInto(
        placeholderRes: Int,
        text: String,
    ) {
        composeRule
            .onNode(hasText(resources.getString(placeholderRes)) and hasSetTextAction())
            .performTextInput(text)
    }

    private fun setContent(
        uiState: PasswordChangeUiState,
        intents: MutableList<PasswordChangeIntent>,
        onChanged: () -> Unit = {},
    ) {
        composeRule.setContent {
            AfternoteTheme {
                PasswordChangeContent(
                    uiState = uiState,
                    onIntent = { intents += it },
                    onBackClick = {},
                    onChanged = onChanged,
                )
            }
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
