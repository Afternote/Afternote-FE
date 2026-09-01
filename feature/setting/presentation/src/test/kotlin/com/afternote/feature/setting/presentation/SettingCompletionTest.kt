package com.afternote.feature.setting.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.data.repoimpl.UserRepositoryImpl
import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.domain.testing.FakeUserRepository
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
import com.afternote.core.network.dto.DeletePushTokenRequestDto
import com.afternote.core.network.dto.PushTokenDto
import com.afternote.core.network.dto.ReceiverDetailDto
import com.afternote.core.network.dto.ReceiverListDto
import com.afternote.core.network.dto.RegisterPushTokenRequestDto
import com.afternote.core.network.dto.SocialAccountLinkRequestDto
import com.afternote.core.network.dto.UserConnectedAccountDto
import com.afternote.core.network.dto.UserCreateReceiverDto
import com.afternote.core.network.dto.UserCreateReceiverRequestDto
import com.afternote.core.network.dto.UserDto
import com.afternote.core.network.dto.UserPatchReceiverDto
import com.afternote.core.network.dto.UserPatchReceiverRequestDto
import com.afternote.core.network.dto.UserPushSettingDto
import com.afternote.core.network.dto.UserUpdateProfileRequestDto
import com.afternote.core.network.dto.UserUpdatePushSettingRequestDto
import com.afternote.core.network.dto.UserUpdateReceiverMessageRequestDto
import com.afternote.core.network.dto.delivery.ReceiverDeliveryConditionDto
import com.afternote.core.network.dto.delivery.ReceiverDeliveryConditionUpdateRequestDto
import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.service.UserApiService
import com.afternote.core.ui.UiText
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.screen.ReceiverEditScreen
import com.afternote.feature.setting.presentation.screen.ReceiverRegisterScreen
import com.afternote.feature.setting.presentation.viewmodel.ConnectedAccountsViewModel
import com.afternote.feature.setting.presentation.viewmodel.DeliveryConditionError
import com.afternote.feature.setting.presentation.viewmodel.DeliveryConditionViewModel
import com.afternote.feature.setting.presentation.viewmodel.ProfileEditEvent
import com.afternote.feature.setting.presentation.viewmodel.ProfileEditUiState
import com.afternote.feature.setting.presentation.viewmodel.ProfileEditViewModel
import com.afternote.feature.setting.presentation.viewmodel.PushNotificationViewModel
import com.afternote.feature.setting.presentation.viewmodel.ReceiverEditEvent
import com.afternote.feature.setting.presentation.viewmodel.ReceiverEditViewModel
import com.afternote.feature.setting.presentation.viewmodel.ReceiverRegisterEvent
import com.afternote.feature.setting.presentation.viewmodel.ReceiverRegisterViewModel
import com.afternote.feature.setting.presentation.viewmodel.SettingUiState
import com.afternote.feature.setting.presentation.viewmodel.SettingViewModel
import com.afternote.feature.setting.presentation.viewmodel.WithdrawUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
import java.util.ArrayDeque
import com.afternote.core.domain.testing.FakeUserRepository.ConnectedAccountLinkCall as CompletionConnectedLinkCall
import com.afternote.core.domain.testing.FakeUserRepository.DeliveryUpdateCall as CompletionDeliveryUpdateCall
import com.afternote.core.domain.testing.FakeUserRepository.ProfileUpdateCall as CompletionProfileUpdateCall
import com.afternote.core.domain.testing.FakeUserRepository.PushUpdateCall as CompletionPushUpdateCall
import com.afternote.core.domain.testing.FakeUserRepository.ReceiverCreateCall as CompletionReceiverRegistrationCall
import com.afternote.core.domain.testing.FakeUserRepository.ReceiverMessageCall as CompletionReceiverMessageCall
import com.afternote.core.domain.testing.FakeUserRepository.ReceiverUpdateCall as CompletionReceiverEditCall
import com.afternote.feature.setting.presentation.R as SettingR

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class SettingCompletionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun profileUpdate_success_emitsExactPayloadAndEventAfterPendingRequest() {
        setHarnessContent()
        val scenario = CompletionUserScenario()
        val repository = scenario.repository
        val updateGate = scenario.enqueueProfileUpdate()
        val viewModel = ProfileEditViewModel(repository)
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            viewModel.uiState.value is ProfileEditUiState.Success
        }

        composeRule.runOnIdle {
            viewModel.updateProfile(name = "새 이름", phone = "01098765432")
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            repository.profileUpdateCalls.size == 1
        }

        assertTrue((viewModel.uiState.value as ProfileEditUiState.Success).isUpdating)
        assertEquals(
            listOf(CompletionProfileUpdateCall(name = "새 이름", phone = "01098765432", profileImageUrl = null)),
            repository.profileUpdateCalls,
        )

        updateGate.complete(
            Result.success(COMPLETION_DEFAULT_USER.copy(name = "새 이름", phone = "01098765432")),
        )

        assertEquals(ProfileEditEvent.UpdateSuccess, awaitEvent(viewModel.events))
    }

    @Test
    fun pushUpdates_keepOptimisticSuccessAndRollbackFailureWithExactPatches() {
        setHarnessContent()
        val scenario = CompletionUserScenario()
        val repository = scenario.repository
        val newsletterGate = scenario.enqueuePushUpdate()
        val mindRecordGate = scenario.enqueuePushUpdate()
        val afterNoteGate = scenario.enqueuePushUpdate()
        val viewModel =
            PushNotificationViewModel(
                context = ApplicationProvider.getApplicationContext(),
                userRepository = repository,
            )
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            !viewModel.uiState.value.isLoading
        }

        composeRule.runOnIdle { viewModel.onNewsletterToggle(false) }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            repository.pushUpdateCalls.size == 1
        }

        assertFalse(viewModel.uiState.value.isNewsletterOn)
        assertEquals(
            listOf(CompletionPushUpdateCall(timeLetter = false, mindRecord = null, afterNote = null)),
            repository.pushUpdateCalls,
        )

        newsletterGate.complete(
            Result.success(COMPLETION_DEFAULT_PUSH_SETTING.copy(timeLetter = false)),
        )
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            scenario.completedPushUpdates == 1
        }
        assertFalse(viewModel.uiState.value.isNewsletterOn)

        composeRule.runOnIdle { viewModel.onMindRecordToggle(false) }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            repository.pushUpdateCalls.size == 2
        }

        assertFalse(viewModel.uiState.value.isMindRecordOn)
        assertEquals(
            listOf(
                CompletionPushUpdateCall(timeLetter = false, mindRecord = null, afterNote = null),
                CompletionPushUpdateCall(timeLetter = null, mindRecord = false, afterNote = null),
            ),
            repository.pushUpdateCalls,
        )

        mindRecordGate.complete(Result.failure(IllegalStateException("offline")))
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            scenario.completedPushUpdates == 2 && viewModel.uiState.value.isMindRecordOn
        }

        assertTrue(viewModel.uiState.value.isMindRecordOn)
        assertFalse(viewModel.uiState.value.isNewsletterOn)

        composeRule.runOnIdle {
            viewModel.onAfternoteToggle(false)
            viewModel.onSmsChecked(true)
            viewModel.onEmailChecked(true)
            viewModel.onPushChecked(true)
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            repository.pushUpdateCalls.size == 3
        }

        assertFalse(viewModel.uiState.value.isAfternoteOn)
        assertTrue(viewModel.uiState.value.isSmsChecked)
        assertTrue(viewModel.uiState.value.isEmailChecked)
        assertTrue(viewModel.uiState.value.isPushChecked)
        assertEquals(
            CompletionPushUpdateCall(timeLetter = null, mindRecord = null, afterNote = false),
            repository.pushUpdateCalls.last(),
        )

        afterNoteGate.complete(
            Result.success(COMPLETION_DEFAULT_PUSH_SETTING.copy(timeLetter = false, afterNote = false)),
        )
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            scenario.completedPushUpdates == 3
        }

        assertFalse(viewModel.uiState.value.isAfternoteOn)
        assertEquals(3, repository.pushUpdateCalls.size)
    }

    @Test
    fun connectedAccounts_linkThenUnlink_appliesExactPayloadAndServerState() {
        setHarnessContent()
        val scenario = CompletionUserScenario()
        val repository = scenario.repository
        val linkGate = scenario.enqueueConnectedLink()
        val unlinkGate = scenario.enqueueConnectedUnlink()
        val viewModel = ConnectedAccountsViewModel(repository)
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            !viewModel.uiState.value.isLoading
        }

        composeRule.runOnIdle {
            viewModel.link(provider = "google", accessToken = "google-access-token")
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            repository.connectedLinkCalls.size == 1
        }
        assertEquals(
            listOf(CompletionConnectedLinkCall("google", "google-access-token")),
            repository.connectedLinkCalls,
        )

        linkGate.complete(
            Result.success(
                completionConnectedAccounts(
                    google = true,
                    googleEmail = "linked@afternote.local",
                ),
            ),
        )
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            viewModel.uiState.value.accounts
                .first { it.provider == "google" }
                .isConnected
        }
        assertEquals(
            "linked@afternote.local",
            viewModel.uiState.value.accounts
                .first { it.provider == "google" }
                .email,
        )

        composeRule.runOnIdle {
            viewModel.onToggle(provider = "google", enabled = false)
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            repository.connectedUnlinkCalls.size == 1
        }
        assertEquals(listOf("google"), repository.connectedUnlinkCalls)

        unlinkGate.complete(Result.success(completionConnectedAccounts()))
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            !viewModel.uiState.value.accounts
                .first { it.provider == "google" }
                .isConnected
        }

        assertEquals(
            null,
            viewModel.uiState.value.accounts
                .first { it.provider == "google" }
                .email,
        )
    }

    @Test
    fun receiverRegistration_failureThenRetry_succeedsWithSameNormalizedPayload() {
        setHarnessContent()
        val scenario = CompletionUserScenario()
        val repository = scenario.repository
        val firstGate = scenario.enqueueReceiverCreate()
        val retryGate = scenario.enqueueReceiverCreate()
        val viewModel = ReceiverRegisterViewModel(repository)
        val expectedCall =
            CompletionReceiverRegistrationCall(
                name = "김수신",
                relation = "가족",
                phone = "01012345678",
                email = "receiver@afternote.local",
                message = null,
            )

        composeRule.runOnIdle {
            viewModel.register(
                name = expectedCall.name,
                relation = expectedCall.relation,
                phone = expectedCall.phone,
                email = expectedCall.email,
                message = "",
            )
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            repository.receiverCreateCalls.size == 1
        }
        assertTrue(viewModel.uiState.value.isLoading)

        firstGate.complete(Result.failure(IllegalStateException("temporary failure")))
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            viewModel.uiState.value.errorMessage == UiText.Resource(SettingR.string.receiver_register_failed)
        }
        assertFalse(viewModel.uiState.value.isLoading)

        composeRule.runOnIdle {
            viewModel.register(
                name = expectedCall.name,
                relation = expectedCall.relation,
                phone = expectedCall.phone,
                email = expectedCall.email,
                message = "",
            )
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            repository.receiverCreateCalls.size == 2
        }

        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(null, viewModel.uiState.value.errorMessage)
        assertEquals(listOf(expectedCall, expectedCall), repository.receiverCreateCalls)

        retryGate.complete(Result.success(ReceiverCreated(receiverId = RECEIVER_ID, authCode = "AUTH-77")))

        assertEquals(ReceiverRegisterEvent.RegisterSuccess, awaitEvent(viewModel.events))
    }

    @Test
    fun receiverEdit_serialFailurePartialFailureThenRetry_sendsExactPayloadOncePerAttempt() {
        setHarnessContent()
        val scenario = CompletionUserScenario()
        val repository = scenario.repository
        val firstBasicGate = scenario.enqueueReceiverUpdate()
        val retryBasicGate = scenario.enqueueReceiverUpdate()
        val retryMessageGate = scenario.enqueueReceiverMessageUpdate()
        val finalBasicGate = scenario.enqueueReceiverUpdate()
        val finalMessageGate = scenario.enqueueReceiverMessageUpdate()
        val viewModel =
            ReceiverEditViewModel(
                savedStateHandle = SavedStateHandle(mapOf("receiverId" to RECEIVER_ID)),
                userRepository = repository,
            )
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            viewModel.uiState.value.receiver == COMPLETION_DEFAULT_RECEIVER_DETAIL
        }
        val expectedBasicCall =
            CompletionReceiverEditCall(
                receiverId = RECEIVER_ID,
                name = "수정 이름",
                phone = "01099998888",
                relation = "가족",
                email = "updated@afternote.local",
            )
        val expectedMessageCall =
            CompletionReceiverMessageCall(
                receiverId = RECEIVER_ID,
                message = "수정한 마지막 인사말",
            )
        val update: () -> Unit = {
            viewModel.update(
                name = expectedBasicCall.name,
                relation = expectedBasicCall.relation,
                phone = expectedBasicCall.phone,
                email = expectedBasicCall.email,
                message = expectedMessageCall.message,
            )
        }

        composeRule.runOnIdle {
            update()
            update()
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            repository.receiverUpdateCalls.size == 1
        }
        assertTrue(viewModel.uiState.value.isSaving)
        assertEquals(listOf(expectedBasicCall), repository.receiverUpdateCalls)
        assertTrue(repository.receiverMessageCalls.isEmpty())

        firstBasicGate.complete(Result.failure(IllegalStateException("basic update failed")))
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            viewModel.uiState.value.errorMessage == UiText.Resource(SettingR.string.receiver_edit_failed)
        }
        assertFalse(viewModel.uiState.value.isSaving)
        assertTrue(repository.receiverMessageCalls.isEmpty())

        composeRule.runOnIdle(update)
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            repository.receiverUpdateCalls.size == 2
        }
        retryBasicGate.complete(Result.success(completionUpdatedReceiver(expectedBasicCall)))
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            repository.receiverMessageCalls.size == 1
        }
        retryMessageGate.complete(Result.failure(IllegalStateException("message update failed")))
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            viewModel.uiState.value.errorMessage ==
                UiText.Resource(SettingR.string.receiver_message_update_partial_failed)
        }
        assertEquals(listOf(expectedMessageCall), repository.receiverMessageCalls)

        composeRule.runOnIdle(update)
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            repository.receiverUpdateCalls.size == 3
        }
        finalBasicGate.complete(Result.success(completionUpdatedReceiver(expectedBasicCall)))
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            repository.receiverMessageCalls.size == 2
        }
        finalMessageGate.complete(Result.success(Unit))

        assertEquals(ReceiverEditEvent.EditSuccess, awaitEvent(viewModel.events))
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            !viewModel.uiState.value.isSaving
        }
        assertEquals(
            listOf(expectedBasicCall, expectedBasicCall, expectedBasicCall),
            repository.receiverUpdateCalls,
        )
        assertEquals(listOf(expectedMessageCall, expectedMessageCall), repository.receiverMessageCalls)
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun receiverEdit_blankPhoneWiring_disablesSaveAndShowsRequiredMessage() {
        val scenario =
            CompletionUserScenario().apply {
                receiverDetail = COMPLETION_DEFAULT_RECEIVER_DETAIL.copy(phone = null)
            }
        val repository = scenario.repository
        val viewModel =
            ReceiverEditViewModel(
                savedStateHandle = SavedStateHandle(mapOf("receiverId" to RECEIVER_ID)),
                userRepository = repository,
            )

        composeRule.setContent {
            AfternoteTheme {
                ReceiverEditScreen(
                    onBackClick = {},
                    onEditSuccess = {},
                    viewModel = viewModel,
                )
            }
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            viewModel.uiState.value.receiver == scenario.receiverDetail
        }

        composeRule.onNodeWithText("연락처를 입력해주세요.").assertIsDisplayed()
        composeRule.onNodeWithText("수정").assertIsNotEnabled()
        assertTrue(repository.receiverUpdateCalls.isEmpty())
    }

    @Test
    fun receiverRegister_blankPhoneWiring_disablesSubmitAndShowsRequiredMessage() {
        val repository = FakeUserRepository(receivers = emptyList())
        val viewModel = ReceiverRegisterViewModel(repository)

        composeRule.setContent {
            AfternoteTheme {
                ReceiverRegisterScreen(
                    onBackClick = {},
                    onRegisterSuccess = {},
                    viewModel = viewModel,
                )
            }
        }

        composeRule.onNodeWithText("이름을 입력하세요").performTextInput("김수신")
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("afternote@email.com"))
        composeRule.onNodeWithText("afternote@email.com").performTextInput("receiver@afternote.local")

        composeRule.onNodeWithText("연락처를 입력해주세요.").assertIsDisplayed()
        composeRule.onNodeWithText("등록").assertIsNotEnabled()
        assertTrue(repository.receiverCreateCalls.isEmpty())
    }

    @Test
    fun deliveryCondition_failureThenRetry_succeedsWithExactReceiverPatch() {
        setHarnessContent()
        val scenario = CompletionUserScenario()
        val repository = scenario.repository
        val initialConditions = completionDefaultDeliveryConditions()
        scenario.deliveryConditions =
            ReceiverDeliveryConditions(receiverId = RECEIVER_ID, conditions = initialConditions)
        val firstGate = scenario.enqueueDeliveryUpdate()
        val retryGate = scenario.enqueueDeliveryUpdate()
        val viewModel =
            DeliveryConditionViewModel(
                savedStateHandle = SavedStateHandle(mapOf("receiverId" to RECEIVER_ID)),
                userRepository = repository,
            )
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            viewModel.uiState.value.isInitialized
        }
        val expectedConditions =
            listOf(
                initialConditions[0].copy(
                    conditionType = DeliveryConditionType.RECEIVER_REQUEST,
                    inactivityPeriod = null,
                ),
                initialConditions[1],
            )
        val expectedCall = CompletionDeliveryUpdateCall(RECEIVER_ID, expectedConditions)

        composeRule.runOnIdle {
            viewModel.onConditionTypeSelected(index = 1)
            viewModel.onSave()
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            repository.deliveryUpdateCalls.size == 1
        }
        assertTrue(viewModel.uiState.value.isSaving)
        assertEquals(listOf(expectedCall), repository.deliveryUpdateCalls)

        firstGate.complete(Result.failure(IllegalStateException("temporary failure")))
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            viewModel.uiState.value.error == DeliveryConditionError.SAVE_FAILED
        }

        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(initialConditions, viewModel.uiState.value.conditions)

        composeRule.runOnIdle { viewModel.onSave() }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            repository.deliveryUpdateCalls.size == 2
        }
        assertTrue(viewModel.uiState.value.isSaving)
        assertEquals(listOf(expectedCall, expectedCall), repository.deliveryUpdateCalls)

        val serverConditions =
            expectedConditions.map { condition ->
                if (condition.contentType == DeliveryContentType.TIME_LETTER) {
                    condition.copy(state = ConditionState.WAITING_VERIFICATION)
                } else {
                    condition
                }
            }
        retryGate.complete(
            Result.success(
                ReceiverDeliveryConditions(receiverId = RECEIVER_ID, conditions = serverConditions),
            ),
        )

        assertEquals(Unit, awaitEvent(viewModel.saveSuccess))
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            viewModel.uiState.value.conditions == serverConditions
        }
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun withdrawal_failureThenRetry_cleansSessionExactlyOnceAfterServerSuccess() {
        setHarnessContent()
        val calls = CompletionCallLedger()
        val userApi = CompletionGatedWithdrawalUserApi(calls)
        val firstGate = userApi.enqueueDelete()
        val retryGate = userApi.enqueueDelete()
        val authRepository = completionStatefulWithdrawalAuthRepository(calls)
        val repository = UserRepositoryImpl(userApi, authRepository, NoopErrorReporter)
        val viewModel = SettingViewModel(authRepository, repository)
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            viewModel.uiState.value is SettingUiState.Success
        }

        composeRule.runOnIdle { viewModel.deleteAccount() }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            userApi.deleteCalls == 1
        }
        assertEquals(0, authRepository.clearSessionCalls)

        firstGate.complete(BaseResponse(status = 503, code = 503, message = "retry"))
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            userApi.completedDeleteCalls == 1
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            viewModel.withdrawUiState.value == WithdrawUiState.Error
        }
        assertEquals(0, authRepository.clearSessionCalls)
        assertTrue(runBlocking { authRepository.isLoggedIn.first() })

        composeRule.runOnIdle { viewModel.deleteAccount() }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            userApi.deleteCalls == 2
        }
        assertEquals(0, authRepository.clearSessionCalls)

        retryGate.complete(BaseResponse(status = 200, code = 200))
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            viewModel.withdrawUiState.value == WithdrawUiState.Success
        }

        assertEquals(2, userApi.completedDeleteCalls)
        assertEquals(1, authRepository.clearSessionCalls)
        assertFalse(runBlocking { authRepository.isLoggedIn.first() })
        assertEquals(
            listOf("deleteAccount#1", "deleteAccount#2", "clearSession"),
            calls.snapshot(),
        )
    }

    private fun setHarnessContent() {
        composeRule.setContent { AfternoteTheme {} }
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

private object NoopErrorReporter : ErrorReporter {
    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) = Unit
}

