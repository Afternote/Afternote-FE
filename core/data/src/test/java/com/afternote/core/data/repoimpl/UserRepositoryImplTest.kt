package com.afternote.core.data.repoimpl

import com.afternote.core.domain.error.ReceiverRegistrationRejected
import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.model.user.Receiver
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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class UserRepositoryImplTest {
    private val calls = mutableListOf<String>()

    private fun repository(
        deleteAccountResponse: BaseResponse<Unit> = success(),
        clearSessionResult: Result<Unit> = Result.success(Unit),
        onGetReceivers: suspend () -> BaseResponse<List<ReceiverListDto>> = { TODO("이 테스트 미사용") },
        onCreateReceiver: suspend (UserCreateReceiverRequestDto) -> BaseResponse<UserCreateReceiverDto> = {
            TODO("이 테스트 미사용")
        },
    ) = UserRepositoryImpl(
        userApiService =
            FakeUserApiService(
                onDeleteAccount = {
                    calls += "deleteAccount"
                    deleteAccountResponse
                },
                onGetReceivers = onGetReceivers,
                onCreateReceiver = onCreateReceiver,
            ),
        authRepository =
            FakeAuthRepository.strict(loggedIn = true).apply {
                onClearSession = {
                    calls += "clearSession"
                    clearSessionResult
                }
            },
    )

    @Test
    fun `deleteAccount - 탈퇴 성공 시 로컬 세션을 정리한다`() {
        val repository = repository()

        runBlocking { repository.deleteAccount() }

        assertEquals(listOf("deleteAccount", "clearSession"), calls)
    }

    @Test
    fun `deleteAccount - 서버 탈퇴 실패면 세션을 유지한다`() {
        val repository = repository(deleteAccountResponse = BaseResponse(status = 500, code = 500))

        assertThrows(ApiException::class.java) {
            runBlocking { repository.deleteAccount() }
        }

        assertEquals(listOf("deleteAccount"), calls)
    }

    /**
     * 서버 계정은 이미 지워진 뒤라 정리 실패를 예외로 올리면 화면이 "탈퇴 실패" 로 표시되고,
     * 사용자의 재시도는 없는 계정에 대해 다시 실패한다. 삼키는 것이 계약이다.
     */
    @Test
    fun `deleteAccount - 세션 정리가 실패해도 탈퇴는 성공으로 끝난다`() {
        val repository = repository(clearSessionResult = Result.failure(IllegalStateException("datastore 쓰기 실패")))

        runBlocking { repository.deleteAccount() }

        assertEquals(listOf("deleteAccount", "clearSession"), calls)
    }

    /** 정리가 DELETE 앞에 오면 요청이 토큰 없이 나가므로, 순서 자체가 계약이다. */
    @Test
    fun `deleteAccount - 세션 정리는 서버 호출 뒤에 온다`() {
        val repository = repository()

        runBlocking { repository.deleteAccount() }

        assertEquals(0, calls.indexOf("deleteAccount"))
        assertEquals(1, calls.indexOf("clearSession"))
    }

    @Test
    fun `getReceivers - 다음 호출은 서버의 최신 계정 목록을 다시 조회한다`() {
        var requestCount = 0
        val repository =
            repository(
                onGetReceivers = {
                    requestCount += 1
                    dataResponse(if (requestCount == 1) listOf(receiverDto("계정 A")) else emptyList())
                },
            )

        val first = runBlocking { repository.getReceivers() }
        val second = runBlocking { repository.getReceivers() }

        assertEquals(listOf("계정 A"), first.map { it.name })
        assertEquals(emptyList<Receiver>(), second)
        assertEquals(2, requestCount)
    }

    @Test
    fun `receiverListFlow - 새 구독은 서버에서 목록을 다시 조회한다`() {
        var requestCount = 0
        val repository =
            repository(
                onGetReceivers = {
                    requestCount += 1
                    dataResponse(listOf(receiverDto("조회 $requestCount")))
                },
            )

        val first = runBlocking { repository.receiverListFlow.first() }
        val second = runBlocking { repository.receiverListFlow.first() }

        assertEquals("조회 1", first.single().name)
        assertEquals("조회 2", second.single().name)
        assertEquals(2, requestCount)
    }

    @Test
    fun `createReceiver - 성공하면 구독 중인 목록을 다시 조회한다`() =
        runBlocking {
            var requestCount = 0
            val repository =
                repository(
                    onGetReceivers = {
                        requestCount += 1
                        dataResponse(listOf(receiverDto("조회 $requestCount")))
                    },
                    onCreateReceiver = { dataResponse(UserCreateReceiverDto(receiverId = 2L, authCode = "AUTH-2")) },
                )
            val emissions = mutableListOf<List<Receiver>>()
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    repository.receiverListFlow.take(2).toList(emissions)
                }

            repository.createReceiver(
                name = "새 수신자",
                relation = "친구",
                phone = null,
                email = "receiver@afternote.com",
                message = null,
            )
            collector.join()

            assertEquals(listOf("조회 1", "조회 2"), emissions.map { it.single().name })
            assertEquals(2, requestCount)
        }

    @Test
    fun `createReceiver - 4xx 서버 문구를 도메인 오류로 전달한다`() {
        val repository =
            repository(
                onCreateReceiver = {
                    throw ApiException(
                        status = 400,
                        code = 400,
                        serverMessage = "올바른 이메일 형식이 아닙니다.",
                        message = "올바른 이메일 형식이 아닙니다.",
                    )
                },
            )

        val exception =
            assertThrows(ReceiverRegistrationRejected::class.java) {
                runBlocking {
                    repository.createReceiver(
                        name = "새 수신자",
                        relation = "친구",
                        phone = null,
                        email = "invalid",
                        message = null,
                    )
                }
            }

        assertEquals("올바른 이메일 형식이 아닙니다.", exception.serverMessage)
    }

    @Test
    fun `createReceiver - 5xx 서버 문구는 사용자 오류로 변환하지 않는다`() {
        val serverFailure =
            ApiException(
                status = 500,
                code = 500,
                serverMessage = "internal database failure",
                message = "internal database failure",
            )
        val repository = repository(onCreateReceiver = { throw serverFailure })

        val exception =
            assertThrows(ApiException::class.java) {
                runBlocking {
                    repository.createReceiver(
                        name = "새 수신자",
                        relation = "친구",
                        phone = null,
                        email = "receiver@afternote.com",
                        message = null,
                    )
                }
            }

        assertEquals(serverFailure, exception)
    }
}

