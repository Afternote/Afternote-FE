package com.afternote.core.data.repoimpl

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.model.user.Receiver
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
import com.afternote.core.network.dto.UserMarketingConsentDto
import com.afternote.core.network.dto.UserPatchReceiverDto
import com.afternote.core.network.dto.UserPatchReceiverRequestDto
import com.afternote.core.network.dto.UserPushSettingDto
import com.afternote.core.network.dto.UserUpdateMarketingConsentRequestDto
import com.afternote.core.network.dto.UserUpdateProfileRequestDto
import com.afternote.core.network.dto.UserUpdatePushSettingRequestDto
import com.afternote.core.network.dto.UserUpdateReceiverMessageRequestDto
import com.afternote.core.network.dto.delivery.ReceiverDeliveryConditionDto
import com.afternote.core.network.dto.delivery.ReceiverDeliveryConditionUpdateRequestDto
import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.service.UserApiService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * 수신자 수정 성공 뒤 구독 중인 목록 갱신 회귀 (#2127).
 *
 * `createReceiver` 와 달리 `updateReceiver` 는 목록 revision 을 올리지 않았다. 설정 › 수신인 수정에서
 * 저장하고 목록으로 돌아와도 `WhileSubscribed(5_000)` 로 살아 있는 **같은 구독** 이 옛 이름·관계를
 * 그대로 보여 준다. 구독이 끊겼다 붙는 느린 복귀에서만 우연히 최신 목록이 보였다.
 *
 * 그래서 여기서는 구독을 한 번도 끊지 않는다. 화면·ViewModel 을 세우지 않고 저장소 공개 계약
 * ([UserReceiverRepository.receiverListFlow] · [UserReceiverRepository.updateReceiver])만으로 고정한다.
 *
 * 400·409 를 도메인 거절로 바꾸는 매핑 자체는 같은 helper 를 공유하는 [ReceiverRequestFailureTest] 가
 * 지킨다. 이 파일이 보는 실패는 실패·취소에는 목록 재조회를 걸지 않는다 쪽이다.
 */
class ReceiverUpdateRefreshTest {
    @Test
    fun `updateReceiver - 성공하면 재구독 없이 같은 구독자가 수정된 이름과 관계를 받는다`() =
        runBlocking {
            val apiService = MutableReceiverApiService(name = "원래 이름", relation = "친구")
            val repository = receiverRepository(apiService)
            val emissions = Channel<List<Receiver>>(capacity = Channel.UNLIMITED)
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    repository.receiverListFlow.collect { emissions.send(it) }
                }

            try {
                val before = withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() }.single()
                assertEquals(listOf("원래 이름", "친구"), listOf(before.name, before.relation))

                val updated = requestUpdate(repository, name = "바뀐 이름", relation = "가족")

                val after = withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() }.single()
                assertEquals(listOf("바뀐 이름", "가족"), listOf(after.name, after.relation))
                assertEquals(2, apiService.getReceiversCallCount)
                // 갱신을 얹어도 수정 호출 자체의 반환값은 그대로다.
                assertEquals(Receiver(RECEIVER_ID, "바뀐 이름", "가족"), updated)
            } finally {
                collector.cancelAndJoin()
            }
        }

    @Test
    fun `updateReceiver - 서버가 실패하면 원본 오류를 올리고 목록을 다시 조회하지 않는다`() {
        val apiError =
            ApiException(
                status = 500,
                code = 500,
                serverMessage = null,
                fallbackMessage = "일시적인 서버 오류",
            )

        assertSame(apiError, failedUpdateError(apiError))
    }

    @Test
    fun `updateReceiver - 취소된 수정도 목록을 다시 조회하지 않는다`() {
        val cancellation = CancellationException("cancelled")

        assertSame(cancellation, failedUpdateError(cancellation))
    }

    /**
     * 실패한 PATCH 하나를 구독 중인 목록에 물려 보고, 올라온 오류를 돌려준다. 오류를 그대로 올리는지는
     * 호출부가 보고, 여기서는 재조회가 걸리지 않았는지 를 확인한다.
     */
    private fun failedUpdateError(failure: Throwable): Throwable? =
        runBlocking {
            val apiService = MutableReceiverApiService(name = "원래 이름", relation = "친구", failUpdateWith = failure)
            val repository = receiverRepository(apiService)
            val emissions = Channel<List<Receiver>>(capacity = Channel.UNLIMITED)
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    repository.receiverListFlow.collect { emissions.send(it) }
                }

            try {
                assertEquals("원래 이름", withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() }.single().name)

                val raised = runCatching { requestUpdate(repository, name = "바뀐 이름", relation = "가족") }.exceptionOrNull()

                assertEquals(1, apiService.updateReceiverCallCount)
                drainPendingRefresh()
                assertEquals(1, apiService.getReceiversCallCount)
                assertNull(emissions.tryReceive().getOrNull())
                raised
            } finally {
                collector.cancelAndJoin()
            }
        }

    private fun receiverRepository(apiService: UserApiService) =
        UserReceiverRepositoryImpl(
            userApiService = apiService,
            authRepository = loggedInAuthRepository(),
            errorReporter = SilentErrorReporter,
        )

    private fun loggedInAuthRepository(): FakeAuthRepository =
        FakeAuthRepository.strict(loggedIn = true).apply {
            onIsLoggedIn = { loggedInState }
        }

    private suspend fun requestUpdate(
        repository: UserReceiverRepository,
        name: String,
        relation: String,
    ): Receiver =
        repository.updateReceiver(
            receiverId = RECEIVER_ID,
            name = name,
            phone = "01012345678",
            relation = relation,
            email = "receiver@example.com",
        )

    /**
     * revision 을 올려도 목록 조회는 그 자리에서 일어나지 않는다. 구독자 코루틴이 깨어나야 한다. 잘못
     * 얹은 갱신이 아직 안 돌았을 뿐 인 채로 초록이 되지 않도록 runBlocking 이벤트 루프를 몇 바퀴 돌린다.
     */
    private suspend fun drainPendingRefresh() = repeat(DISPATCH_ROUNDS) { yield() }
}