private val COMPLETION_DEFAULT_USER =
    User(
        name = "테스트 사용자",
        email = "test@afternote.local",
        phone = "01012345678",
        profileImageUrl = null,
    )

private val COMPLETION_DEFAULT_PUSH_SETTING =
    UserPushSetting(
        timeLetter = true,
        mindRecord = true,
        afterNote = true,
    )

private val COMPLETION_DEFAULT_RECEIVER_DETAIL =
    ReceiverDetail(
        receiverId = 77L,
        name = "기존 수신자",
        relation = "가족",
        phone = "01011112222",
        email = "before@afternote.local",
        dailyQuestionCount = 1,
        timeLetterCount = 2,
        afterNoteCount = 3,
        message = "기존 마지막 인사말",
        authCode = "AUTH-77",
    )

private fun completionConnectedAccounts(
    google: Boolean = false,
    googleEmail: String? = null,
) = UserConnectedAccount(
    local = true,
    google = google,
    naver = false,
    kakao = false,
    apple = false,
    localEmail = COMPLETION_DEFAULT_USER.email,
    googleEmail = googleEmail,
    naverEmail = null,
    kakaoEmail = null,
    appleEmail = null,
)

private fun completionUpdatedReceiver(call: CompletionReceiverEditCall) =
    Receiver(
        receiverId = call.receiverId,
        name = call.name,
        relation = call.relation,
        authCode = "AUTH-77",
    )