private fun success() = BaseResponse<Unit>(status = 200, code = 200)

private fun <T> dataResponse(data: T) = BaseResponse(status = 200, code = 200, data = data)

private fun receiverDto(name: String) =
    ReceiverListDto(
        receiverId = 1L,
        name = name,
        relation = "친구",
        authCode = "AUTH-1",
    )

private class FakeUserApiService(
    private val onDeleteAccount: suspend () -> BaseResponse<Unit>,
    private val onGetReceivers: suspend () -> BaseResponse<List<ReceiverListDto>>,
    private val onCreateReceiver: suspend (UserCreateReceiverRequestDto) -> BaseResponse<UserCreateReceiverDto>,
) : UserApiService {
    override suspend fun deleteAccount(): BaseResponse<Unit> = onDeleteAccount()

    override suspend fun getReceivers(): BaseResponse<List<ReceiverListDto>> = onGetReceivers()

    override suspend fun createReceiver(request: UserCreateReceiverRequestDto): BaseResponse<UserCreateReceiverDto> =
        onCreateReceiver(request)

    override suspend fun getReceiverDetail(receiverId: Long): BaseResponse<ReceiverDetailDto> = TODO("이 테스트 미사용")

    override suspend fun updateReceiver(
        receiverId: Long,
        request: UserPatchReceiverRequestDto,
    ): BaseResponse<UserPatchReceiverDto> = TODO("이 테스트 미사용")

    override suspend fun updateReceiverMessage(
        receiverId: Long,
        request: UserUpdateReceiverMessageRequestDto,
    ): BaseResponse<Unit> = TODO("이 테스트 미사용")

    override suspend fun getMyProfile(): BaseResponse<UserDto> = TODO("이 테스트 미사용")

    override suspend fun updateMyProfile(request: UserUpdateProfileRequestDto): BaseResponse<UserDto> = TODO("이 테스트 미사용")

    override suspend fun logActivity(): BaseResponse<Unit> = TODO("이 테스트 미사용")

    override suspend fun getMyPushSettings(): BaseResponse<UserPushSettingDto> = TODO("이 테스트 미사용")

    override suspend fun updateMyPushSettings(request: UserUpdatePushSettingRequestDto): BaseResponse<UserPushSettingDto> =
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
