package com.afternote.afternote_fe

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.afternote_fe.test.appTestAuthRepository
import com.afternote.afternote_fe.test.appTestUserRepository
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.screen.SettingScreen
import com.afternote.feature.setting.presentation.viewmodel.PushNotificationViewModel
import com.afternote.feature.setting.presentation.viewmodel.SettingViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingFlowAndroidTest {
    @get:Rule(order = 0)
    val composeRule = createComposeRule()

    @get:Rule(order = 1)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    @Test
    fun profileAndSecurityEntries_emitExpectedNavigation() {
        val auth = appTestAuthRepository(loggedIn = true)
        val user = appTestUserRepository()
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
    fun receivedRecordsEntry_emitsNavigationWithoutClearingMemberSession() {
        val auth = appTestAuthRepository(loggedIn = true)
        val viewModel = SettingViewModel(auth, appTestUserRepository())
        var navigationCalls = 0

        setSettingContent(
            viewModel = viewModel,
            onReceivedRecords = { navigationCalls += 1 },
        )

        composeRule
            .onNodeWithText("받은 기록 확인하기")
            .performScrollTo()
            .performClick()

        assertEquals(1, navigationCalls)
        assertEquals(0, auth.logoutCalls)
        assertEquals(0, auth.clearSessionCalls)
        assertTrue(auth.loggedIn)
    }

    @Test
    fun logout_cancelThenConfirm_callsRepositoryExactlyOnce() {
        val auth = appTestAuthRepository(loggedIn = true)
        val user = appTestUserRepository()
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
        val user = appTestUserRepository()
        val viewModel = SettingViewModel(appTestAuthRepository(loggedIn = true), user)
        composeRule.setContent { AfternoteTheme {} }

        assertEquals(0, user.deleteAccountCalls)
        composeRule.runOnIdle { viewModel.deleteAccount() }
        composeRule.waitUntil(timeoutMillis = 5_000) { user.deleteAccountCalls == 1 }

        assertEquals(1, user.deleteAccountCalls)
    }

    @Test
    fun pushToggle_failure_rollsBackAndSendsExactPatchOnce() {
        val user = appTestUserRepository()
        val pushSettingUpdateResults = ArrayDeque<Result<com.afternote.core.model.user.UserPushSetting>>()
        pushSettingUpdateResults.addLast(Result.failure(IllegalStateException("offline")))
        user.onUpdateMyPushSettings = { _, _, _ ->
            requireNotNull(pushSettingUpdateResults.removeFirstOrNull()) { "push setting 응답이 준비되지 않음" }.getOrThrow()
        }
        val viewModel =
            PushNotificationViewModel(
                context = ApplicationProvider.getApplicationContext(),
                userRepository = user,
            )
        composeRule.setContent { AfternoteTheme {} }
        composeRule.waitUntil(timeoutMillis = 5_000) { !viewModel.uiState.value.isLoading }

        composeRule.runOnIdle { viewModel.onNewsletterToggle(false) }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.isNewsletterOn
        }

        assertEquals(listOf(Triple(false, null, null)), user.pushSettingUpdates)
    }

    private fun setSettingContent(
        viewModel: SettingViewModel,
        onLogoutSuccess: () -> Unit = {},
        onReceivedRecords: () -> Unit = {},
        onProfileEdit: () -> Unit = {},
        onAppLock: () -> Unit = {},
    ) {
        composeRule.setContent {
            AfternoteTheme {
                SettingScreen(
                    onBackClick = {},
                    onLogoutSuccess = onLogoutSuccess,
                    onReceivedRecordsClick = onReceivedRecords,
                    onProfileEditClick = onProfileEdit,
                    onPasswordChangeClick = {},
                    onLinkedAccountClick = {},
                    onNotificationClick = {},
                    onRecipientListClick = {},
                    onRecipientRegisterClick = {},
                    onAfterDeliveryClick = {},
                    onPasskeyClick = {},
                    onAppLockClick = onAppLock,
                    onFaqClick = {},
                    onInquiryClick = {},
                    onNoticeClick = {},
                    onTermsClick = {},
                    onPrivacyClick = {},
                    onServiceInfoClick = {},
                    onWithdrawGuideClick = {},
                    viewModel = viewModel,
                )
            }
        }
    }
}