private fun completionDefaultDeliveryConditions() =
    listOf(
        DeliveryConditionItem(
            contentType = DeliveryContentType.TIME_LETTER,
            conditionType = DeliveryConditionType.INACTIVITY,
            inactivityPeriod = InactivityPeriod.SIX_MONTHS,
            state = ConditionState.ACTIVE,
            fulfilled = false,
            gracePeriodStartedAt = null,
            fulfilledAt = null,
        ),
        DeliveryConditionItem(
            contentType = DeliveryContentType.DIARY,
            conditionType = DeliveryConditionType.INACTIVITY,
            inactivityPeriod = InactivityPeriod.ONE_YEAR,
            state = ConditionState.ACTIVE,
            fulfilled = false,
            gracePeriodStartedAt = null,
            fulfilledAt = null,
        ),
    )

private class CompletionUserScenario {
    private val profileUpdateGates = ArrayDeque<CompletableDeferred<Result<User>>>()
    private val pushUpdateGates = ArrayDeque<CompletableDeferred<Result<UserPushSetting>>>()
    private val receiverCreateGates = ArrayDeque<CompletableDeferred<Result<ReceiverCreated>>>()
    private val receiverUpdateGates = ArrayDeque<CompletableDeferred<Result<Receiver>>>()
    private val receiverMessageGates = ArrayDeque<CompletableDeferred<Result<Unit>>>()
    private val deliveryUpdateGates = ArrayDeque<CompletableDeferred<Result<ReceiverDeliveryConditions>>>()
    private val connectedLinkGates = ArrayDeque<CompletableDeferred<Result<UserConnectedAccount>>>()
    private val connectedUnlinkGates = ArrayDeque<CompletableDeferred<Result<UserConnectedAccount>>>()
    private var pushUpdateCompletions = 0

