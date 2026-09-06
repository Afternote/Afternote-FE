package com.afternote.core.data.repoimpl

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.core.domain.repository.UserRepository
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
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.net.UnknownHostException

class UserRepositoryImplTest {
    private val calls = mutableListOf<String>()
    private val errorReporter = RecordingErrorReporter()

    private fun repository(
        deleteAccountResponse: BaseResponse<Unit> = success(),
        clearSessionResult: Result<Unit> = Result.success(Unit),
        onGetReceivers: suspend () -> BaseResponse<List<ReceiverListDto>> = { TODO("이 테스트 미사용") },
        onCreateReceiver: suspend (UserCreateReceiverRequestDto) -> BaseResponse<UserCreateReceiverDto> = {
            TODO("이 테스트 미사용")
        },
        authRepository: FakeAuthRepository = receiverAuthRepository(loggedIn = true),
    ) = repositoryOf(
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
            authRepository.apply {
                onClearSession = {
                    calls += "clearSession"
                    clearSessionResult
                }
            },
        errorReporter = errorReporter,
    )

    @Test
    fun `deleteAccount - 탈퇴 성공 시 로컬 세션을 정리한다`() {
        val repository = repository()

        runBlocking { repository.deleteAccount() }

        assertEquals(listOf("deleteAccount", "clearSession"), calls)
        assertEquals(0, errorReporter.writtenFailures.size)
    }

    @Test
    fun `deleteAccount - 서버 탈퇴 실패면 세션을 유지한다`() {
        val repository = repository(deleteAccountResponse = BaseResponse(status = 500, code = 500))

        assertThrows(ApiException::class.java) {
            runBlocking { repository.deleteAccount() }
        }

        assertEquals(listOf("deleteAccount"), calls)
        assertEquals(0, errorReporter.writtenFailures.size)
    }

    /**
     * 서버 계정은 이미 지워진 뒤라 정리 실패를 예외로 올리면 화면이 "탈퇴 실패" 로 표시되고,
     * 사용자의 재시도는 없는 계정에 대해 다시 실패한다. 삼키는 것이 계약이다.
     */
    @Test
    fun `deleteAccount - 세션 정리가 실패해도 탈퇴는 성공으로 끝난다`() {
        val failure = IllegalStateException("datastore 쓰기 실패")
        val repository = repository(clearSessionResult = Result.failure(failure))

        runBlocking { repository.deleteAccount() }

        assertEquals(listOf("deleteAccount", "clearSession"), calls)
        val (reported, attributes) = errorReporter.writtenFailures.single()
        assertEquals(IllegalStateException::class.java.name, reported.message)
        assertEquals(
            mapOf(
                "account_stage" to "delete_session_cleanup",
                "error_type" to IllegalStateException::class.java.name,
            ),
            attributes,
        )
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
    fun `receiverListFlow - 조회가 실패해도 예외로 새지 않고 빈 목록을 낸다`() {
        val repository = repository(onGetReceivers = { throw UnknownHostException("Unable to resolve host") })

        val emitted = runBlocking { repository.receiverListFlow.first() }

        assertEquals(emptyList<Receiver>(), emitted)
    }

    /**
     * 이 flow 는 예외를 삼켜 화면을 살리므로, 삼킨 뒤의 리포터 기록이 이 실패 경로의 유일한 신호다.
     * logcat 은 실기에서 회수되지 않는다 — 기록이 빠지면 이 경로는 무음으로 재발한다.
     */
    @Test
    fun `receiverListFlow - 삼킨 조회 실패는 리포터에 단계와 함께 남는다`() {
        val repository = repository(onGetReceivers = { throw UnknownHostException("Unable to resolve host") })

        runBlocking { repository.receiverListFlow.first() }

        val (reported, attributes) = errorReporter.writtenFailures.single()
        assertEquals(UnknownHostException::class.java.name, reported.message)
        assertEquals(
            mapOf(
                "stage" to "receiver_list",
                "error_type" to UnknownHostException::class.java.name,
            ),
            attributes,
        )
    }

    @Test
    fun `receiverListFlow - 세션 만료도 흐름을 끊지 않는다`() {
        val repository =
            repository(
                onGetReceivers = {
                    throw ApiException(
                        status = 401,
                        code = 401,
                        serverMessage = "인증되지 않은 요청입니다.",
                        fallbackMessage = "인증되지 않은 요청입니다.",
                    )
                },
            )

        val emitted = runBlocking { repository.receiverListFlow.first() }

        assertEquals(emptyList<Receiver>(), emitted)
    }

    @Test
    fun `receiverListFlow - 로그아웃 중에는 서버를 호출하지 않는다`() {
        var requestCount = 0
        val authRepository = receiverAuthRepository(loggedIn = false)
        val repository =
            repository(
                authRepository = authRepository,
                onGetReceivers = {
                    requestCount += 1
                    dataResponse(listOf(receiverDto("호출되면 안 됨")))
                },
            )

        val directResult = runBlocking { repository.getReceivers() }
        val flowResult = runBlocking { repository.receiverListFlow.first() }

        assertEquals(emptyList<Receiver>(), directResult)
        assertEquals(emptyList<Receiver>(), flowResult)
        assertEquals(0, requestCount)
    }

    @Test
    fun `receiverListFlow - 로그아웃 뒤 새 세션의 첫 실패에는 이전 계정 목록을 내지 않는다`() =
        runBlocking {
            var requestCount = 0
            val authRepository = receiverAuthRepository(loggedIn = true)
            val repository =
                repository(
                    authRepository = authRepository,
                    onGetReceivers = {
                        requestCount += 1
                        if (requestCount == 1) {
                            dataResponse(listOf(receiverDto("이전 계정 수신인")))
                        } else {
                            throw UnknownHostException("새 세션 첫 조회 실패")
                        }
                    },
                )
            val emissions = Channel<List<Receiver>>(capacity = Channel.UNLIMITED)
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    repository.receiverListFlow.collect { emissions.send(it) }
                }

            try {
                assertEquals(
                    listOf("이전 계정 수신인"),
                    withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() }.map { it.name },
                )

                authRepository.loggedIn = false
                assertEquals(
                    emptyList<Receiver>(),
                    withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() },
                )
                assertEquals(1, requestCount)

