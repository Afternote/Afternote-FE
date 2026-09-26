package com.afternote.core.data.repoimpl

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.model.ReceiverListState
import com.afternote.core.domain.repository.UserReceiverRepository
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.UnknownHostException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * 수신자 목록 조회 결과 계약 (#2045).
 *
 * [UserReceiverRepository.receiverListStateFlow] 가 로딩·성공·실패·세션 종료를 가르는지, 그리고 같은 원천에서
 * 파생한 [UserReceiverRepository.receiverListFlow] 가 #2135 의 방출 순서를 그대로 지키는지 공개 계약으로만 본다.
 * 세션 경계는 실제 파일 DataStore 위의 [TestTokenSessionStore] 로 세운다. 기존 목록 전용 테스트
 * (`UserRepositoryImplTest`·[ReceiverSessionBoundaryRaceTest]·[ReceiverUpdateRefreshTest])는 수정 없이 통과해야 한다.
 */
class ReceiverListStateFlowTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val sessionStore by lazy { TestTokenSessionStore(temporaryFolder.root) }
    private val fetches = ScriptedFetches()
    private val reporter = RecordingErrorReporter()

    @Before
    fun openSession() = runBlocking { sessionStore.login(accessToken = "access-a", refreshToken = "refresh-a") }

    @After
    fun closeSessionStore() = sessionStore.close()

    @Test
    fun `receiverListStateFlow - 첫 조회는 로딩 뒤 결과를 내고 성공한 0건은 빈 Success 다`() =
        runBlocking {
            val repository = repository()
            val states = collectStates(repository)

            try {
                assertEquals(ReceiverListState.Loading(null), states.next())
                fetches.awaitStarted()
                fetches.respond()
                assertEquals(ReceiverListState.Success(emptyList()), states.next())
            } finally {
                states.stop()
            }
        }

    @Test
    fun `receiverListStateFlow - 첫 실패는 빈 성공과 구별되고 같은 구독이 refreshReceiverList 로 다시 조회한다`() =
        runBlocking {
            val repository = repository()
            val states = collectStates(repository)

            try {
                assertEquals(ReceiverListState.Loading(null), states.next())
                fetches.awaitStarted()
                fetches.fail(UnknownHostException("오프라인"))
                assertEquals(ReceiverListState.Failure(null), states.next())

                repository.refreshReceiverList()

                assertEquals(ReceiverListState.Loading(null), states.next())
                fetches.awaitStarted()
                fetches.respond(RECEIVER_A)
                assertEquals(ReceiverListState.Success(listOf(receiver(RECEIVER_A))), states.next())
                assertEquals(2, fetches.startedCount)
                assertEquals(1, reporter.failures.size)
            } finally {
                states.stop()
            }
        }

    @Test
    fun `receiverListStateFlow - 갱신 로딩과 갱신 실패는 마지막 성공 목록을 싣는다`() =
        runBlocking {
            val repository = repository()
            val states = collectStates(repository)

            try {
                states.succeedFirst(RECEIVER_A)

                repository.refreshReceiverList()

                val previous = listOf(receiver(RECEIVER_A))
                assertEquals(ReceiverListState.Loading(previous), states.next())
                fetches.awaitStarted()
                fetches.fail(ApiException(status = 500, code = 500, serverMessage = null, fallbackMessage = "서버 오류"))
                assertEquals(ReceiverListState.Failure(previous), states.next())
            } finally {
                states.stop()
            }
        }

    @Test
    fun `receiverListStateFlow - 401 은 마지막 성공 목록을 버리고 다음 실패에도 되살리지 않는다`() =
        runBlocking {
            val repository = repository()
            val states = collectStates(repository)

            try {
                states.succeedFirst(RECEIVER_A)

                repository.refreshReceiverList()
                assertEquals(ReceiverListState.Loading(listOf(receiver(RECEIVER_A))), states.next())
                fetches.awaitStarted()
                fetches.fail(ApiException(status = 401, code = 401, serverMessage = null, fallbackMessage = "인증 만료"))
                assertEquals(ReceiverListState.Failure(null), states.next())

                repository.refreshReceiverList()
                assertEquals(ReceiverListState.Loading(null), states.next())
                fetches.awaitStarted()
                fetches.fail(UnknownHostException("오프라인"))
                assertEquals(ReceiverListState.Failure(null), states.next())
            } finally {
                states.stop()
            }
        }

    /**
     * 앱에서 401 은 `TokenAuthenticator` 가 재발급 거절을 확정하고 세션을 비운 뒤에야 조회 예외로 올라온다
     * (`TokenReissuer` 가 락 안에서 정리, #1126). 그래서 이 경로에서는 Failure(null) 가 나오지 않고, 소비자는
     * SignedOut 으로 이전 계정 행을 비운다.
     */
    @Test
    fun `receiverListStateFlow - 세션을 비운 뒤 올라온 401 은 Failure 없이 SignedOut 만 낸다`() =
        runBlocking {
            val repository = repository()
            val states = collectStates(repository)

            try {
                states.succeedFirst(RECEIVER_A)
                sessionStore.holdObservation()

                repository.refreshReceiverList()
                assertEquals(ReceiverListState.Loading(listOf(receiver(RECEIVER_A))), states.next())
                fetches.awaitStarted()
                sessionStore.clearSession()
                fetches.fail(ApiException(status = 401, code = 401, serverMessage = null, fallbackMessage = "인증 만료"))
                assertNull("세션을 비운 뒤의 401 이 Failure 로 나왔다", states.nextOrNull())

                sessionStore.releaseObservation()
                assertEquals(ReceiverListState.SignedOut, states.next())
                assertNull("SignedOut 뒤에 상태가 더 나왔다", states.nextOrNull())
            } finally {
                states.stop()
            }
        }

    @Test
    fun `receiverListStateFlow - 로그인 세션이 없으면 서버를 부르지 않고 SignedOut 을 낸다`() =
        runBlocking {
            sessionStore.clearSession()
            val repository = repository()

            assertEquals(ReceiverListState.SignedOut, repository.receiverListStateFlow.first())
            assertEquals(0, fetches.startedCount)
        }

    /**
     * 수집자가 중간 로그아웃 방출을 못 본 채 새 세션만 받는 구간 (#2135). 상태 Flow 는 새 세션의 로딩보다
     * 먼저 SignedOut 을 내야 한다. Loading(null) 부터 내면 소비자는 이전 계정 행을 계속 들고 있어도 된다고 읽는다.
     */
    @Test
    fun `receiverListStateFlow - 로그아웃 방출 없이 세션이 바뀌어도 새 세션 로딩 전에 SignedOut 을 먼저 낸다`() =
        runBlocking {
            val repository = repository()
            val states = collectStates(repository)

            try {
                states.succeedFirst(RECEIVER_A)

                sessionStore.holdObservation()
                sessionStore.clearSession()
                sessionStore.login(accessToken = "access-b", refreshToken = "refresh-b")
                sessionStore.releaseObservation()

                assertEquals(ReceiverListState.SignedOut, states.next())
                assertEquals(ReceiverListState.Loading(null), states.next())
                fetches.awaitStarted()
                fetches.respond(RECEIVER_B)
                assertEquals(ReceiverListState.Success(listOf(receiver(RECEIVER_B))), states.next())
            } finally {
                states.stop()
            }
        }

    /**
     * 세션 전환이 저장소에는 커밋됐지만 상위 관측이 아직 새 식별자를 전달하지 못한 구간에 끝난 조회는
     * 목록 Flow 와 마찬가지로 상태 Flow 에도 결과를 내지 않는다 (#2135 의 늦은 결과 가드).
     */
    @Test
    fun `receiverListStateFlow - 이전 세션의 늦은 조회 결과는 성공도 실패도 내지 않는다`() =
        runBlocking {
            val repository = repository()
            val states = collectStates(repository)

            try {
                states.succeedFirst(RECEIVER_A)
                sessionStore.holdObservation()

                repository.refreshReceiverList()
                assertEquals(ReceiverListState.Loading(listOf(receiver(RECEIVER_A))), states.next())
                fetches.awaitStarted()
                sessionStore.clearSession()
                sessionStore.login(accessToken = "access-b", refreshToken = "refresh-b")
                fetches.fail(UnknownHostException("이전 세션 조회 실패"))
                assertNull("이전 세션 조회의 실패가 상태로 나왔다", states.nextOrNull())

                repository.refreshReceiverList()
                assertEquals(ReceiverListState.Loading(listOf(receiver(RECEIVER_A))), states.next())
                fetches.awaitStarted()
                fetches.respond(RECEIVER_A)
                assertNull("이전 세션 조회의 늦은 성공이 상태로 나왔다", states.nextOrNull())

                sessionStore.releaseObservation()
                assertEquals(ReceiverListState.SignedOut, states.next())
                assertEquals(ReceiverListState.Loading(null), states.next())
                fetches.awaitStarted()
                fetches.respond(RECEIVER_B)
                assertEquals(ReceiverListState.Success(listOf(receiver(RECEIVER_B))), states.next())
            } finally {
                states.stop()
            }
        }

    @Test
    fun `receiverListStateFlow - 조회 중 구독을 취소하면 Failure 도 실패 리포트도 남기지 않는다`() =
        runBlocking {
            val repository = repository()
            val states = collectStates(repository)

            assertEquals(ReceiverListState.Loading(null), states.next())
            fetches.awaitStarted()
            states.stop()

            assertNull(states.nextOrNull())
            assertEquals(emptyList<Throwable>(), reporter.failures.toList())
        }

    /**
     * 목록 전용 소비자(타임레터 수신인 목록 등)의 방출 순서 보존 조건. 상태 Flow 가 재조회마다 Loading 을 더 내도
     * 목록 Flow 에는 새 방출이 생기지 않아야 한다. 생기면 기존 소비자가 조회 중에 빈 목록이나 같은 목록을 한 번 더 받는다.
     */
    @Test
    fun `receiverListFlow - 상태 Flow 에서 파생돼도 재조회 로딩은 방출을 만들지 않는다`() =
        runBlocking {
            val repository = repository()
            val emissions = Channel<List<Receiver>>(capacity = Channel.UNLIMITED)
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) { repository.receiverListFlow.collect { emissions.send(it) } }

            try {
                fetches.awaitStarted()
                assertNull("조회 결과 전에 목록이 나왔다", emissions.tryReceive().getOrNull())
                fetches.respond(RECEIVER_A)
                assertEquals(listOf(receiver(RECEIVER_A)), withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() })

                repository.refreshReceiverList()
                fetches.awaitStarted()
                assertNull("재조회 로딩이 목록 방출을 만들었다", withTimeoutOrNull(QUIET_WAIT_MILLIS) { emissions.receive() })

                fetches.fail(UnknownHostException("오프라인"))
                assertEquals(listOf(receiver(RECEIVER_A)), withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() })
            } finally {
                collector.cancelAndJoin()
            }
        }

    /** 두 공개 Flow 가 같은 revision 을 본다. 재시도가 목록 전용 구독자도 다시 조회하게 하는 것은 의도된 공유다. */
    @Test
    fun `refreshReceiverList - 목록 전용 구독자도 함께 다시 조회한다`() =
        runBlocking {
            val repository = repository()
            val emissions = Channel<List<Receiver>>(capacity = Channel.UNLIMITED)
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) { repository.receiverListFlow.collect { emissions.send(it) } }

            try {
                fetches.awaitStarted()
                fetches.respond(RECEIVER_A)
                assertEquals(listOf(receiver(RECEIVER_A)), withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() })

                repository.refreshReceiverList()

                fetches.awaitStarted()
                fetches.respond(RECEIVER_B)
                assertEquals(listOf(receiver(RECEIVER_B)), withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() })
            } finally {
                collector.cancelAndJoin()
            }
        }

    private fun repository(): UserReceiverRepositoryImpl =
        UserReceiverRepositoryImpl(
            userApiService = ScriptedUserApiService(fetches),
            tokenDataSource = sessionStore.tokenDataSource,
            errorReporter = reporter,
        )

    private fun CoroutineScope.collectStates(repository: UserReceiverRepository): StateRecorder {
        val channel = Channel<ReceiverListState>(capacity = Channel.UNLIMITED)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { repository.receiverListStateFlow.collect { channel.send(it) } }
        return StateRecorder(channel, job)
    }

    private inner class StateRecorder(
        private val channel: Channel<ReceiverListState>,
        private val job: Job,
    ) {
        suspend fun next(): ReceiverListState = withTimeout(TEST_TIMEOUT_MILLIS) { channel.receive() }

        suspend fun nextOrNull(): ReceiverListState? = withTimeoutOrNull(QUIET_WAIT_MILLIS) { channel.receive() }

        /** 첫 로딩과 첫 조회 성공까지 지나 보낸다. */
        suspend fun succeedFirst(name: String) {
            assertEquals(ReceiverListState.Loading(null), next())
            fetches.awaitStarted()
            fetches.respond(name)
            assertEquals(ReceiverListState.Success(listOf(receiver(name))), next())
        }

        suspend fun stop() = job.cancelAndJoin()
    }

    /** `getReceivers` 를 테스트가 한 건씩 여는 조회. 시작 시점과 응답 시점을 따로 쥐어야 로딩 구간을 단언할 수 있다. */
    private class ScriptedFetches {
        private val started = Channel<Unit>(capacity = Channel.UNLIMITED)
        private val responses = Channel<Result<List<String>>>(capacity = Channel.UNLIMITED)
        private val startedCounter = AtomicInteger()

        val startedCount: Int get() = startedCounter.get()

        suspend fun fetch(): BaseResponse<List<ReceiverListDto>> {
            startedCounter.incrementAndGet()
            started.send(Unit)
            val names = responses.receive().getOrThrow()
            return BaseResponse(
                status = 200,
                code = 200,
                data = names.map { ReceiverListDto(receiverId = receiverIdOf(it), name = it, relation = RELATION) },
            )
        }

        suspend fun awaitStarted() {
            withTimeout(TEST_TIMEOUT_MILLIS) { started.receive() }
        }

        suspend fun respond(vararg names: String) = responses.send(Result.success(names.toList()))

        suspend fun fail(error: Throwable) = responses.send(Result.failure(error))
    }

    private class RecordingErrorReporter : ErrorReporter {
        val failures = CopyOnWriteArrayList<Throwable>()

        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) {
            failures += throwable
        }
    }

    private class ScriptedUserApiService(
        private val fetches: ScriptedFetches,
    ) : UserApiService {
        override suspend fun getReceivers(): BaseResponse<List<ReceiverListDto>> = fetches.fetch()

        override suspend fun createReceiver(request: UserCreateReceiverRequestDto): BaseResponse<UserCreateReceiverDto> = TODO("이 테스트 미사용")

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

        override suspend fun deleteAccount(): BaseResponse<Unit> = TODO("이 테스트 미사용")

        override suspend fun registerPushToken(request: RegisterPushTokenRequestDto): BaseResponse<PushTokenDto> = TODO("이 테스트 미사용")

        override suspend fun deletePushToken(request: DeletePushTokenRequestDto): BaseResponse<Unit> = TODO("이 테스트 미사용")

        override suspend fun getMyPushSettings(): BaseResponse<UserPushSettingDto> = TODO("이 테스트 미사용")

        override suspend fun updateMyPushSettings(request: UserUpdatePushSettingRequestDto): BaseResponse<UserPushSettingDto> =
            TODO("이 테스트 미사용")

        override suspend fun getMyMarketingConsents(): BaseResponse<UserMarketingConsentDto> = TODO("이 테스트 미사용")

        override suspend fun updateMyMarketingConsents(
            request: UserUpdateMarketingConsentRequestDto,
        ): BaseResponse<UserMarketingConsentDto> = TODO("이 테스트 미사용")

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

    private companion object {
        const val RECEIVER_A = "계정 A 수신인"
        const val RECEIVER_B = "계정 B 수신인"
        const val RELATION = "친구"
        const val TEST_TIMEOUT_MILLIS = 5_000L
        const val QUIET_WAIT_MILLIS = 500L

        fun receiverIdOf(name: String): Long = if (name == RECEIVER_A) 1L else 2L

        fun receiver(name: String) = Receiver(receiverId = receiverIdOf(name), name = name, relation = RELATION)
    }
}