    var deliveryConditions =
        ReceiverDeliveryConditions(receiverId = 77L, conditions = completionDefaultDeliveryConditions())

    var receiverDetail: ReceiverDetail = COMPLETION_DEFAULT_RECEIVER_DETAIL

    val completedPushUpdates: Int
        get() = synchronized(this) { pushUpdateCompletions }

    val repository =
        FakeUserRepository.strict().apply {
            onReceiverListFlow = { flowOf(emptyList()) }
            onGetMyProfile = { COMPLETION_DEFAULT_USER }
            onUpdateMyProfile = { _, _, _ ->
                takeGate(profileUpdateGates, "updateMyProfile").await().getOrThrow()
            }
            onGetMyPushSettings = { COMPLETION_DEFAULT_PUSH_SETTING }
            onUpdateMyPushSettings = { _, _, _ ->
                val gate = takeGate(pushUpdateGates, "updateMyPushSettings")
                try {
                    gate.await().getOrThrow()
                } finally {
                    synchronized(this@CompletionUserScenario) { pushUpdateCompletions += 1 }
                }
            }
            onCreateReceiver = { _, _, _, _, _ ->
                takeGate(receiverCreateGates, "createReceiver").await().getOrThrow()
            }
            onGetReceiverDeliveryConditions = { receiverId ->
                assertEquals(this@CompletionUserScenario.deliveryConditions.receiverId, receiverId)
                this@CompletionUserScenario.deliveryConditions
            }
            onUpdateReceiverDeliveryConditions = { _, _ ->
                takeGate(deliveryUpdateGates, "updateReceiverDeliveryConditions").await().getOrThrow()
            }
            onGetReceiverDetail = { receiverId ->
                assertEquals(this@CompletionUserScenario.receiverDetail.receiverId, receiverId)
                this@CompletionUserScenario.receiverDetail
            }
            onUpdateReceiver = { _, _, _, _, _ ->
                takeGate(receiverUpdateGates, "updateReceiver").await().getOrThrow()
            }
            onUpdateReceiverMessage = { _, _ ->
                takeGate(receiverMessageGates, "updateReceiverMessage").await().getOrThrow()
            }
            onGetConnectedAccounts = { completionConnectedAccounts() }
            onLinkConnectedAccount = { _, _ ->
                takeGate(connectedLinkGates, "linkConnectedAccount").await().getOrThrow()
            }
            onUnlinkConnectedAccount = {
                takeGate(connectedUnlinkGates, "unlinkConnectedAccount").await().getOrThrow()
            }
        }

