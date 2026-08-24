package com.afternote.afternote_fe

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.model.Session
import com.afternote.core.model.TokenBundle
import com.afternote.core.model.delivery.ConditionState
import com.afternote.core.model.delivery.DeliveryConditionItem
import com.afternote.core.model.delivery.DeliveryConditionType
import com.afternote.core.model.delivery.DeliveryContentType
import com.afternote.core.model.delivery.InactivityPeriod
import com.afternote.core.model.delivery.ReceiverDeliveryConditions
import com.afternote.core.model.user.Receiver
import com.afternote.core.model.user.ReceiverCreated
import com.afternote.core.model.user.ReceiverDetail
import com.afternote.core.model.user.User
import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.model.user.UserPushSetting
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingAccountSecurityAndroidTest {
    @get:Rule(order = 0)
    val composeRule = createComposeRule()

    @get:Rule(order = 1)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    @Test
    fun profileLoadValidationAndUpdateFailure_preserveExactContract() {
        val loadFailureRepository =
            SettingContractUserRepository().apply {
                profileResult = Result.failure(IllegalStateException("profile unavailable"))
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

        val updateFailureRepository = SettingContractUserRepository()
        val updateFailureViewModel = ProfileEditViewModel(updateFailureRepository)
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            updateFailureViewModel.uiState.value is ProfileEditUiState.Success
        }
        updateFailureRepository.profileUpdateResult = Result.failure(IllegalStateException("offline"))

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
        val linkRepository = SettingContractUserRepository()
        val linkViewModel = ConnectedAccountsViewModel(linkRepository)
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            !linkViewModel.uiState.value.isLoading
        }

        composeRule.runOnIdle { linkViewModel.onToggle(provider = "google", enabled = true) }
        val request = awaitEvent(linkViewModel.events)

        assertEquals(ConnectedAccountsEvent.RequestLink("google"), request)
        assertTrue(linkRepository.linkCalls.isEmpty())

        linkRepository.linkResult = Result.failure(IllegalStateException("oauth rejected"))
        composeRule.runOnIdle { linkViewModel.link(provider = "google", accessToken = "google-token") }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            linkViewModel.uiState.value.errorMessage == "계정 연결에 실패했습니다."
        }

        assertEquals(
            listOf(ConnectedAccountLinkCall(provider = "google", accessToken = "google-token")),
            linkRepository.linkCalls,
        )

        val unlinkRepository =
            SettingContractUserRepository().apply {
                connectedAccountsResult = Result.success(connectedAccounts(google = true))
                unlinkResult = Result.failure(IllegalStateException("server error"))
            }
        val unlinkViewModel = ConnectedAccountsViewModel(unlinkRepository)
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            !unlinkViewModel.uiState.value.isLoading
        }

        assertTrue(unlinkRepository.unlinkCalls.isEmpty())
        composeRule.runOnIdle { unlinkViewModel.onToggle(provider = "google", enabled = false) }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            unlinkRepository.unlinkCalls.size == 1
        }

        assertEquals(listOf("google"), unlinkRepository.unlinkCalls)
        assertEquals("계정 연결 해제에 실패했습니다.", unlinkViewModel.uiState.value.errorMessage)
    }

    @Test
    fun receiverRegister_blankOptionalFieldsAndFailure_preserveExactPayload() {
        val repository =
            SettingContractUserRepository().apply {
                receiverCreateResult = Result.failure(IllegalStateException("offline"))
            }
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
            viewModel.uiState.value.errorMessage == "수신자 등록에 실패했습니다."
        }

        assertEquals(
            listOf(
                ReceiverRegistrationCall(
                    name = "김수신",
                    relation = "가족",
                    phone = null,
                    email = null,
                    message = null,
                ),
            ),
            repository.receiverRegistrationCalls,
        )
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
            SettingContractUserRepository().apply {
                deliveryLoadResult =
                    Result.success(
                        ReceiverDeliveryConditions(
                            receiverId = RECEIVER_ID,
                            conditions = loadedConditions,
                        ),
                    )
                deliveryUpdateResult = Result.failure(IllegalStateException("save failed"))
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
        val authRepository = SettingContractAuthRepository()
        val userRepository =
            SettingContractUserRepository().apply {
                deleteAccountResult = Result.failure(IllegalStateException("delete rejected"))
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
        composeRule.onNodeWithText("회원 탈퇴가 완료되었습니다.\n애프터노트를 이용해 주셔서 감사합니다.").assertIsDisplayed()
        assertEquals(0, userRepository.deleteAccountCalls)

        composeRule.onNodeWithText("확인하기").performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            userRepository.deleteAccountCalls == 1
        }

        assertFalse(viewModel.withdrawCompleted.value)
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

private data class ProfileUpdateCall(
    val name: String?,
    val phone: String?,
    val profileImageUrl: String?,
)

private data class ConnectedAccountLinkCall(
    val provider: String,
    val accessToken: String,
)

private data class ReceiverRegistrationCall(
    val name: String,
    val relation: String,
    val phone: String?,
    val email: String?,
    val message: String?,
)

private data class DeliveryUpdateCall(
    val receiverId: Long,
    val conditions: List<DeliveryConditionItem>,
)

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

private class SettingContractUserRepository : UserRepository {
    private val receivers = MutableStateFlow<List<Receiver>>(emptyList())
    override val receiverListFlow: Flow<List<Receiver>> = receivers

    var profileResult: Result<User> = Result.success(DEFAULT_USER)
    var profileUpdateResult: Result<User> = Result.success(DEFAULT_USER)
    var connectedAccountsResult: Result<UserConnectedAccount> = Result.success(connectedAccounts())
    var linkResult: Result<UserConnectedAccount> = Result.success(connectedAccounts(google = true))
    var unlinkResult: Result<UserConnectedAccount> = Result.success(connectedAccounts())
    var receiverCreateResult: Result<ReceiverCreated> = Result.success(ReceiverCreated(1L, "AUTH-1"))
    var deliveryLoadResult: Result<ReceiverDeliveryConditions> =
        Result.success(ReceiverDeliveryConditions(receiverId = 77L, conditions = emptyList()))
    var deliveryUpdateResult: Result<ReceiverDeliveryConditions> = deliveryLoadResult
    var deleteAccountResult: Result<Unit> = Result.success(Unit)

    val profileUpdateCalls = mutableListOf<ProfileUpdateCall>()
    val linkCalls = mutableListOf<ConnectedAccountLinkCall>()
    val unlinkCalls = mutableListOf<String>()
    val receiverRegistrationCalls = mutableListOf<ReceiverRegistrationCall>()
    val deliveryUpdateCalls = mutableListOf<DeliveryUpdateCall>()
    var deleteAccountCalls = 0
        private set

    override suspend fun getMyProfile(): User = profileResult.getOrThrow()

    override suspend fun updateMyProfile(
        name: String?,
        phone: String?,
        profileImageUrl: String?,
    ): User {
        profileUpdateCalls += ProfileUpdateCall(name, phone, profileImageUrl)
        return profileUpdateResult.getOrThrow()
    }

    override suspend fun getConnectedAccounts(): UserConnectedAccount = connectedAccountsResult.getOrThrow()

    override suspend fun linkConnectedAccount(
        provider: String,
        accessToken: String,
    ): UserConnectedAccount {
        linkCalls += ConnectedAccountLinkCall(provider, accessToken)
        return linkResult.getOrThrow()
    }

    override suspend fun unlinkConnectedAccount(provider: String): UserConnectedAccount {
        unlinkCalls += provider
        return unlinkResult.getOrThrow()
    }

    override suspend fun createReceiver(
        name: String,
        relation: String,
        phone: String?,
        email: String?,
        message: String?,
    ): ReceiverCreated {
        receiverRegistrationCalls += ReceiverRegistrationCall(name, relation, phone, email, message)
        return receiverCreateResult.getOrThrow()
    }

    override suspend fun getReceiverDeliveryConditions(receiverId: Long): ReceiverDeliveryConditions = deliveryLoadResult.getOrThrow()

    override suspend fun updateReceiverDeliveryConditions(
        receiverId: Long,
        conditions: List<DeliveryConditionItem>,
    ): ReceiverDeliveryConditions {
        deliveryUpdateCalls += DeliveryUpdateCall(receiverId, conditions)
        return deliveryUpdateResult.getOrThrow()
    }

    override suspend fun deleteAccount() {
        deleteAccountCalls += 1
        deleteAccountResult.getOrThrow()
    }

    override suspend fun getReceivers(): List<Receiver> = unexpected("getReceivers")

    override suspend fun getReceiverDetail(receiverId: Long): ReceiverDetail = unexpected("getReceiverDetail")

    override suspend fun updateReceiver(
        receiverId: Long,
        name: String,
        phone: String,
        relation: String,
        email: String,
    ): Receiver = unexpected("updateReceiver")

    override suspend fun updateReceiverMessage(
        receiverId: Long,
        message: String,
    ) = unexpected<Unit>("updateReceiverMessage")

    override suspend fun logActivity() = unexpected<Unit>("logActivity")

    override suspend fun getMyPushSettings(): UserPushSetting = unexpected("getMyPushSettings")

    override suspend fun updateMyPushSettings(
        timeLetter: Boolean?,
        mindRecord: Boolean?,
        afterNote: Boolean?,
    ): UserPushSetting = unexpected("updateMyPushSettings")
}

private class SettingContractAuthRepository : AuthRepository {
    private val loggedInState = MutableStateFlow(true)
    override val isLoggedIn: Flow<Boolean> = loggedInState

    var clearSessionCalls = 0
        private set

    override suspend fun clearSession(): Result<Unit> {
        clearSessionCalls += 1
        loggedInState.value = false
        return Result.success(Unit)
    }

    override suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
    ): Result<Unit> = unexpected("saveSession")

    override suspend fun updateTokens(
        accessToken: String,
        refreshToken: String,
    ): Result<Unit> = unexpected("updateTokens")

    override suspend fun getAccessToken(): Result<String?> = Result.success("access")

    override suspend fun getRefreshToken(): Result<String?> = Result.success("refresh")

    override suspend fun defaultLogin(
        email: String,
        password: String,
    ): Result<Session.DefaultSession> = unexpected("defaultLogin")

    override suspend fun kakaoLogin(oauthToken: String): Result<Session.SocialSession> = unexpected("kakaoLogin")

    override suspend fun googleLogin(idToken: String): Result<Session.SocialSession> = unexpected("googleLogin")

    override suspend fun rotateToken(): Result<TokenBundle> = unexpected("rotateToken")

    override suspend fun logout(): Result<Unit> = unexpected("logout")
}

private fun <T> unexpected(method: String): T = error("$method must not be called by this contract test")
