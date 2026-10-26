package com.afternote.feature.setting.presentation.profile

import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import com.afternote.core.domain.testing.FakeMyProfileRepository
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.model.user.User
import com.afternote.core.ui.theme.AfternoteTheme
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProfileEditPhoneTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `연락처를 누르면 전화번호 키보드를 요청한다`() {
        val viewModel = viewModel(FakeMyProfileRepository(profile = PROFILE))
        var inputType: Int? = null
        composeRule.setContent {
            AfternoteTheme {
                InterceptPlatformTextInput(
                    interceptor = { request, _ ->
                        val editorInfo = EditorInfo()
                        request.createInputConnection(editorInfo)
                        inputType = editorInfo.inputType
                        awaitCancellation()
                    },
                ) {
                    ProfileEditScreen(onBackClick = {}, onWithdrawGuideClick = {}, viewModel = viewModel)
                }
            }
        }
        awaitSuccess(viewModel)

        phoneField(PHONE).performScrollTo().performClick()

        composeRule.waitUntil(TIMEOUT_MILLIS) { inputType != null }
        composeRule.runOnIdle { assertEquals(InputType.TYPE_CLASS_PHONE, inputType!! and InputType.TYPE_MASK_CLASS) }
    }

    @Test
    fun `서버의 잘못된 연락처에도 오류가 보이고 저장이 막힌다`() {
        val repository = FakeMyProfileRepository(profile = PROFILE.copy(phone = INVALID_PHONE))
        val viewModel = viewModel(repository)
        setContent(viewModel)
        awaitSuccess(viewModel)

        scrollToText(PHONE_ERROR)
        composeRule.onNodeWithText(PHONE_ERROR).assertIsDisplayed()
        scrollToText("수정하기")
        composeRule
            .onNodeWithText("수정하기")
            .assertIsNotEnabled()
            .performClick()

        composeRule.runOnIdle { assertTrue(repository.profileUpdateCalls.isEmpty()) }
    }

    @Test
    fun `붙여넣은 잘못된 연락처를 고치면 오류가 해제되고 저장된다`() {
        val repository = FakeMyProfileRepository(profile = PROFILE)
        val viewModel = viewModel(repository)
        var navigatedBack = 0
        setContent(viewModel) { navigatedBack++ }
        awaitSuccess(viewModel)

        phoneField(PHONE).performScrollTo().performTextReplacement(INVALID_PHONE)
        scrollToText(PHONE_ERROR)
        composeRule.onNodeWithText(PHONE_ERROR).assertIsDisplayed()
        scrollToText("수정하기")
        composeRule.onNodeWithText("수정하기").assertIsNotEnabled()
        composeRule.runOnIdle { assertTrue(repository.profileUpdateCalls.isEmpty()) }

        scrollToText(INVALID_PHONE)
        phoneField(INVALID_PHONE).performTextReplacement("010-5555-6666")
        composeRule.onNodeWithText(PHONE_ERROR).assertDoesNotExist()
        scrollToText("수정하기")
        composeRule
            .onNodeWithText("수정하기")
            .assertIsEnabled()
            .performClick()

        composeRule.waitUntil(TIMEOUT_MILLIS) { navigatedBack == 1 }
        composeRule.runOnIdle {
            assertEquals(
                listOf(FakeMyProfileRepository.ProfileUpdateCall("기존 이름", "010-5555-6666", null)),
                repository.profileUpdateCalls,
            )
        }
    }

    private fun scrollToText(value: String) {
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(value))
    }

    private fun phoneField(value: String) = composeRule.onNode(hasSetTextAction() and hasText(value))

    private fun setContent(
        viewModel: ProfileEditViewModel,
        onBackClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            AfternoteTheme {
                ProfileEditScreen(onBackClick = onBackClick, onWithdrawGuideClick = {}, viewModel = viewModel)
            }
        }
    }

    private fun viewModel(repository: FakeMyProfileRepository) = ProfileEditViewModel(repository, FakePhotoUploadRepository.strict())

    private fun awaitSuccess(viewModel: ProfileEditViewModel) {
        composeRule.waitUntil(TIMEOUT_MILLIS) { viewModel.uiState.value is ProfileEditUiState.Success }
        composeRule.waitForIdle()
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
        const val PHONE = "01012345678"
        const val INVALID_PHONE = "01055556666ggyyy"
        const val PHONE_ERROR = "올바른 연락처를 입력해주세요."
        val PROFILE = User("기존 이름", "qa@afternote.local", PHONE, null)
    }
}