    fun enqueueProfileUpdate(): CompletableDeferred<Result<User>> =
        CompletableDeferred<Result<User>>().also { gate ->
            synchronized(this) { profileUpdateGates.addLast(gate) }
        }

    fun enqueuePushUpdate(): CompletableDeferred<Result<UserPushSetting>> =
        CompletableDeferred<Result<UserPushSetting>>().also { gate ->
            synchronized(this) { pushUpdateGates.addLast(gate) }
        }

    fun enqueueReceiverCreate(): CompletableDeferred<Result<ReceiverCreated>> =
        CompletableDeferred<Result<ReceiverCreated>>().also { gate ->
            synchronized(this) { receiverCreateGates.addLast(gate) }
        }

    fun enqueueReceiverUpdate(): CompletableDeferred<Result<Receiver>> =
        CompletableDeferred<Result<Receiver>>().also { gate ->
            synchronized(this) { receiverUpdateGates.addLast(gate) }
        }

    fun enqueueReceiverMessageUpdate(): CompletableDeferred<Result<Unit>> =
        CompletableDeferred<Result<Unit>>().also { gate ->
            synchronized(this) { receiverMessageGates.addLast(gate) }
        }

    fun enqueueDeliveryUpdate(): CompletableDeferred<Result<ReceiverDeliveryConditions>> =
        CompletableDeferred<Result<ReceiverDeliveryConditions>>().also { gate ->
            synchronized(this) { deliveryUpdateGates.addLast(gate) }
        }

