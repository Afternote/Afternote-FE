package com.afternote.feature.setting.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.User
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.screen.ProfileEditScreen
import com.afternote.feature.setting.presentation.viewmodel.ProfileEditUiState
import com.afternote.feature.setting.presentation.viewmodel.ProfileEditViewModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ProfileReentryFormTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun successfulRefresh_preservesEditedNameAndUpdatesUntouchedFields() {
        val repository =
            FakeUserRepository.strict().apply {
                onGetMyProfile = { User("초기 이름", "before@example.com", "01011112222", null) }
            }
        val viewModel = ProfileEditViewModel(repository)
        setContent(viewModel)
        composeRule.onNode(hasSetTextAction() and hasText("초기 이름")).performScrollTo().performTextReplacement("작성 중인 이름")

        composeRule.runOnIdle {
            repository.onGetMyProfile = { User("서버 이름", "after@example.com", "01033334444", null) }
            viewModel.refreshOnReturn()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            (viewModel.uiState.value as? ProfileEditUiState.Success)?.name == "서버 이름"
        }

        composeRule.onNodeWithText("작성 중인 이름").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("01033334444").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("after@example.com").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun failedRefresh_keepsEditedFormMounted() {
        var calls = 0
        val repository =
            FakeUserRepository.strict().apply {
                onGetMyProfile = {
                    calls++
                    User("초기 이름", "before@example.com", "01011112222", null)
                }
            }
        val viewModel = ProfileEditViewModel(repository)
        setContent(viewModel)
        composeRule.onNode(hasSetTextAction() and hasText("초기 이름")).performScrollTo().performTextReplacement("작성 중인 이름")
        composeRule.onNode(hasSetTextAction() and hasText("01011112222")).performScrollTo().performTextReplacement("01099998888")

        composeRule.runOnIdle {
            repository.onGetMyProfile = {
                calls++
                error("offline")
            }
            viewModel.refreshOnReturn()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) { calls == 2 }

        assertTrue(viewModel.uiState.value is ProfileEditUiState.Success)
        composeRule.onNodeWithText("작성 중인 이름").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("01099998888").performScrollTo().assertIsDisplayed()
    }

    private fun setContent(viewModel: ProfileEditViewModel) {
        composeRule.setContent {
            AfternoteTheme {
                ProfileEditScreen(onBackClick = {}, onWithdrawGuideClick = {}, viewModel = viewModel)
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value is ProfileEditUiState.Success }
    }
}
