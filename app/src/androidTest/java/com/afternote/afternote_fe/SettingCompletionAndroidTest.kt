package com.afternote.afternote_fe

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.core.data.repoimpl.UserRepositoryImpl
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
import com.afternote.core.network.dto.ReceiverDetailDto
import com.afternote.core.network.dto.ReceiverListDto
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
import com.afternote.core.ui.theme.AfternoteTheme
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
import kotlinx.coroutines.flow.MutableStateFlow
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
import java.util.ArrayDeque

@RunWith(AndroidJUnit4::class)
class SettingCompletionAndroidTest {
    @get:Rule(order = 0)
    val composeRule = createComposeRule()

    @get:Rule(order = 1)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    @Test
    fun profileUpdate_success_emitsExactPayloadAndEventAfterPendingRequest() {
        setHarnessContent()
        val repository = CompletionUserRepository()
        val updateGate = repository.enqueueProfileUpdate()
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
        val repository = CompletionUserRepository()
        val newsletterGate = repository.enqueuePushUpdate()
        val mindRecordGate = repository.enqueuePushUpdate()
        val afterNoteGate = repository.enqueuePushUpdate()
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
            repository.completedPushUpdates == 1
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
            repository.completedPushUpdates == 2 && viewModel.uiState.value.isMindRecordOn
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
            repository.completedPushUpdates == 3
        }

