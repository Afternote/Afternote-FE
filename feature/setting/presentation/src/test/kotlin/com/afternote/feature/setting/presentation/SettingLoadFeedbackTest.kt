package com.afternote.feature.setting.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.afternote.core.domain.testing.FakeMyProfileRepository
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.model.user.User
import com.afternote.core.ui.UiText
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.domain.testing.FakeSettingAccountRepository
import com.afternote.feature.setting.presentation.account.ConnectedAccountsContent
import com.afternote.feature.setting.presentation.account.ConnectedAccountsIntent
import com.afternote.feature.setting.presentation.account.ConnectedAccountsScreen
import com.afternote.feature.setting.presentation.account.ConnectedAccountsUiState
import com.afternote.feature.setting.presentation.account.ConnectedAccountsViewModel
import com.afternote.feature.setting.presentation.notification.PushNotificationContent
import com.afternote.feature.setting.presentation.notification.PushNotificationUiState
import com.afternote.feature.setting.presentation.profile.ProfileEditIntent
import com.afternote.feature.setting.presentation.profile.ProfileEditScreen
import com.afternote.feature.setting.presentation.profile.ProfileEditUiState
import com.afternote.feature.setting.presentation.profile.ProfileEditViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class SettingLoadFeedbackTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun pushLoadErrorHidesSwitchesAndExposesRetry() {
        var retries = 0
        composeRule.setContent {
            AfternoteTheme {
                PushNotificationContent(
                    uiState = PushNotificationUiState(errorMessage = UiText.Resource(R.string.setting_push_load_error)),
                    onBack = {},
                    onNewsletterToggle = {},
                    onMindRecordToggle = {},
                    onAfternoteToggle = {},
                    onRetry = { retries++ },
                )
            }
        }
        composeRule.onNodeWithText("알림 설정을 불러올 수 없습니다.").assertIsDisplayed()
        composeRule.onNodeWithText("뉴스레터").assertDoesNotExist()
        composeRule.onNodeWithText("다시 시도").performClick()
        assertEquals(1, retries)
    }

    @Test fun accountLoadErrorExposesRetryInsteadOfAnEmptyAccountList() {
        var retries = 0
        composeRule.setContent {
            AfternoteTheme {
                ConnectedAccountsContent(
                    uiState = ConnectedAccountsUiState(errorMessage = "계정 정보를 불러올 수 없습니다."),
                    snackbarHostState = SnackbarHostState(),
                    onBack = {},
                    onToggle = { _, _ -> },
                    onRetry = { retries++ },
                )
            }
        }
        composeRule.onNodeWithText("계정 정보를 불러올 수 없습니다.").assertIsDisplayed()
        composeRule.onNodeWithText("다시 시도").performClick()
        assertEquals(1, retries)
    }

    @Test fun profileLoadRetryRecoversAndSaveFailureIsVisible() {
        var reads = 0
        val repository =
            FakeMyProfileRepository.strict().apply {
                onGetMyProfile = { if (++reads == 1) throw IOException() else User("테스트", "test@example.com", "01012345678", null) }
                onUpdateMyProfile = { _, _, _ -> throw IOException() }
            }
        val vm = ProfileEditViewModel(repository, FakePhotoUploadRepository.strict())
        composeRule.setContent { AfternoteTheme { ProfileEditScreen({}, {}, viewModel = vm) } }
        composeRule.onNodeWithText("프로필을 불러올 수 없습니다.").assertIsDisplayed()
        composeRule.onNodeWithText("다시 시도").performClick()
        composeRule.waitUntil(5_000) { vm.uiState.value is ProfileEditUiState.Success }
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("수정하기"))
        composeRule.onNodeWithText("수정하기").performClick()
        composeRule.onNodeWithText("프로필 수정에 실패했습니다. 다시 시도해 주세요.").assertIsDisplayed()
        assertEquals(2, reads)
    }

    @Test fun unlinkFailureShowsFeedbackAndKeepsAccountRows() {
        val repository = FakeSettingAccountRepository().apply { onUnlinkConnectedAccount = { throw IOException() } }
        val vm = ConnectedAccountsViewModel(repository)
        composeRule.setContent { AfternoteTheme { ConnectedAccountsScreen({}, viewModel = vm) } }
        composeRule.waitUntil(5_000) { !vm.uiState.value.isLoading }
        composeRule.runOnIdle { vm.onIntent(ConnectedAccountsIntent.Toggle("kakao", false)) }
        composeRule.onNodeWithText("계정 연결 해제에 실패했습니다.").assertIsDisplayed()
        assertEquals(4, vm.uiState.value.accounts.size)
        assertEquals(null, vm.uiState.value.errorMessage)
    }
}
