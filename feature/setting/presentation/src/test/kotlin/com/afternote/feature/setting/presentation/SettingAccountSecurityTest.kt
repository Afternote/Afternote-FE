package com.afternote.feature.setting.presentation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.domain.testing.FakeMyProfileRepository
import com.afternote.core.domain.testing.FakeMyProfileRepository.ProfileUpdateCall
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.domain.testing.FakeUserReceiverRepository
import com.afternote.core.domain.testing.FakeUserReceiverRepository.DeliveryUpdateCall
import com.afternote.core.model.delivery.ConditionState
import com.afternote.core.model.delivery.DeliveryConditionItem
import com.afternote.core.model.delivery.DeliveryConditionType
import com.afternote.core.model.delivery.DeliveryContentType
import com.afternote.core.model.delivery.InactivityPeriod
import com.afternote.core.model.delivery.ReceiverDeliveryConditions
import com.afternote.core.model.user.ReceiverCreated
import com.afternote.core.model.user.User
import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.ui.UiText
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.domain.Passkey
import com.afternote.feature.setting.domain.UpdateTimeLetterDeliveryConditionUseCase
import com.afternote.feature.setting.domain.testing.FakeSettingAccountRepository
import com.afternote.feature.setting.domain.testing.FakeSettingAccountRepository.ConnectedAccountLinkCall
import com.afternote.feature.setting.presentation.account.ConnectedAccountsIntent
import com.afternote.feature.setting.presentation.account.ConnectedAccountsViewModel
import com.afternote.feature.setting.presentation.applock.AppLockSetupScreen
import com.afternote.feature.setting.presentation.applock.AppLockSetupViewModel
import com.afternote.feature.setting.presentation.applock.PinSetupStep
import com.afternote.feature.setting.presentation.delivery.DeliveryConditionError
import com.afternote.feature.setting.presentation.delivery.DeliveryConditionIntent
import com.afternote.feature.setting.presentation.delivery.DeliveryConditionViewModel
import com.afternote.feature.setting.presentation.home.SettingViewModel
import com.afternote.feature.setting.presentation.home.WithdrawConfirmScreen
import com.afternote.feature.setting.presentation.home.WithdrawUiState
import com.afternote.feature.setting.presentation.navigation.SettingRoute
import com.afternote.feature.setting.presentation.passkey.PassKeyListScreen
import com.afternote.feature.setting.presentation.passkey.PassKeyScreen
import com.afternote.feature.setting.presentation.profile.ProfileEditEvent
import com.afternote.feature.setting.presentation.profile.ProfileEditIntent
import com.afternote.feature.setting.presentation.profile.ProfileEditScreen
import com.afternote.feature.setting.presentation.profile.ProfileEditUiState
import com.afternote.feature.setting.presentation.profile.ProfileEditViewModel
import com.afternote.feature.setting.presentation.receiver.ReceiverRegisterViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.afternote.feature.setting.presentation.R as SettingR

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class SettingAccountSecurityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun profileLoadValidationAndUpdateFailure_preserveExactContract() {
        val loadFailureRepository =
            settingContractProfileRepository().apply {
                onGetMyProfile = { throw IllegalStateException("profile unavailable") }
            }
        val loadFailureViewModel = ProfileEditViewModel(loadFailureRepository, FakePhotoUploadRepository.strict())

        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            loadFailureViewModel.uiState.value == ProfileEditUiState.Error
        }
        composeRule.setContent {
            AfternoteTheme {
                ProfileEditScreen(
                    onBackClick = {},
                    onWithdrawGuideClick = {},
                    viewModel = loadFailureViewModel,
                )
            }
        }

        composeRule.onNodeWithText("프로필을 불러올 수 없습니다.").assertIsDisplayed()
        composeRule.runOnIdle { loadFailureViewModel.onIntent(ProfileEditIntent.UpdateProfile("새 이름", "01012345678")) }
        assertTrue(loadFailureRepository.profileUpdateCalls.isEmpty())

        val updateFailureRepository = settingContractProfileRepository()
        val updateFailureViewModel = ProfileEditViewModel(updateFailureRepository, FakePhotoUploadRepository.strict())
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            updateFailureViewModel.uiState.value is ProfileEditUiState.Success
        }
        updateFailureRepository.onUpdateMyProfile = { _, _, _ -> throw IllegalStateException("offline") }

        composeRule.runOnIdle { updateFailureViewModel.onIntent(ProfileEditIntent.UpdateProfile("   ", "")) }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            (updateFailureViewModel.uiState.value as? ProfileEditUiState.Success)?.pendingEvent != null
        }
        val event = (updateFailureViewModel.uiState.value as ProfileEditUiState.Success).pendingEvent
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            (updateFailureViewModel.uiState.value as? ProfileEditUiState.Success)?.isUpdating == false
        }

        assertEquals(
            listOf(ProfileUpdateCall(name = null, phone = null, profileImageUrl = null)),
            updateFailureRepository.profileUpdateCalls,
        )
        assertEquals(ProfileEditEvent.UpdateFailure, event)
    }

    @Test
    fun connectedAccountLinkAndUnlink_preservePreCallAndFailureBoundaries() {
        val linkRepository = settingContractAccountRepository()
        val linkViewModel = ConnectedAccountsViewModel(linkRepository)
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            !linkViewModel.uiState.value.isLoading
        }

        composeRule.runOnIdle { linkViewModel.onIntent(ConnectedAccountsIntent.Toggle(provider = "google", enabled = true)) }

        assertEquals("google", linkViewModel.uiState.value.pendingLinkProvider)
        assertTrue(linkRepository.connectedLinkCalls.isEmpty())

        linkRepository.onLinkConnectedAccount = { _, _ -> throw IllegalStateException("oauth rejected") }
        composeRule.runOnIdle { linkViewModel.onIntent(ConnectedAccountsIntent.Link(provider = "google", accessToken = "google-token")) }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            linkViewModel.uiState.value.pendingError == "계정 연결에 실패했습니다."
        }

        assertEquals(
            listOf(ConnectedAccountLinkCall(provider = "google", accessToken = "google-token")),
            linkRepository.connectedLinkCalls,
        )

        val unlinkRepository =
            settingContractAccountRepository().apply {
                onGetConnectedAccounts = { connectedAccounts(google = true) }
                onUnlinkConnectedAccount = { throw IllegalStateException("server error") }
            }
        val unlinkViewModel = ConnectedAccountsViewModel(unlinkRepository)
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            !unlinkViewModel.uiState.value.isLoading
        }

        assertTrue(unlinkRepository.connectedUnlinkCalls.isEmpty())
        composeRule.runOnIdle { unlinkViewModel.onIntent(ConnectedAccountsIntent.Toggle(provider = "google", enabled = false)) }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            unlinkRepository.connectedUnlinkCalls.size == 1
        }

        assertEquals(listOf("google"), unlinkRepository.connectedUnlinkCalls)
        assertEquals("계정 연결 해제에 실패했습니다.", unlinkViewModel.uiState.value.pendingError)
    }

    @Test
    fun receiverRegister_blankRequiredEmail_isRejectedBeforeRepositoryCall() {
        val repository = settingContractReceiverRepository()
        val viewModel = ReceiverRegisterViewModel(repository)

        composeRule.runOnIdle {
            viewModel.register(
                name = "김수신",
                relation = "가족",
                phone = "   ",
                email = "",
                message = null,
            )
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            viewModel.uiState.value.errorMessage == UiText.Resource(SettingR.string.setting_receiver_email_required)
        }

        assertTrue(repository.receiverCreateCalls.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun deliveryCondition_failureSendsExactReceiverAndConditionPayload() {
        val loadedConditions =
            listOf(
                deliveryCondition(
                    contentType = DeliveryContentType.TIME_LETTER,
                    conditionType = DeliveryConditionType.INACTIVITY,
                    inactivityPeriod = InactivityPeriod.SIX_MONTHS,
                ),
                deliveryCondition(
                    contentType = DeliveryContentType.DIARY,
                    conditionType = DeliveryConditionType.INACTIVITY,
                    inactivityPeriod = InactivityPeriod.ONE_YEAR,
                ),
            )
        val repository =
            settingContractReceiverRepository().apply {
                onGetReceiverDeliveryConditions = {
                    ReceiverDeliveryConditions(
                        receiverId = RECEIVER_ID,
                        conditions = loadedConditions,
                    )
                }
                onUpdateReceiverDeliveryConditions = { _, _ -> throw IllegalStateException("save failed") }
            }
        val viewModel =
            DeliveryConditionViewModel(
                route = SettingRoute.AfterDeliveryRoute(RECEIVER_ID),
                receiverRepository = repository,
                updateTimeLetterDeliveryCondition = UpdateTimeLetterDeliveryConditionUseCase(repository),
            )
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            viewModel.uiState.value.isInitialized
        }

        composeRule.runOnIdle {
            viewModel.onIntent(DeliveryConditionIntent.SelectConditionType(DeliveryConditionType.RECEIVER_REQUEST))
            viewModel.onIntent(DeliveryConditionIntent.Save)
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            viewModel.uiState.value.error == DeliveryConditionError.SAVE_FAILED
        }

        assertEquals(
            listOf(
                DeliveryUpdateCall(
                    receiverId = RECEIVER_ID,
                    conditions =
                        listOf(
                            loadedConditions[0].copy(
                                conditionType = DeliveryConditionType.RECEIVER_REQUEST,
                                inactivityPeriod = null,
                            ),
                            loadedConditions[1],
                        ),
                ),
            ),
            repository.deliveryUpdateCalls,
        )
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(loadedConditions, viewModel.uiState.value.conditions)
    }

    @Test
    fun appLockPinAndPasskey_preserveSensitiveStateAndEntryContracts() {
        val viewModel = AppLockSetupViewModel()
        val completedPins = mutableListOf<String>()
        var registerCalls = 0
        val screen = mutableStateOf(SecurityContractScreen.APP_LOCK)
        composeRule.setContent {
            AfternoteTheme {
                when (screen.value) {
                    SecurityContractScreen.APP_LOCK -> {
                        AppLockSetupScreen(
                            step = PinSetupStep.ENTER_NEW,
                            onPinComplete = completedPins::add,
                            onBack = {},
                            viewModel = viewModel,
                        )
                    }

                    SecurityContractScreen.PASSKEY_ENTRY -> {
                        PassKeyScreen(
                            onBackClick = {},
                            onRegisterClick = {
                                registerCalls += 1
                                screen.value = SecurityContractScreen.PASSKEY_LIST
                            },
                        )
                    }

                    SecurityContractScreen.PASSKEY_LIST -> {
                        PassKeyListScreen(
                            passkeys = listOf(Passkey(7L, "서버 패스키", "2026-09-06T10:00:00")),
                            isLoading = false,
                            errorMessage = null,
                            onBackClick = {},
                            onRegisterClick = {},
                            onRetryClick = {},
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("1").performClick()
        composeRule.onNodeWithText("2").performClick()
        composeRule.onNodeWithText("3").performClick()
        assertTrue(completedPins.isEmpty())

        composeRule.onNodeWithText("4").performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) { completedPins.size == 1 }

        assertEquals(listOf("1234"), completedPins)
        assertEquals("", viewModel.uiState.value.pin)
        assertFalse(viewModel.uiState.value.isComplete)

        composeRule.runOnIdle { screen.value = SecurityContractScreen.PASSKEY_ENTRY }

        composeRule.onNodeWithText("패스키 등록").performClick()
        assertEquals(1, registerCalls)

        composeRule.onNodeWithText("패스키 목록").assertIsDisplayed()
        composeRule.onNodeWithText("서버 패스키").assertIsDisplayed()
        composeRule.onNodeWithText("2026.09.06 10:00").assertIsDisplayed()
    }

    @Test
    fun withdrawFailure_requiresFinalConfirmationAndKeepsSession() {
        val authRepository = settingContractAuthRepository()
        val accountRepository =
            settingContractAccountRepository().apply {
                onDeleteAccount = { throw IllegalStateException("delete rejected") }
            }
        val viewModel = SettingViewModel(authRepository, settingContractProfileRepository(), accountRepository)
        var successCalls = 0
        composeRule.setContent {
            AfternoteTheme {
                WithdrawConfirmScreen(
                    uiState = viewModel.uiState.collectAsStateWithLifecycle().value,
                    onBackClick = {},
                    onWithdrawSuccess = { successCalls += 1 },
                    viewModel = viewModel,
                )
            }
        }

        composeRule.onNodeWithText("탈퇴하기").performClick()
        composeRule.onNodeWithText("문장이 일치하지 않습니다. 재입력해 주세요.").assertIsDisplayed()
        assertEquals(0, accountRepository.deleteAccountCalls)

        composeRule.onNodeWithText("탈퇴하겠습니다").performTextInput("탈퇴하겠습니다")
        composeRule.onNodeWithText("탈퇴하기").performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            accountRepository.deleteAccountCalls == 1
        }

        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            viewModel.uiState.value.withdraw == WithdrawUiState.Error
        }
        composeRule
            .onNodeWithText("회원 탈퇴에 실패했습니다. 잠시 후 다시 시도해 주세요.")
            .assertIsDisplayed()
        assertEquals(0, successCalls)
        assertEquals(0, authRepository.clearSessionCalls)
        assertTrue(runBlocking { authRepository.isLoggedIn.first() })
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
        const val RECEIVER_ID = 77L
    }
}

private enum class SecurityContractScreen {
    APP_LOCK,
    PASSKEY_ENTRY,
    PASSKEY_LIST,
}

private val DEFAULT_USER =
    User(
        name = "테스트 사용자",
        email = "test@afternote.local",
        phone = "01012345678",
        profileImageUrl = null,
    )

private fun connectedAccounts(google: Boolean = false) =
    UserConnectedAccount(
        local = true,
        google = google,
        naver = false,
        kakao = false,
        apple = false,
        localEmail = DEFAULT_USER.email,
        googleEmail = "google@afternote.local".takeIf { google },
        naverEmail = null,
        kakaoEmail = null,
        appleEmail = null,
    )

private fun deliveryCondition(
    contentType: DeliveryContentType,
    conditionType: DeliveryConditionType,
    inactivityPeriod: InactivityPeriod?,
) = DeliveryConditionItem(
    contentType = contentType,
    conditionType = conditionType,
    inactivityPeriod = inactivityPeriod,
    state = ConditionState.ACTIVE,
    fulfilled = false,
    gracePeriodStartedAt = null,
    fulfilledAt = null,
)

private fun settingContractProfileRepository(): FakeMyProfileRepository =
    FakeMyProfileRepository.strict().apply {
        onGetMyProfile = { DEFAULT_USER }
        onUpdateMyProfile = { _, _, _ -> DEFAULT_USER }
    }

private fun settingContractReceiverRepository(): FakeUserReceiverRepository =
    FakeUserReceiverRepository.strict().apply {
        onReceiverListFlow = { receiverState }
        onCreateReceiver = { _, _, _, _, _ -> ReceiverCreated(1L, "AUTH-1") }
        onGetReceiverDeliveryConditions = { receiverId -> ReceiverDeliveryConditions(receiverId, emptyList()) }
        onUpdateReceiverDeliveryConditions = { receiverId, conditions ->
            ReceiverDeliveryConditions(receiverId, conditions)
        }
    }

private fun settingContractAccountRepository(): FakeSettingAccountRepository =
    FakeSettingAccountRepository.strict().apply {
        onGetConnectedAccounts = { connectedAccounts() }
        onLinkConnectedAccount = { _, _ -> connectedAccounts(google = true) }
        onUnlinkConnectedAccount = { connectedAccounts() }
        onDeleteAccount = {}
    }

private fun settingContractAuthRepository(): FakeAuthRepository =
    FakeAuthRepository
        .strict(
            loggedIn = true,
            accessToken = "access",
            refreshToken = "refresh",
        ).apply {
            onIsLoggedIn = { loggedInState }
            onGetAccessToken = null
            onGetRefreshToken = null
            onClearSession = null
        }