        assertFalse(viewModel.uiState.value.isAfternoteOn)
        assertEquals(3, repository.pushUpdateCalls.size)
    }

    @Test
    fun connectedAccounts_linkThenUnlink_appliesExactPayloadAndServerState() {
        setHarnessContent()
        val repository = CompletionUserRepository()
        val linkGate = repository.enqueueConnectedLink()
        val unlinkGate = repository.enqueueConnectedUnlink()
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
        val repository = CompletionUserRepository()
        val firstGate = repository.enqueueReceiverCreate()
        val retryGate = repository.enqueueReceiverCreate()
        val viewModel = ReceiverRegisterViewModel(repository)
        val expectedCall =
            CompletionReceiverRegistrationCall(
                name = "김수신",
                relation = "가족",
                phone = null,
                email = "receiver@afternote.local",
                message = null,
            )

        composeRule.runOnIdle {
            viewModel.register(
                name = expectedCall.name,
                relation = expectedCall.relation,
                phone = "   ",
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
            viewModel.uiState.value.errorMessage == "수신자 등록에 실패했습니다."
        }
        assertFalse(viewModel.uiState.value.isLoading)

        composeRule.runOnIdle {
            viewModel.register(
                name = expectedCall.name,
                relation = expectedCall.relation,
                phone = "   ",
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
        val repository = CompletionUserRepository()
        val firstBasicGate = repository.enqueueReceiverUpdate()
        val retryBasicGate = repository.enqueueReceiverUpdate()
        val retryMessageGate = repository.enqueueReceiverMessageUpdate()
        val finalBasicGate = repository.enqueueReceiverUpdate()
        val finalMessageGate = repository.enqueueReceiverMessageUpdate()
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
            viewModel.uiState.value.errorMessage == "수신자 수정에 실패했습니다."
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
                "기본 정보는 수정됐지만 마지막 인사말 수정에 실패했습니다."
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
    fun deliveryCondition_failureThenRetry_succeedsWithExactReceiverPatch() {
        setHarnessContent()
        val repository = CompletionUserRepository()
        val initialConditions = completionDefaultDeliveryConditions()
        repository.deliveryConditions =
            ReceiverDeliveryConditions(receiverId = RECEIVER_ID, conditions = initialConditions)
        val firstGate = repository.enqueueDeliveryUpdate()
        val retryGate = repository.enqueueDeliveryUpdate()
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
        val authRepository = CompletionStatefulWithdrawalAuthRepository(calls)
        val repository = UserRepositoryImpl(userApi, authRepository)
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

private data class CompletionProfileUpdateCall(
    val name: String?,
    val phone: String?,
    val profileImageUrl: String?,
)

private data class CompletionPushUpdateCall(
    val timeLetter: Boolean?,
    val mindRecord: Boolean?,
    val afterNote: Boolean?,
)

private data class CompletionReceiverRegistrationCall(
    val name: String,
    val relation: String,
    val phone: String?,
    val email: String?,
    val message: String?,
)

private data class CompletionConnectedLinkCall(
    val provider: String,
    val accessToken: String,
)

private data class CompletionReceiverEditCall(
    val receiverId: Long,
    val name: String,
    val phone: String,
    val relation: String,
    val email: String,
)

private data class CompletionReceiverMessageCall(
    val receiverId: Long,
    val message: String,
)

private data class CompletionDeliveryUpdateCall(
    val receiverId: Long,
    val conditions: List<DeliveryConditionItem>,
)

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

private class CompletionUserRepository : UserRepository {
    private val profileUpdateGates = ArrayDeque<CompletableDeferred<Result<User>>>()
    private val pushUpdateGates = ArrayDeque<CompletableDeferred<Result<UserPushSetting>>>()
    private val receiverCreateGates = ArrayDeque<CompletableDeferred<Result<ReceiverCreated>>>()
    private val receiverUpdateGates = ArrayDeque<CompletableDeferred<Result<Receiver>>>()
    private val receiverMessageGates = ArrayDeque<CompletableDeferred<Result<Unit>>>()
    private val deliveryUpdateGates = ArrayDeque<CompletableDeferred<Result<ReceiverDeliveryConditions>>>()
    private val connectedLinkGates = ArrayDeque<CompletableDeferred<Result<UserConnectedAccount>>>()
    private val connectedUnlinkGates = ArrayDeque<CompletableDeferred<Result<UserConnectedAccount>>>()

    private val recordedProfileUpdates = mutableListOf<CompletionProfileUpdateCall>()
    private val recordedPushUpdates = mutableListOf<CompletionPushUpdateCall>()
    private val recordedReceiverCreates = mutableListOf<CompletionReceiverRegistrationCall>()
    private val recordedReceiverUpdates = mutableListOf<CompletionReceiverEditCall>()
    private val recordedReceiverMessages = mutableListOf<CompletionReceiverMessageCall>()
    private val recordedDeliveryUpdates = mutableListOf<CompletionDeliveryUpdateCall>()
    private val recordedConnectedLinks = mutableListOf<CompletionConnectedLinkCall>()
    private val recordedConnectedUnlinks = mutableListOf<String>()
    private var pushUpdateCompletions = 0

    override val receiverListFlow: Flow<List<Receiver>> = flowOf(emptyList())

    var deliveryConditions =
        ReceiverDeliveryConditions(receiverId = 77L, conditions = completionDefaultDeliveryConditions())

    val profileUpdateCalls: List<CompletionProfileUpdateCall>
        get() = synchronized(this) { recordedProfileUpdates.toList() }

    val pushUpdateCalls: List<CompletionPushUpdateCall>
        get() = synchronized(this) { recordedPushUpdates.toList() }

    val completedPushUpdates: Int
        get() = synchronized(this) { pushUpdateCompletions }

    val receiverCreateCalls: List<CompletionReceiverRegistrationCall>
        get() = synchronized(this) { recordedReceiverCreates.toList() }

    val receiverUpdateCalls: List<CompletionReceiverEditCall>
        get() = synchronized(this) { recordedReceiverUpdates.toList() }

    val receiverMessageCalls: List<CompletionReceiverMessageCall>
        get() = synchronized(this) { recordedReceiverMessages.toList() }

    val deliveryUpdateCalls: List<CompletionDeliveryUpdateCall>
        get() = synchronized(this) { recordedDeliveryUpdates.toList() }

    val connectedLinkCalls: List<CompletionConnectedLinkCall>
        get() = synchronized(this) { recordedConnectedLinks.toList() }

    val connectedUnlinkCalls: List<String>
        get() = synchronized(this) { recordedConnectedUnlinks.toList() }

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

    override suspend fun getMyProfile(): User = COMPLETION_DEFAULT_USER

    override suspend fun updateMyProfile(
        name: String?,
        phone: String?,
        profileImageUrl: String?,
    ): User {
        val gate =
            synchronized(this) {
                recordedProfileUpdates += CompletionProfileUpdateCall(name, phone, profileImageUrl)
                profileUpdateGates.pollFirst() ?: error("updateMyProfile gate was not prepared")
            }
        return gate.await().getOrThrow()
    }

    override suspend fun getMyPushSettings(): UserPushSetting = COMPLETION_DEFAULT_PUSH_SETTING

    override suspend fun updateMyPushSettings(
        timeLetter: Boolean?,
        mindRecord: Boolean?,
        afterNote: Boolean?,
    ): UserPushSetting {
        val gate =
            synchronized(this) {
                recordedPushUpdates += CompletionPushUpdateCall(timeLetter, mindRecord, afterNote)
                pushUpdateGates.pollFirst() ?: error("updateMyPushSettings gate was not prepared")
            }
        return try {
            gate.await().getOrThrow()
        } finally {
            synchronized(this) { pushUpdateCompletions += 1 }
        }
    }

    override suspend fun createReceiver(
        name: String,
        relation: String,
        phone: String?,
        email: String?,
        message: String?,
    ): ReceiverCreated {
        val gate =
            synchronized(this) {
                recordedReceiverCreates += CompletionReceiverRegistrationCall(name, relation, phone, email, message)
                receiverCreateGates.pollFirst() ?: error("createReceiver gate was not prepared")
            }
        return gate.await().getOrThrow()
    }

    override suspend fun getReceiverDeliveryConditions(receiverId: Long): ReceiverDeliveryConditions {
        assertEquals(deliveryConditions.receiverId, receiverId)
        return deliveryConditions
    }

    override suspend fun updateReceiverDeliveryConditions(
        receiverId: Long,
        conditions: List<DeliveryConditionItem>,
    ): ReceiverDeliveryConditions {
        val gate =
            synchronized(this) {
                recordedDeliveryUpdates += CompletionDeliveryUpdateCall(receiverId, conditions)
                deliveryUpdateGates.pollFirst() ?: error("updateReceiverDeliveryConditions gate was not prepared")
            }
        return gate.await().getOrThrow()
    }

    override suspend fun getReceivers(): List<Receiver> = completionUnexpected("getReceivers")

    override suspend fun getReceiverDetail(receiverId: Long): ReceiverDetail {
        assertEquals(COMPLETION_DEFAULT_RECEIVER_DETAIL.receiverId, receiverId)
        return COMPLETION_DEFAULT_RECEIVER_DETAIL
    }

    override suspend fun updateReceiver(
        receiverId: Long,
        name: String,
        phone: String,
        relation: String,
        email: String,
    ): Receiver {
        val gate =
            synchronized(this) {
                recordedReceiverUpdates += CompletionReceiverEditCall(receiverId, name, phone, relation, email)
                receiverUpdateGates.pollFirst() ?: error("updateReceiver gate was not prepared")
            }
        return gate.await().getOrThrow()
    }

    override suspend fun updateReceiverMessage(
        receiverId: Long,
        message: String,
    ) {
        val gate =
            synchronized(this) {
                recordedReceiverMessages += CompletionReceiverMessageCall(receiverId, message)
                receiverMessageGates.pollFirst() ?: error("updateReceiverMessage gate was not prepared")
            }
        gate.await().getOrThrow()
    }

    override suspend fun deleteAccount() = completionUnexpected<Unit>("deleteAccount")

    override suspend fun logActivity() = completionUnexpected<Unit>("logActivity")

    override suspend fun getConnectedAccounts(): UserConnectedAccount = completionConnectedAccounts()

    override suspend fun linkConnectedAccount(
        provider: String,
        accessToken: String,
    ): UserConnectedAccount {
        val gate =
            synchronized(this) {
                recordedConnectedLinks += CompletionConnectedLinkCall(provider, accessToken)
                connectedLinkGates.pollFirst() ?: error("linkConnectedAccount gate was not prepared")
            }
        return gate.await().getOrThrow()
    }

    override suspend fun unlinkConnectedAccount(provider: String): UserConnectedAccount {
        val gate =
            synchronized(this) {
                recordedConnectedUnlinks += provider
                connectedUnlinkGates.pollFirst() ?: error("unlinkConnectedAccount gate was not prepared")
            }
        return gate.await().getOrThrow()
    }
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

    override suspend fun logActivity(): BaseResponse<Unit> = completionUnexpected("logActivity")

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

private class CompletionStatefulWithdrawalAuthRepository(
    private val calls: CompletionCallLedger,
) : AuthRepository {
    private val loggedIn = MutableStateFlow(true)
    override val isLoggedIn: Flow<Boolean> = loggedIn

    private var clearCalls = 0
    val clearSessionCalls: Int
        get() = synchronized(this) { clearCalls }

    override suspend fun clearSession(): Result<Unit> {
        synchronized(this) { clearCalls += 1 }
        calls.record("clearSession")
        loggedIn.value = false
        return Result.success(Unit)
    }

    override suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
    ): Result<Unit> = completionUnexpected("saveSession")

    override suspend fun updateTokens(
        accessToken: String,
        refreshToken: String,
    ): Result<Unit> = completionUnexpected("updateTokens")

    override suspend fun getAccessToken(): Result<String?> = Result.success("access")

    override suspend fun getRefreshToken(): Result<String?> = Result.success("refresh")

    override suspend fun defaultLogin(
        email: String,
        password: String,
    ): Result<Session.DefaultSession> = completionUnexpected("defaultLogin")

    override suspend fun kakaoLogin(oauthToken: String): Result<Session.SocialSession> = completionUnexpected("kakaoLogin")

    override suspend fun googleLogin(idToken: String): Result<Session.SocialSession> = completionUnexpected("googleLogin")

    override suspend fun rotateToken(): Result<TokenBundle> = completionUnexpected("rotateToken")

    override suspend fun logout(): Result<Unit> = completionUnexpected("logout")
}

private fun <T> completionUnexpected(method: String): T = error("$method must not be called by this test")