    fun enqueueConnectedLink(): CompletableDeferred<Result<UserConnectedAccount>> =
        CompletableDeferred<Result<UserConnectedAccount>>().also { gate ->
            synchronized(this) { connectedLinkGates.addLast(gate) }
        }

    fun enqueueConnectedUnlink(): CompletableDeferred<Result<UserConnectedAccount>> =
        CompletableDeferred<Result<UserConnectedAccount>>().also { gate ->
            synchronized(this) { connectedUnlinkGates.addLast(gate) }
        }

    private fun <T> takeGate(
        gates: ArrayDeque<T>,
        method: String,
    ): T = synchronized(this) { gates.pollFirst() ?: error("$method gate was not prepared") }
}

private class CompletionCallLedger {
    private val calls = mutableListOf<String>()

    fun record(call: String) {
        synchronized(this) { calls += call }
    }

    fun snapshot(): List<String> = synchronized(this) { calls.toList() }
}

private class CompletionGatedWithdrawalUserApi(
    private val calls: CompletionCallLedger,
) : UserApiService {
    private val deleteGates = ArrayDeque<CompletableDeferred<BaseResponse<Unit>>>()
    private var startedDeletes = 0
    private var completedDeletes = 0

    val deleteCalls: Int
        get() = synchronized(this) { startedDeletes }

    val completedDeleteCalls: Int
        get() = synchronized(this) { completedDeletes }

    fun enqueueDelete(): CompletableDeferred<BaseResponse<Unit>> =
        CompletableDeferred<BaseResponse<Unit>>().also { gate ->
            synchronized(this) { deleteGates.addLast(gate) }
        }

    override suspend fun registerPushToken(request: RegisterPushTokenRequestDto): BaseResponse<PushTokenDto> = TODO("이 테스트 미사용")

    override suspend fun deletePushToken(request: DeletePushTokenRequestDto): BaseResponse<Unit> = TODO("이 테스트 미사용")

    override suspend fun getMyProfile(): BaseResponse<UserDto> =
        BaseResponse(
            status = 200,
            code = 200,
            data =
                UserDto(
                    name = COMPLETION_DEFAULT_USER.name,
                    email = COMPLETION_DEFAULT_USER.email,
                    phone = COMPLETION_DEFAULT_USER.phone,
                    profileImageUrl = COMPLETION_DEFAULT_USER.profileImageUrl,
                ),
        )

    override suspend fun deleteAccount(): BaseResponse<Unit> {
        val callNumber: Int
        val gate: CompletableDeferred<BaseResponse<Unit>>
        synchronized(this) {
            startedDeletes += 1
            callNumber = startedDeletes
            gate = deleteGates.pollFirst() ?: error("deleteAccount gate was not prepared")
        }
        calls.record("deleteAccount#$callNumber")
        return try {
            gate.await()
        } finally {
            synchronized(this) { completedDeletes += 1 }
        }
    }

    override suspend fun getReceivers(): BaseResponse<List<ReceiverListDto>> = completionUnexpected("getReceivers")

    override suspend fun createReceiver(request: UserCreateReceiverRequestDto): BaseResponse<UserCreateReceiverDto> =
        completionUnexpected("createReceiver")

    override suspend fun getReceiverDetail(receiverId: Long): BaseResponse<ReceiverDetailDto> = completionUnexpected("getReceiverDetail")

    override suspend fun updateReceiver(
        receiverId: Long,
        request: UserPatchReceiverRequestDto,
    ): BaseResponse<UserPatchReceiverDto> = completionUnexpected("updateReceiver")

    override suspend fun updateReceiverMessage(
        receiverId: Long,
        request: UserUpdateReceiverMessageRequestDto,
    ): BaseResponse<Unit> = completionUnexpected("updateReceiverMessage")

    override suspend fun updateMyProfile(request: UserUpdateProfileRequestDto): BaseResponse<UserDto> =
        completionUnexpected("updateMyProfile")

    override suspend fun getMyPushSettings(): BaseResponse<UserPushSettingDto> = completionUnexpected("getMyPushSettings")

    override suspend fun updateMyPushSettings(request: UserUpdatePushSettingRequestDto): BaseResponse<UserPushSettingDto> =
        completionUnexpected("updateMyPushSettings")

    override suspend fun getConnectedAccounts(): BaseResponse<UserConnectedAccountDto> = completionUnexpected("getConnectedAccounts")

    override suspend fun linkConnectedAccount(
        provider: String,
        request: SocialAccountLinkRequestDto,
    ): BaseResponse<UserConnectedAccountDto> = completionUnexpected("linkConnectedAccount")

    override suspend fun unlinkConnectedAccount(provider: String): BaseResponse<UserConnectedAccountDto> =
        completionUnexpected("unlinkConnectedAccount")

    override suspend fun getReceiverDeliveryConditions(receiverId: Long): BaseResponse<ReceiverDeliveryConditionDto> =
        completionUnexpected("getReceiverDeliveryConditions")

    override suspend fun updateReceiverDeliveryConditions(
        receiverId: Long,
        request: ReceiverDeliveryConditionUpdateRequestDto,
    ): BaseResponse<ReceiverDeliveryConditionDto> = completionUnexpected("updateReceiverDeliveryConditions")
}

private fun completionStatefulWithdrawalAuthRepository(calls: CompletionCallLedger): FakeAuthRepository =
    FakeAuthRepository
        .strict(
            loggedIn = true,
            accessToken = "access",
            refreshToken = "refresh",
        ).apply {
            onIsLoggedIn = { loggedInState }
            onGetAccessToken = null
            onGetRefreshToken = null
            onClearSession = {
                calls.record("clearSession")
                accessToken = null
                refreshToken = null
                loggedIn = false
                Result.success(Unit)
            }
        }

private fun <T> completionUnexpected(method: String): T = error("$method must not be called by this test")
