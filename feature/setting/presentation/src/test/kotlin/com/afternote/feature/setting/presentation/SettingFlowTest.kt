package com.afternote.feature.setting.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.domain.testing.FakeMyProfileRepository
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.domain.testing.FakeSettingAccountRepository
import com.afternote.feature.setting.domain.testing.FakeSettingNotificationRepository
import com.afternote.feature.setting.presentation.home.SettingIntent
import com.afternote.feature.setting.presentation.home.SettingScreen
import com.afternote.feature.setting.presentation.home.SettingViewModel
import com.afternote.feature.setting.presentation.notification.PushNotificationIntent
import com.afternote.feature.setting.presentation.notification.PushNotificationViewModel
import com.afternote.feature.setting.presentation.notification.PushSetting
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
    fun profileLoadFailureKeepsSettingsEntriesAndRetryRecoversProfile() {
        var reads = 0
        var offline = true
        val profile =
            com.afternote.core.domain.testing.FakeMyProfileRepository.strict().apply {
                onGetMyProfile = {
                    reads++
                    if (offline) throw java.io.IOException("offline")
                    com.afternote.core.model.user
                        .User("복구 사용자", "test@example.com", null, null)
                }
            }
        val viewModel = SettingViewModel(settingFlowAuthRepository(loggedIn = true), profile, settingFlowAccountRepository())
        setSettingContent(viewModel)
        composeRule.onNodeWithText("프로필을 불러올 수 없습니다.").assertIsDisplayed()
        composeRule.onNodeWithText("프로필 수정").assertIsDisplayed()
        val failedReads = reads
        composeRule.runOnIdle { offline = false }
        composeRule.onNodeWithText("다시 시도").performClick()
        composeRule.onNodeWithText("복구 사용자").assertIsDisplayed()
        assertEquals(failedReads + 1, reads)
    }

    @Test
    fun profileAndSecurityEntries_emitExpectedNavigation() {
        val auth = settingFlowAuthRepository(loggedIn = true)
        val viewModel = settingViewModel(auth)
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
        val viewModel = settingViewModel(auth)
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
        val account = settingFlowAccountRepository()
        val viewModel = settingViewModel(settingFlowAuthRepository(loggedIn = true), account = account)
        composeRule.setContent { AfternoteTheme {} }

        assertEquals(0, account.deleteAccountCalls)
        composeRule.runOnIdle { viewModel.onIntent(SettingIntent.DeleteAccount) }
        composeRule.waitUntil(timeoutMillis = 5_000) { account.deleteAccountCalls == 1 }

        assertEquals(1, account.deleteAccountCalls)
    }

    @Test
    fun pushToggle_failure_rollsBackAndSendsExactPatchOnce() {
        val notification = settingFlowNotificationRepository()
        val pushSettingUpdateResults = ArrayDeque<Result<com.afternote.core.model.user.UserPushSetting>>()
        pushSettingUpdateResults.addLast(Result.failure(IllegalStateException("offline")))
        notification.onUpdateMyPushSettings = { _, _, _ ->
            requireNotNull(pushSettingUpdateResults.removeFirstOrNull()) { "push setting 응답이 준비되지 않음" }.getOrThrow()
        }
        val viewModel =
            PushNotificationViewModel(
                context = ApplicationProvider.getApplicationContext(),
                notificationRepository = notification,
                errorReporter = NoOpErrorReporter,
            )
        composeRule.setContent { AfternoteTheme {} }
        composeRule.waitUntil(timeoutMillis = 5_000) { !viewModel.uiState.value.isLoading }

        composeRule.runOnIdle { viewModel.onIntent(PushNotificationIntent.TogglePushSetting(PushSetting.NEWSLETTER, false)) }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.isNewsletterOn
        }

        assertEquals(listOf(Triple(false, null, null)), notification.pushSettingUpdates)
    }

    /**
     * 비밀번호 변경은 목적지가 생겼다 (#564) — 이용 불가 안내가 아니라 실제 배선을 탄다.
     *
     * 아래 [unavailableMenus_showFeedbackAndKeepSettingsUsable] 의 목록에서 이 항목을 뺀 것과 한 쌍이다.
     */
    @Test
    fun passwordChangeMenu_opensDestinationInsteadOfUnavailableFeedback() {
        val viewModel = settingViewModel(settingFlowAuthRepository(loggedIn = true))
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
    fun profileShortcuts_showSupportFeedbackAndKeepExistingNavigation() {
        val viewModel = settingViewModel(settingFlowAuthRepository(loggedIn = true))
        val destinations = mutableListOf<String>()
        setSettingContent(
            viewModel = viewModel,
            onNotice = { destinations += "notice" },
            onRecipientList = { destinations += "recipient-list" },
        )
        val resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources

        composeRule
            .onNode(
                hasContentDescription(resources.getString(R.string.setting_support_inquiry)) and hasClickAction(),
            ).performClick()
        composeRule.onNodeWithText("현재 이 메뉴는 이용할 수 없습니다.").assertIsDisplayed()
        assertEquals(emptyList<String>(), destinations)
        composeRule.onNodeWithText("확인").performClick()
        composeRule.onNodeWithText("현재 이 메뉴는 이용할 수 없습니다.").assertDoesNotExist()

        composeRule
            .onNode(
                hasContentDescription(resources.getString(R.string.setting_support_notice)) and hasClickAction(),
            ).performClick()
        composeRule
            .onNode(
                hasContentDescription(resources.getString(R.string.setting_recipient_list)) and hasClickAction(),
            ).performClick()
        assertEquals(listOf("notice", "recipient-list"), destinations)
    }

    @Test
    fun unavailableMenus_showFeedbackAndKeepSettingsUsable() {
        val viewModel = settingViewModel(settingFlowAuthRepository(loggedIn = true))
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
            val label = resources.getString(menuId)
            composeRule
                .onNode(
                    hasText(label) and hasClickAction() and !hasContentDescription(label),
                ).performScrollTo()
                .performClick()
            composeRule.onNodeWithText("현재 이 메뉴는 이용할 수 없습니다.").assertIsDisplayed()
            composeRule.onNodeWithText("확인").performClick()
        }
        composeRule.onNodeWithText("프로필 수정").performScrollTo().performClick()
        assertEquals(true, profileOpened)
    }

    private fun settingViewModel(
        auth: FakeAuthRepository,
        account: FakeSettingAccountRepository = settingFlowAccountRepository(),
    ): SettingViewModel = SettingViewModel(auth, settingFlowProfileRepository(), account)

    private fun setSettingContent(
        viewModel: SettingViewModel,
        onLogoutSuccess: () -> Unit = {},
        onProfileEdit: () -> Unit = {},
        onPasswordChange: () -> Unit = {},
        onAppLock: () -> Unit = {},
        onNotice: () -> Unit = {},
        onRecipientList: () -> Unit = {},
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
                    onRecipientListClick = onRecipientList,
                    onRecipientRegisterClick = {},
                    onDeliveryConditionsClick = {},
                    onPasskeyClick = {},
                    onAppLockClick = onAppLock,
                    onNoticeClick = onNotice,
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

private fun settingFlowProfileRepository(): FakeMyProfileRepository =
    FakeMyProfileRepository.strict().apply {
        onGetMyProfile = null
        onUpdateMyProfile = null
    }

private fun settingFlowAccountRepository(): FakeSettingAccountRepository =
    FakeSettingAccountRepository.strict().apply {
        onDeleteAccount = null
        onGetConnectedAccounts = null
    }

private fun settingFlowNotificationRepository(): FakeSettingNotificationRepository =
    FakeSettingNotificationRepository.strict().apply {
        onGetMyPushSettings = null
        onUpdateMyPushSettings = null
        onGetMyMarketingConsents = null
        onUpdateMyMarketingConsents = null
    }
