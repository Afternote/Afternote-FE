package com.afternote.feature.setting.presentation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.domain.testing.FakeUserRepository.ConnectedAccountLinkCall
import com.afternote.core.domain.testing.FakeUserRepository.DeliveryUpdateCall
import com.afternote.core.domain.testing.FakeUserRepository.ProfileUpdateCall
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
import com.afternote.feature.setting.presentation.component.PinSetupStep
import com.afternote.feature.setting.presentation.screen.AppLockSetupScreen
import com.afternote.feature.setting.presentation.screen.PassKeyListScreen
import com.afternote.feature.setting.presentation.screen.PassKeyScreen
import com.afternote.feature.setting.presentation.screen.ProfileEditScreen
import com.afternote.feature.setting.presentation.screen.WithdrawConfirmScreen
import com.afternote.feature.setting.presentation.viewmodel.AppLockSetupViewModel
import com.afternote.feature.setting.presentation.viewmodel.ConnectedAccountsEvent
import com.afternote.feature.setting.presentation.viewmodel.ConnectedAccountsViewModel
import com.afternote.feature.setting.presentation.viewmodel.DeliveryConditionError
import com.afternote.feature.setting.presentation.viewmodel.DeliveryConditionViewModel
import com.afternote.feature.setting.presentation.viewmodel.ProfileEditEvent
import com.afternote.feature.setting.presentation.viewmodel.ProfileEditUiState
import com.afternote.feature.setting.presentation.viewmodel.ProfileEditViewModel
import com.afternote.feature.setting.presentation.viewmodel.ReceiverRegisterViewModel
import com.afternote.feature.setting.presentation.viewmodel.SettingUiState
import com.afternote.feature.setting.presentation.viewmodel.SettingViewModel
import com.afternote.feature.setting.presentation.viewmodel.WithdrawUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
            settingContractUserRepository().apply {
                onGetMyProfile = { throw IllegalStateException("profile unavailable") }
            }
        val loadFailureViewModel = ProfileEditViewModel(loadFailureRepository)

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
        composeRule.runOnIdle { loadFailureViewModel.updateProfile("새 이름", "01012345678") }
        assertTrue(loadFailureRepository.profileUpdateCalls.isEmpty())

        val updateFailureRepository = settingContractUserRepository()
        val updateFailureViewModel = ProfileEditViewModel(updateFailureRepository)
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            updateFailureViewModel.uiState.value is ProfileEditUiState.Success
        }
        updateFailureRepository.onUpdateMyProfile = { _, _, _ -> throw IllegalStateException("offline") }

        composeRule.runOnIdle { updateFailureViewModel.updateProfile("   ", "") }
        val event = awaitEvent(updateFailureViewModel.events)
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
        val linkRepository = settingContractUserRepository()
        val linkViewModel = ConnectedAccountsViewModel(linkRepository)
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            !linkViewModel.uiState.value.isLoading
        }

        composeRule.runOnIdle { linkViewModel.onToggle(provider = "google", enabled = true) }
        val request = awaitEvent(linkViewModel.events)

        assertEquals(ConnectedAccountsEvent.RequestLink("google"), request)
        assertTrue(linkRepository.connectedLinkCalls.isEmpty())

        linkRepository.onLinkConnectedAccount = { _, _ -> throw IllegalStateException("oauth rejected") }
        composeRule.runOnIdle { linkViewModel.link(provider = "google", accessToken = "google-token") }
        val linkFailure = awaitEvent(linkViewModel.events)

        assertEquals(ConnectedAccountsEvent.ShowError("계정 연결에 실패했습니다."), linkFailure)
        assertEquals(
            listOf(ConnectedAccountLinkCall(provider = "google", accessToken = "google-token")),
            linkRepository.connectedLinkCalls,
        )

        val unlinkRepository =
            settingContractUserRepository().apply {
                onGetConnectedAccounts = { connectedAccounts(google = true) }
                onUnlinkConnectedAccount = { throw IllegalStateException("server error") }
            }
        val unlinkViewModel = ConnectedAccountsViewModel(unlinkRepository)
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            !unlinkViewModel.uiState.value.isLoading
        }

        assertTrue(unlinkRepository.connectedUnlinkCalls.isEmpty())
        composeRule.runOnIdle { unlinkViewModel.onToggle(provider = "google", enabled = false) }
        val unlinkFailure = awaitEvent(unlinkViewModel.events)

        assertEquals(listOf("google"), unlinkRepository.connectedUnlinkCalls)
        assertEquals(ConnectedAccountsEvent.ShowError("계정 연결 해제에 실패했습니다."), unlinkFailure)
    }

    @Test
    fun receiverRegister_blankRequiredEmail_isRejectedBeforeRepositoryCall() {
        val repository = settingContractUserRepository()
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
            viewModel.uiState.value.errorMessage == UiText.Resource(SettingR.string.receiver_email_required)
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
            settingContractUserRepository().apply {
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
                savedStateHandle = SavedStateHandle(mapOf("receiverId" to RECEIVER_ID)),
                userRepository = repository,
            )
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            viewModel.uiState.value.isInitialized
        }

        composeRule.runOnIdle {
            viewModel.onConditionTypeSelected(index = 1)
            viewModel.onSave()
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
                        PassKeyListScreen(onBackClick = {})
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
        composeRule.onNodeWithText("이름").assertIsDisplayed()
        composeRule.onNodeWithText("생성일시").assertIsDisplayed()
    }

    @Test
    fun withdrawFailure_requiresFinalConfirmationAndKeepsSession() {
        val authRepository = settingContractAuthRepository()
        val userRepository =
            settingContractUserRepository().apply {
                onDeleteAccount = { throw IllegalStateException("delete rejected") }
            }
        val viewModel = SettingViewModel(authRepository, userRepository)
        var successCalls = 0
        composeRule.setContent {
            AfternoteTheme {
                WithdrawConfirmScreen(
                    uiState = SettingUiState.Success(name = DEFAULT_USER.name, email = DEFAULT_USER.email),
                    onBackClick = {},
                    onWithdrawSuccess = { successCalls += 1 },
                    viewModel = viewModel,
                )
            }
        }

        composeRule.onNodeWithText("탈퇴하기").performClick()
        composeRule.onNodeWithText("문장이 일치하지 않습니다. 재입력해 주세요.").assertIsDisplayed()
        assertEquals(0, userRepository.deleteAccountCalls)

        composeRule.onNodeWithText("탈퇴하겠습니다").performTextInput("탈퇴하겠습니다")
        composeRule.onNodeWithText("탈퇴하기").performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            userRepository.deleteAccountCalls == 1
        }

        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            viewModel.withdrawUiState.value == WithdrawUiState.Error
        }
        composeRule
            .onNodeWithText("회원 탈퇴에 실패했습니다. 잠시 후 다시 시도해 주세요.")
            .assertIsDisplayed()
        assertEquals(0, successCalls)
        assertEquals(0, authRepository.clearSessionCalls)
        assertTrue(runBlocking { authRepository.isLoggedIn.first() })
    }

    private fun <T> awaitEvent(events: Flow<T>): T =
        runBlocking {
            withTimeout(TIMEOUT_MILLIS) { events.first() }
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

private fun settingContractUserRepository(): FakeUserRepository =
    FakeUserRepository.strict().apply {
        onReceiverListFlow = { receiverState }
        onGetMyProfile = { DEFAULT_USER }
        onUpdateMyProfile = { _, _, _ -> DEFAULT_USER }
        onGetConnectedAccounts = { connectedAccounts() }
        onLinkConnectedAccount = { _, _ -> connectedAccounts(google = true) }
        onUnlinkConnectedAccount = { connectedAccounts() }
        onCreateReceiver = { _, _, _, _, _ -> ReceiverCreated(1L, "AUTH-1") }
        onGetReceiverDeliveryConditions = { receiverId -> ReceiverDeliveryConditions(receiverId, emptyList()) }
        onUpdateReceiverDeliveryConditions = { receiverId, conditions ->
            ReceiverDeliveryConditions(receiverId, conditions)
        }
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
