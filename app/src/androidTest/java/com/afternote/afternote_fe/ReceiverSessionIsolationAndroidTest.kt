package com.afternote.afternote_fe

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.afternote_fe.test.FakeErrorReporter
import com.afternote.core.data.repoimpl.UserRepositoryImpl
import com.afternote.core.domain.testing.FakeAuthRepository
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
import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.service.UserApiService
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.timeletter.presentation.screen.sender.RecipientListScreen
import com.afternote.feature.timeletter.presentation.viewmodel.RecipientListViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.ArrayDeque

@RunWith(AndroidJUnit4::class)
class ReceiverSessionIsolationAndroidTest {
    @get:Rule(order = 0)
    val composeRule = createComposeRule()

    @get:Rule(order = 1)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    @Test
    fun recipientList_newSession401_keepsScreenAliveWithoutPreviousReceiverIdentity() {
        val authRepository =
            FakeAuthRepository.strict(loggedIn = true).apply {
                onIsLoggedIn = { loggedInState }
            }
        val userApi = SessionReceiverUserApi()
        userApi.enqueueReceivers(
            Result.success(
                BaseResponse(
                    status = 200,
                    code = 200,
                    data = listOf(receiverDto(PREVIOUS_ACCOUNT_RECEIVER)),
                ),
            ),
        )
        userApi.enqueueReceivers(
            Result.failure(
                ApiException(
                    status = 401,
                    code = 401,
                    serverMessage = "인증되지 않은 요청입니다.",
                    fallbackMessage = "인증되지 않은 요청입니다.",
                ),
            ),
        )
        val repository = UserRepositoryImpl(userApi, authRepository, FakeErrorReporter())
        val viewModel = RecipientListViewModel(repository)

        composeRule.setContent {
            AfternoteTheme {
                RecipientListScreen(
                    onBackClick = {},
                    onConfirmClick = {},
                    viewModel = viewModel,
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            userApi.receiverCalls == 1 && viewModel.recipients.value.any { it.name == PREVIOUS_ACCOUNT_RECEIVER }
        }
        composeRule.onNodeWithText(PREVIOUS_ACCOUNT_RECEIVER).assertIsDisplayed()

        composeRule.runOnIdle { authRepository.loggedIn = false }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            viewModel.recipients.value.isEmpty()
        }
        assertEquals(1, userApi.receiverCalls)
        composeRule.onNodeWithText(PREVIOUS_ACCOUNT_RECEIVER).assertDoesNotExist()

        composeRule.runOnIdle { authRepository.loggedIn = true }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            userApi.receiverCalls == 2
        }

        // 401을 예외로 방출했다면 이 시점 전에 테스트 프로세스가 죽는다. 실제 화면이 남고 이전 계정의
        // 식별자가 다시 나타나지 않는지를 함께 확인해 «크래시 방지 + 세션 격리» 경계를 고정한다.
        composeRule.onNodeWithText("수신인 목록").assertIsDisplayed()
        composeRule.onNodeWithText(PREVIOUS_ACCOUNT_RECEIVER).assertDoesNotExist()
        assertEquals(2, userApi.receiverCalls)
    }

    private companion object {
        const val PREVIOUS_ACCOUNT_RECEIVER = "이전 계정 수신인"
        const val TIMEOUT_MILLIS = 5_000L
    }
}

private fun receiverDto(name: String) =
    ReceiverListDto(
        receiverId = 91L,
        name = name,
        relation = "가족",
        authCode = "previous-account-auth",
    )

private class SessionReceiverUserApi : UserApiService {
    private val receiverResults = ArrayDeque<Result<BaseResponse<List<ReceiverListDto>>>>()

    @Volatile
    var receiverCalls: Int = 0
        private set

    fun enqueueReceivers(result: Result<BaseResponse<List<ReceiverListDto>>>) {
        synchronized(this) { receiverResults.addLast(result) }
    }

    override suspend fun getReceivers(): BaseResponse<List<ReceiverListDto>> {
        val result =
            synchronized(this) {
                receiverCalls += 1
                receiverResults.pollFirst() ?: error("getReceivers result was not prepared")
            }
        return result.getOrThrow()
    }

    override suspend fun createReceiver(request: UserCreateReceiverRequestDto): BaseResponse<UserCreateReceiverDto> =
        receiverSessionUnexpected("createReceiver")

    override suspend fun getReceiverDetail(receiverId: Long): BaseResponse<ReceiverDetailDto> =
        receiverSessionUnexpected("getReceiverDetail")

    override suspend fun updateReceiver(
        receiverId: Long,
        request: UserPatchReceiverRequestDto,
    ): BaseResponse<UserPatchReceiverDto> = receiverSessionUnexpected("updateReceiver")

    override suspend fun updateReceiverMessage(
        receiverId: Long,
        request: UserUpdateReceiverMessageRequestDto,
    ): BaseResponse<Unit> = receiverSessionUnexpected("updateReceiverMessage")

    override suspend fun getMyProfile(): BaseResponse<UserDto> = receiverSessionUnexpected("getMyProfile")

    override suspend fun updateMyProfile(request: UserUpdateProfileRequestDto): BaseResponse<UserDto> =
        receiverSessionUnexpected("updateMyProfile")

    override suspend fun deleteAccount(): BaseResponse<Unit> = receiverSessionUnexpected("deleteAccount")

    override suspend fun logActivity(): BaseResponse<Unit> = receiverSessionUnexpected("logActivity")

    override suspend fun getMyPushSettings(): BaseResponse<UserPushSettingDto> = receiverSessionUnexpected("getMyPushSettings")

    override suspend fun updateMyPushSettings(request: UserUpdatePushSettingRequestDto): BaseResponse<UserPushSettingDto> =
        receiverSessionUnexpected("updateMyPushSettings")

    override suspend fun getConnectedAccounts(): BaseResponse<UserConnectedAccountDto> = receiverSessionUnexpected("getConnectedAccounts")

    override suspend fun linkConnectedAccount(
        provider: String,
        request: SocialAccountLinkRequestDto,
    ): BaseResponse<UserConnectedAccountDto> = receiverSessionUnexpected("linkConnectedAccount")

    override suspend fun unlinkConnectedAccount(provider: String): BaseResponse<UserConnectedAccountDto> =
        receiverSessionUnexpected("unlinkConnectedAccount")

    override suspend fun getReceiverDeliveryConditions(receiverId: Long): BaseResponse<ReceiverDeliveryConditionDto> =
        receiverSessionUnexpected("getReceiverDeliveryConditions")

    override suspend fun updateReceiverDeliveryConditions(
        receiverId: Long,
        request: ReceiverDeliveryConditionUpdateRequestDto,
    ): BaseResponse<ReceiverDeliveryConditionDto> = receiverSessionUnexpected("updateReceiverDeliveryConditions")
}

private fun <T> receiverSessionUnexpected(method: String): T = error("$method must not be called by this test")