                authRepository.loggedIn = true
                assertEquals(
                    emptyList<Receiver>(),
                    withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() },
                )
                assertEquals(2, requestCount)
            } finally {
                collector.cancelAndJoin()
            }
        }

    @Test
    fun `receiverListFlow - 한 번 실패해도 다음 구독은 목록을 다시 조회한다`() {
        var requestCount = 0
        val repository =
            repository(
                onGetReceivers = {
                    requestCount += 1
                    if (requestCount == 1) throw UnknownHostException("Unable to resolve host")
                    dataResponse(listOf(receiverDto("복구 후 목록")))
                },
            )

        val failed = runBlocking { repository.receiverListFlow.first() }
        val recovered = runBlocking { repository.receiverListFlow.first() }

        assertEquals(emptyList<Receiver>(), failed)
        assertEquals("복구 후 목록", recovered.single().name)
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
            val emissions = Channel<List<Receiver>>(capacity = Channel.UNLIMITED)
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    repository.receiverListFlow.collect { emissions.send(it) }
                }

            try {
                assertEquals("조회 1", withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() }.single().name)
                repository.createReceiver(
                    name = "새 수신자",
                    relation = "친구",
                    phone = null,
                    email = "receiver@example.com",
                    message = null,
                )
                assertEquals("조회 2", withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() }.single().name)
                assertEquals(2, requestCount)
            } finally {
                collector.cancelAndJoin()
            }
        }

    /**
     * 수신자 구현이 [UserRepositoryImpl] 밖으로 나갔어도 좁은 계약과 합본 계약은 **같은 인스턴스**를
     * 봐야 한다 (#1282). 여기서 갈리면 `UserReceiverRepository` 로 등록한 수신자가
     * `UserRepository` 구독자의 목록을 갱신하지 못하고 화면이 방금 만든 수신인을 놓친다.
     */
    @Test
    fun `좁은 계약과 합본 계약은 같은 수신자 갱신 상태를 본다`() =
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
            val narrowContract: UserReceiverRepository = repository
            val mergedContract: UserRepository = repository
            val emissions = Channel<List<Receiver>>(capacity = Channel.UNLIMITED)
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    mergedContract.receiverListFlow.collect { emissions.send(it) }
                }

            try {
                assertEquals("조회 1", withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() }.single().name)
                narrowContract.createReceiver(
                    name = "새 수신자",
                    relation = "친구",
                    phone = null,
                    email = "receiver@example.com",
                    message = null,
                )
                assertEquals("조회 2", withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() }.single().name)
            } finally {
                collector.cancelAndJoin()
            }
        }

    /**
     * 크래시를 막자고 «보고 있던 목록» 을 지우면 안 된다. 첫 조회 실패는 아직 아무것도 못 본 상태라
     * 빈 목록이 맞지만(위 테스트들), 두 번째부터의 실패는 화면에 떠 있던 수신인을 0명으로 바꾼다.
     */
    @Test
    fun `receiverListFlow - 갱신이 실패해도 마지막으로 성공한 목록을 유지한다`() =
        runBlocking {
            var requestCount = 0
            val repository =
                repository(
                    onGetReceivers = {
                        requestCount += 1
                        if (requestCount == 1) {
                            dataResponse(listOf(receiverDto("계정 A")))
                        } else {
                            throw UnknownHostException("Unable to resolve host")
                        }
                    },
                    onCreateReceiver = { dataResponse(UserCreateReceiverDto(receiverId = 2L, authCode = "AUTH-2")) },
                )
            val emissions = Channel<List<Receiver>>(capacity = Channel.UNLIMITED)
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    repository.receiverListFlow.collect { emissions.send(it) }
                }

            try {
                val first = withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() }
                repository.createReceiver(
                    name = "새 수신자",
                    relation = "친구",
                    phone = null,
                    email = "receiver@example.com",
                    message = null,
                )
                val fallback = withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() }

                assertEquals(listOf("계정 A"), first.map { it.name })
                assertEquals(listOf("계정 A"), fallback.map { it.name })
                assertEquals(2, requestCount)
            } finally {
                collector.cancelAndJoin()
            }
        }

    @Test
    fun `receiverListFlow - 같은 세션이어도 401이면 마지막 목록을 폐기한다`() =
        runBlocking {
            var requestCount = 0
            val repository =
                repository(
                    onGetReceivers = {
                        requestCount += 1
                        if (requestCount == 1) {
                            dataResponse(listOf(receiverDto("인증이 끝난 계정의 수신인")))
                        } else {
                            throw ApiException(
                                status = 401,
                                code = 401,
                                serverMessage = "인증되지 않은 요청입니다.",
                                fallbackMessage = "인증되지 않은 요청입니다.",
                            )
                        }
                    },
                    onCreateReceiver = { dataResponse(UserCreateReceiverDto(receiverId = 2L, authCode = "AUTH-2")) },
                )
            val emissions = Channel<List<Receiver>>(capacity = Channel.UNLIMITED)
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    repository.receiverListFlow.collect { emissions.send(it) }
                }

            try {
                val first = withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() }
                repository.createReceiver(
                    name = "새 수신자",
                    relation = "친구",
                    phone = null,
                    email = "receiver@example.com",
                    message = null,
                )
                val afterUnauthorized = withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() }

                assertEquals(listOf("인증이 끝난 계정의 수신인"), first.map { it.name })
                assertEquals(emptyList<Receiver>(), afterUnauthorized)
                assertEquals(2, requestCount)
            } finally {
                collector.cancelAndJoin()
            }
        }

    private fun receiverAuthRepository(loggedIn: Boolean): FakeAuthRepository =
        FakeAuthRepository.strict(loggedIn = loggedIn).apply {
            onIsLoggedIn = { loggedInState }
        }

    private companion object {
        const val TEST_TIMEOUT_MILLIS = 2_000L
    }
}

private class RecordingErrorReporter : ErrorReporter {
    val writtenFailures = mutableListOf<Pair<Throwable, Map<String, String>>>()

    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) {
        writtenFailures += throwable to attributes
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

    override suspend fun registerPushToken(request: RegisterPushTokenRequestDto): BaseResponse<PushTokenDto> = TODO("이 테스트 미사용")

    override suspend fun deletePushToken(request: DeletePushTokenRequestDto): BaseResponse<Unit> = TODO("이 테스트 미사용")

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

/** 프로덕션 조립과 같은 모양 — 위임 대상은 Hilt 가 주입하므로 여기서는 테스트가 대신 만들어 넘긴다. */
private fun repositoryOf(
    userApiService: UserApiService,
    authRepository: FakeAuthRepository,
    errorReporter: ErrorReporter,
): UserRepositoryImpl =
    UserRepositoryImpl(
        userApiService = userApiService,
        authRepository = authRepository,
        errorReporter = errorReporter,
        receiverRepository = UserReceiverRepositoryImpl(userApiService, authRepository, errorReporter),
        myProfileRepository = MyProfileRepositoryImpl(userApiService),
    )
