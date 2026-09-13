package com.afternote.core.data.repoimpl

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.model.ReceiverListState
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class UserRepositoryImplTest {
    private val errorReporter = RecordingErrorReporter()

    private fun repository(
        onGetReceivers: suspend () -> BaseResponse<List<ReceiverListDto>> = { TODO("이 테스트 미사용") },
        onCreateReceiver: suspend (UserCreateReceiverRequestDto) -> BaseResponse<UserCreateReceiverDto> = {
            TODO("이 테스트 미사용")
        },
        authRepository: FakeAuthRepository = receiverAuthRepository(loggedIn = true),
    ) = repositoryOf(
        userApiService =
            FakeUserApiService(
                onDeleteAccount = { error("이 테스트 미사용") },
                onGetReceivers = onGetReceivers,
                onCreateReceiver = onCreateReceiver,
            ),
        authRepository = authRepository,
        errorReporter = errorReporter,
    )

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
    fun `refreshReceiverList - 같은 구독을 서버의 최신 목록으로 갱신한다`() =
        runBlocking {
            var requestCount = 0
            val repository =
                repository(
                    onGetReceivers = {
                        requestCount++
                        dataResponse(listOf(receiverDto("조회 $requestCount")))
                    },
                )
            val emissions = Channel<List<Receiver>>(capacity = Channel.UNLIMITED)
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    repository.receiverListFlow.collect { emissions.send(it) }
                }

            try {
                assertEquals("조회 1", withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() }.single().name)

                val narrowContract: UserReceiverRepository = repository
                narrowContract.refreshReceiverList()

                assertEquals("조회 2", withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() }.single().name)
                assertEquals(2, requestCount)
            } finally {
                collector.cancelAndJoin()
            }
        }

    @Test
    fun `refreshReceiverList - 갱신 실패는 마지막 목록을 유지하고 다음 갱신은 복구한다`() =
        runBlocking {
            var requestCount = 0
            val repository =
                repository(
                    onGetReceivers = {
                        requestCount++
                        if (requestCount == 2) throw UnknownHostException("일시적인 조회 실패")
                        dataResponse(listOf(receiverDto("조회 $requestCount")))
                    },
                )
            val emissions = Channel<List<Receiver>>(capacity = Channel.UNLIMITED)
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    repository.receiverListFlow.collect { emissions.send(it) }
                }

            try {
                val first = withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() }
                repository.refreshReceiverList()

                assertEquals(first, withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() })
                assertEquals(2, requestCount)
                assertEquals(1, errorReporter.writtenFailures.size)

                repository.refreshReceiverList()

                assertEquals("조회 3", withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() }.single().name)
                assertEquals(3, requestCount)
            } finally {
                collector.cancelAndJoin()
            }
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

    @Test
    fun `receiverListStateFlow - 로딩 뒤 성공한 빈 목록을 명시한다`() =
        runBlocking {
            val response = CompletableDeferred<BaseResponse<List<ReceiverListDto>>>()
            val repository = repository(onGetReceivers = { response.await() })
            val emissions = Channel<ReceiverListState>(capacity = Channel.UNLIMITED)
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    repository.receiverListStateFlow.collect { emissions.send(it) }
                }

            try {
                assertEquals(ReceiverListState.Loading(null), emissions.nextState())
                response.complete(dataResponse(emptyList()))

                assertEquals(ReceiverListState.Success(emptyList()), emissions.nextState())
                assertTrue(errorReporter.writtenFailures.isEmpty())
            } finally {
                collector.cancelAndJoin()
            }
        }

    @Test
    fun `receiverListStateFlow - 첫 실패는 빈 목록 성공과 구별되고 같은 구독에서 재시도한다`() =
        runBlocking {
            var requestCount = 0
            val repository =
                repository(
                    onGetReceivers = {
                        requestCount++
                        if (requestCount == 1) throw UnknownHostException("첫 조회 실패")
                        dataResponse(listOf(receiverDto("재시도 성공")))
                    },
                )
            val emissions = Channel<ReceiverListState>(capacity = Channel.UNLIMITED)
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    repository.receiverListStateFlow.collect { emissions.send(it) }
                }

            try {
                assertEquals(ReceiverListState.Loading(null), emissions.nextState())
                assertEquals(ReceiverListState.Failure(null), emissions.nextState())
                assertEquals(1, errorReporter.writtenFailures.size)

                val contract: UserReceiverRepository = repository
                contract.refreshReceiverList()

                assertEquals(ReceiverListState.Loading(null), emissions.nextState())
                val recovered = emissions.nextState() as ReceiverListState.Success
                assertEquals(listOf("재시도 성공"), recovered.receivers.map { it.name })
                assertEquals(2, requestCount)
                assertEquals(1, errorReporter.writtenFailures.size)
            } finally {
                collector.cancelAndJoin()
            }
        }

    @Test
    fun `receiverListStateFlow - 갱신 로딩과 실패에 마지막 성공 목록을 보존한다`() =
        runBlocking {
            var requestCount = 0
            val repository =
                repository(
                    onGetReceivers = {
                        requestCount++
                        if (requestCount > 1) throw UnknownHostException("갱신 실패")
                        dataResponse(listOf(receiverDto("기존 수신자")))
                    },
                )
            val emissions = Channel<ReceiverListState>(capacity = Channel.UNLIMITED)
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    repository.receiverListStateFlow.collect { emissions.send(it) }
                }

            try {
                assertEquals(ReceiverListState.Loading(null), emissions.nextState())
                val loaded = emissions.nextState() as ReceiverListState.Success
                assertEquals(listOf("기존 수신자"), loaded.receivers.map { it.name })

                repository.refreshReceiverList()

                assertEquals(ReceiverListState.Loading(loaded.receivers), emissions.nextState())
                assertEquals(ReceiverListState.Failure(loaded.receivers), emissions.nextState())
                assertEquals(2, requestCount)
                assertEquals(1, errorReporter.writtenFailures.size)
            } finally {
                collector.cancelAndJoin()
            }
        }

    @Test
    fun `receiverListStateFlow - 401 뒤에는 다음 로딩과 일시 실패에도 폐기한 캐시를 내지 않는다`() =
        runBlocking {
            var requestCount = 0
            val repository =
                repository(
                    onGetReceivers = {
                        requestCount++
                        when (requestCount) {
                            1 -> dataResponse(listOf(receiverDto("권한이 끝난 계정")))
                            2 -> throw ApiException(401, 401, "인증 만료", "인증 만료")
                            else -> throw UnknownHostException("401 이후 네트워크 실패")
                        }
                    },
                )
            val emissions = Channel<ReceiverListState>(capacity = Channel.UNLIMITED)
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    repository.receiverListStateFlow.collect { emissions.send(it) }
                }

            try {
                assertEquals(ReceiverListState.Loading(null), emissions.nextState())
                val loaded = emissions.nextState() as ReceiverListState.Success
                assertEquals(listOf("권한이 끝난 계정"), loaded.receivers.map { it.name })

                repository.refreshReceiverList()

                assertEquals(ReceiverListState.Loading(loaded.receivers), emissions.nextState())
                assertEquals(ReceiverListState.Failure(null), emissions.nextState())

                repository.refreshReceiverList()

                assertEquals(ReceiverListState.Loading(null), emissions.nextState())
                assertEquals(ReceiverListState.Failure(null), emissions.nextState())
                assertEquals(3, requestCount)
                assertEquals(2, errorReporter.writtenFailures.size)
            } finally {
                collector.cancelAndJoin()
            }
        }

    @Test
    fun `receiverListStateFlow - 로그아웃과 새 로그인 사이에 이전 계정 캐시를 격리한다`() =
        runBlocking {
            var requestCount = 0
            val authRepository = receiverAuthRepository(loggedIn = true)
            val repository =
                repository(
                    authRepository = authRepository,
                    onGetReceivers = {
                        requestCount++
                        if (requestCount > 1) throw UnknownHostException("새 계정 조회 실패")
                        dataResponse(listOf(receiverDto("이전 계정 수신자")))
                    },
                )
            val emissions = Channel<ReceiverListState>(capacity = Channel.UNLIMITED)
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    repository.receiverListStateFlow.collect { emissions.send(it) }
                }

            try {
                assertEquals(ReceiverListState.Loading(null), emissions.nextState())
                val loaded = emissions.nextState() as ReceiverListState.Success
                assertEquals(listOf("이전 계정 수신자"), loaded.receivers.map { it.name })

                authRepository.loggedIn = false

                assertEquals(ReceiverListState.SignedOut, emissions.nextState())
                assertEquals(1, requestCount)

                authRepository.loggedIn = true

                assertEquals(ReceiverListState.Loading(null), emissions.nextState())
                assertEquals(ReceiverListState.Failure(null), emissions.nextState())
                assertEquals(2, requestCount)
            } finally {
                collector.cancelAndJoin()
            }
        }

    @Test
    fun `receiverListStateFlow - 수집 취소를 API에 전파하고 실패 상태와 리포트를 만들지 않는다`() =
        runBlocking {
            val requestStarted = CompletableDeferred<Unit>()
            val cancelledRequest = CompletableDeferred<CancellationException>()
            val repository =
                repository(
                    onGetReceivers = {
                        requestStarted.complete(Unit)
                        try {
                            awaitCancellation()
                        } catch (cancellation: CancellationException) {
                            cancelledRequest.complete(cancellation)
                            throw cancellation
                        }
                    },
                )
            val emissions = Channel<ReceiverListState>(capacity = Channel.UNLIMITED)
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    repository.receiverListStateFlow.collect { emissions.send(it) }
                }

            try {
                assertEquals(ReceiverListState.Loading(null), emissions.nextState())
                withTimeout(TEST_TIMEOUT_MILLIS) { requestStarted.await() }
                val cancellation = CancellationException("화면 수집 종료")

                collector.cancel(cancellation)
                withTimeout(TEST_TIMEOUT_MILLIS) { collector.join() }

                val propagated = withTimeout(TEST_TIMEOUT_MILLIS) { cancelledRequest.await() }
                assertEquals(cancellation.message, propagated.message)
                assertTrue(collector.isCancelled)
                assertTrue(emissions.tryReceive().isFailure)
                assertTrue(errorReporter.writtenFailures.isEmpty())
            } finally {
                collector.cancelAndJoin()
            }
        }

    private suspend fun Channel<ReceiverListState>.nextState(): ReceiverListState = withTimeout(TEST_TIMEOUT_MILLIS) { receive() }

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

/** 프로덕션 조립과 같은 모양 — 위임 대상은 Hilt 가 주입하므로 여기서는 테스트가 대신 만들어 넘긴다. */
private fun repositoryOf(
    userApiService: UserApiService,
    authRepository: FakeAuthRepository,
    errorReporter: ErrorReporter,
): UserRepositoryImpl =
    UserRepositoryImpl(
        receiverRepository = UserReceiverRepositoryImpl(userApiService, authRepository, errorReporter),
        myProfileRepository = MyProfileRepositoryImpl(userApiService),
    )