private const val RECEIVER_ID = 7L
private const val TEST_TIMEOUT_MILLIS = 2_000L
private const val DISPATCH_ROUNDS = 5

private object SilentErrorReporter : ErrorReporter {
    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) = Unit
}

/** PATCH 를 실제 서버처럼 다음 조회에 반영 한다. 목록이 낡았는지는 이 반영을 봐야 드러난다. */
private class MutableReceiverApiService(
    name: String,
    relation: String,
    private val failUpdateWith: Throwable? = null,
) : UserApiService {
    var getReceiversCallCount = 0
        private set

    var updateReceiverCallCount = 0
        private set

    private var serverName = name
    private var serverRelation = relation

    override suspend fun getReceivers(): BaseResponse<List<ReceiverListDto>> {
        getReceiversCallCount += 1
        return BaseResponse(
            status = 200,
            code = 200,
            data =
                listOf(
                    ReceiverListDto(
                        receiverId = RECEIVER_ID,
                        name = serverName,
                        relation = serverRelation,
                    ),
                ),
        )
    }

    override suspend fun updateReceiver(
        receiverId: Long,
        request: UserPatchReceiverRequestDto,
    ): BaseResponse<UserPatchReceiverDto> {
        updateReceiverCallCount += 1
        failUpdateWith?.let { throw it }
        serverName = request.name
        serverRelation = request.relation
        return BaseResponse(
            status = 200,
            code = 200,
            data =
                UserPatchReceiverDto(
                    receiverId = receiverId,
                    name = request.name,
                    phone = request.phone,
                    relation = request.relation,
                    email = request.email,
                ),
        )
    }

    override suspend fun createReceiver(request: UserCreateReceiverRequestDto): BaseResponse<UserCreateReceiverDto> = TODO("이 테스트 미사용")

    override suspend fun getReceiverDetail(receiverId: Long): BaseResponse<ReceiverDetailDto> = TODO("이 테스트 미사용")

    override suspend fun updateReceiverMessage(
        receiverId: Long,
        request: UserUpdateReceiverMessageRequestDto,
    ): BaseResponse<Unit> = TODO("이 테스트 미사용")

    override suspend fun getMyProfile(): BaseResponse<UserDto> = TODO("이 테스트 미사용")

    override suspend fun updateMyProfile(request: UserUpdateProfileRequestDto): BaseResponse<UserDto> = TODO("이 테스트 미사용")

    override suspend fun deleteAccount(): BaseResponse<Unit> = TODO("이 테스트 미사용")

    override suspend fun registerPushToken(request: RegisterPushTokenRequestDto): BaseResponse<PushTokenDto> = TODO("이 테스트 미사용")

    override suspend fun deletePushToken(request: DeletePushTokenRequestDto): BaseResponse<Unit> = TODO("이 테스트 미사용")

    override suspend fun getMyPushSettings(): BaseResponse<UserPushSettingDto> = TODO("이 테스트 미사용")

    override suspend fun updateMyPushSettings(request: UserUpdatePushSettingRequestDto): BaseResponse<UserPushSettingDto> =
        TODO("이 테스트 미사용")

    override suspend fun getMyMarketingConsents(): BaseResponse<UserMarketingConsentDto> = TODO("이 테스트 미사용")

    override suspend fun updateMyMarketingConsents(request: UserUpdateMarketingConsentRequestDto): BaseResponse<UserMarketingConsentDto> =
        TODO("이 테스트 미사용")

    override suspend fun getConnectedAccounts(): BaseResponse<UserConnectedAccountDto> = TODO("이 테스트 미사용")

    override suspend fun linkConnectedAccount(
        provider: String,
        request: SocialAccountLinkRequestDto,
    ): BaseResponse<UserConnectedAccountDto> = TODO("이 테스트 미사용")

    override suspend fun unlinkConnectedAccount(provider: String): BaseResponse<UserConnectedAccountDto> = TODO("이 테스트 미사용")

    override suspend fun getReceiverDeliveryConditions(receiverId: Long): BaseResponse<ReceiverDeliveryConditionDto> = TODO("이 테스트 미사용")

    override suspend fun updateReceiverDeliveryConditions(
        receiverId: Long,
        request: ReceiverDeliveryConditionUpdateRequestDto,
    ): BaseResponse<ReceiverDeliveryConditionDto> = TODO("이 테스트 미사용")
}
