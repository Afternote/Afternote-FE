package com.afternote.feature.setting.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.Receiver
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.home.SettingScreen
import com.afternote.feature.setting.presentation.home.SettingViewModel
import com.afternote.feature.setting.presentation.notification.PushNotificationViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class SettingFlowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun profileAndSecurityEntries_emitExpectedNavigation() {
        val auth = settingFlowAuthRepository(loggedIn = true)
        val user = settingFlowUserRepository()
        val viewModel = SettingViewModel(auth, user)
        var destination: String? = null

        setSettingContent(
            viewModel = viewModel,
            onProfileEdit = { destination = "profile" },
            onAppLock = { destination = "app-lock" },
        )

        composeRule.onNodeWithText("테스트 사용자").assertIsDisplayed()
        composeRule.onNodeWithText("프로필 수정").performClick()
        assertEquals("profile", destination)

        composeRule.onNodeWithText("앱 잠금 설정").performScrollTo().performClick()
        assertEquals("app-lock", destination)
    }

    @Test
    fun logout_cancelThenConfirm_callsRepositoryExactlyOnce() {
        val auth = settingFlowAuthRepository(loggedIn = true)
        val user = settingFlowUserRepository()
        val viewModel = SettingViewModel(auth, user)
        var navigationCalls = 0

        setSettingContent(
            viewModel = viewModel,
            onLogoutSuccess = { navigationCalls += 1 },
        )

        composeRule.onNodeWithText("로그아웃").performScrollTo().performClick()
        composeRule.onNodeWithText("애프터노트를 로그아웃하시겠습니까?").assertIsDisplayed()
        composeRule.onNodeWithText("아니요").performClick()
        assertEquals(0, auth.logoutCalls)

        composeRule.onNodeWithText("로그아웃").performScrollTo().performClick()
        composeRule.onNodeWithText("예").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { navigationCalls == 1 }

        assertEquals(1, auth.logoutCalls)
        assertEquals(1, navigationCalls)
    }

    @Test
    fun destructiveDelete_isNotCalledUntilViewModelCommand() {
        val user = settingFlowUserRepository()
        val viewModel = SettingViewModel(settingFlowAuthRepository(loggedIn = true), user)
        composeRule.setContent { AfternoteTheme {} }

        assertEquals(0, user.deleteAccountCalls)
        composeRule.runOnIdle { viewModel.deleteAccount() }
        composeRule.waitUntil(timeoutMillis = 5_000) { user.deleteAccountCalls == 1 }

        assertEquals(1, user.deleteAccountCalls)
    }

    @Test
    fun pushToggle_failure_rollsBackAndSendsExactPatchOnce() {
        val user = settingFlowUserRepository()
        val pushSettingUpdateResults = ArrayDeque<Result<com.afternote.core.model.user.UserPushSetting>>()
        pushSettingUpdateResults.addLast(Result.failure(IllegalStateException("offline")))
        user.onUpdateMyPushSettings = { _, _, _ ->
            requireNotNull(pushSettingUpdateResults.removeFirstOrNull()) { "push setting 응답이 준비되지 않음" }.getOrThrow()
        }
        val viewModel =
            PushNotificationViewModel(
                context = ApplicationProvider.getApplicationContext(),
                userRepository = user,
                errorReporter = NoOpErrorReporter,
            )
        composeRule.setContent { AfternoteTheme {} }
        composeRule.waitUntil(timeoutMillis = 5_000) { !viewModel.uiState.value.isLoading }

        composeRule.runOnIdle { viewModel.onNewsletterToggle(false) }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.isNewsletterOn
        }

        assertEquals(listOf(Triple(false, null, null)), user.pushSettingUpdates)
    }

    /**
     * 비밀번호 변경은 목적지가 생겼다 (#564) — 이용 불가 안내가 아니라 실제 배선을 탄다.
     *
     * 아래 [unavailableMenus_showFeedbackAndKeepSettingsUsable] 의 목록에서 이 항목을 뺀 것과 한 쌍이다.
     */
    @Test
    fun passwordChangeMenu_opensDestinationInsteadOfUnavailableFeedback() {
        val viewModel = SettingViewModel(settingFlowAuthRepository(loggedIn = true), settingFlowUserRepository())
        var passwordChangeOpened = false
        setSettingContent(viewModel = viewModel, onPasswordChange = { passwordChangeOpened = true })
        val resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources

        composeRule
            .onNode(
                hasText(resources.getString(R.string.setting_account_password_change)) and hasClickAction(),
            ).performScrollTo()
            .performClick()

        assertEquals(true, passwordChangeOpened)
        composeRule.onNodeWithText("현재 이 메뉴는 이용할 수 없습니다.").assertDoesNotExist()
    }

    @Test
    fun unavailableMenus_showFeedbackAndKeepSettingsUsable() {
        val viewModel = SettingViewModel(settingFlowAuthRepository(loggedIn = true), settingFlowUserRepository())
        var profileOpened = false
        setSettingContent(viewModel = viewModel, onProfileEdit = { profileOpened = true })
        val resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
        val menuIds =
            listOf(
                R.string.setting_support_faq,
                R.string.setting_support_inquiry,
                R.string.setting_support_terms,
                R.string.setting_support_privacy,
                R.string.setting_support_service_info,
            )
        menuIds.forEach { menuId ->
            composeRule.onNode(hasText(resources.getString(menuId)) and hasClickAction()).performScrollTo().performClick()
            composeRule.onNodeWithText("현재 이 메뉴는 이용할 수 없습니다.").assertIsDisplayed()
            composeRule.onNodeWithText("확인").performClick()
        }
        composeRule.onNodeWithText("프로필 수정").performScrollTo().performClick()
        assertEquals(true, profileOpened)
    }

    private fun setSettingContent(
        viewModel: SettingViewModel,
        onLogoutSuccess: () -> Unit = {},
        onProfileEdit: () -> Unit = {},
        onPasswordChange: () -> Unit = {},
        onAppLock: () -> Unit = {},
    ) {
        composeRule.setContent {
            AfternoteTheme {
                SettingScreen(
                    onBackClick = {},
                    onLogoutSuccess = onLogoutSuccess,
                    onProfileEditClick = onProfileEdit,
                    onPasswordChangeClick = onPasswordChange,
                    onLinkedAccountClick = {},
                    onNotificationClick = {},
                    onRecipientListClick = {},
                    onRecipientRegisterClick = {},
                    onDeliveryConditionsClick = {},
                    onPasskeyClick = {},
                    onAppLockClick = onAppLock,
                    onNoticeClick = {},
                    onWithdrawGuideClick = {},
                    viewModel = viewModel,
                )
            }
        }
    }
}

private fun settingFlowAuthRepository(loggedIn: Boolean): FakeAuthRepository =
    FakeAuthRepository.strict(loggedIn = loggedIn).apply {
        onIsLoggedIn = null
        onSaveSession = null
        onUpdateTokens = null
        onClearSession = null
        onGetAccessToken = null
        onGetRefreshToken = null
        onDefaultLogin = null
        onLogout = null
    }

private fun settingFlowUserRepository(): FakeUserRepository =
    FakeUserRepository.strict().apply {
        receiverState.value = listOf(Receiver(7L, "김수신", "가족"))
        onReceiverListFlow = null
        onGetReceivers = null
        onCreateReceiver = null
        onGetMyProfile = null
        onUpdateMyProfile = null
        onDeleteAccount = null
        onGetMyPushSettings = null
        onUpdateMyPushSettings = null
        onGetMyMarketingConsents = null
        onUpdateMyMarketingConsents = null
        onGetConnectedAccounts = null
    }
