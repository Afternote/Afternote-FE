package com.afternote.core.data.repoimpl

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.data.repoimpl.auth.AuthRepositoryImpl
import com.afternote.core.datastore.LocalStoreRegistry
import com.afternote.core.datastore.StoreScope
import com.afternote.core.domain.push.DevicePushTargetProvider
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.domain.repository.push.PushTargetRepository
import com.afternote.core.model.user.Receiver
import com.afternote.core.network.dto.DeletePushTokenRequestDto
import com.afternote.core.network.dto.LoginDto
import com.afternote.core.network.dto.LoginRequestDto
import com.afternote.core.network.dto.LogoutRequestDto
import com.afternote.core.network.dto.PushTokenDto
import com.afternote.core.network.dto.ReceiverDetailDto
import com.afternote.core.network.dto.ReceiverListDto
import com.afternote.core.network.dto.RegisterPushTokenRequestDto
import com.afternote.core.network.dto.ReissueDto
import com.afternote.core.network.dto.ReissueRequestDto
import com.afternote.core.network.dto.SocialAccountLinkRequestDto
import com.afternote.core.network.dto.SocialLoginRequestDto
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
import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.service.AuthApiService
import com.afternote.core.network.service.TokenApiService
import com.afternote.core.network.service.UserApiService
import com.afternote.core.network.token.AccessTokenExpiryTracker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.UnknownHostException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * 수신자 목록 캐시의 세션 경계 계약 (#2135, #2045 재현 조사에서 분리).
 *
 * 기존 세션 경계 테스트(`UserRepositoryImplTest`)도 실제 토큰 저장소를 쓰지만, 여기서는 세션을 여닫는
 * 쪽도 프로덕션 조립 그대로다. [AuthRepositoryImpl] 실물 + [UserReceiverRepositoryImpl] 실물 +
 * [TestTokenSessionStore] 의 실제 파일 Preferences DataStore. 공개 계약([AuthRepository.saveSession] ·
 * [AuthRepository.logout] · [AuthRepository.rotateToken])만 호출하고 비공개 상태는 건드리지 않는다.
 *
 * 수집자가 세션 전환 구간을 제때 못 돌리는 상황은 두 가지로 모델링한다. [blockCollectorThread] 는 수집자
 * 디스패처를 잠그고, [TestTokenSessionStore.holdObservation] 은 관측 스트림만 멈춘다. 로그인 여부 Boolean 으로 캐시 수명을 정하면 이 구간의 로그아웃이
 * `true → true` 로 뭉개져 이전 계정의 목록이 새 계정에 남았다.
 */
class ReceiverSessionBoundaryRaceTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val sessionStore by lazy { TestTokenSessionStore(temporaryFolder.root) }

    // 데몬 스레드. 빗장이 끝내 안 풀리는 경우에도 이 스레드가 Gradle 워커를 붙잡고 있지 않게 한다.
    private val collectorExecutor =
        Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "receiver-collector").apply { isDaemon = true } }
    private val collectorDispatcher = collectorExecutor.asCoroutineDispatcher()
    private val collectorGate = CountDownLatch(1)

    @After
    fun tearDown() {
        // 단언이 빗장 구간 안에서 깨졌을 때를 위한 안전망. 잠긴 채로 두면 수집자 취소가 매달린다.
        collectorGate.countDown()
        collectorDispatcher.close()
        sessionStore.close()
    }

    @Test
    fun `로그아웃과 새 계정 로그인이 한 구간에 겹쳐도 이전 계정 목록은 새 계정에 남지 않는다`() =
        runBlocking {
            var requestCount = 0
            val userApiService =
                StubUserApiService(
                    onGetReceivers = {
                        requestCount += 1
                        if (requestCount == 1) {
                            receiverListResponse(ACCOUNT_A_RECEIVER)
                        } else {
                            throw UnknownHostException("계정 B 조회 실패")
                        }
                    },
                )
            val authRepository = authRepository()
            val repository = UserReceiverRepositoryImpl(userApiService, sessionStore.tokenDataSource, NoopErrorReporter)

            authRepository.saveSession(accessToken = "access-account-a", refreshToken = "refresh-account-a").getOrThrow()

            val emissions = Channel<List<Receiver>>(capacity = Channel.UNLIMITED)
            val collector = launch(collectorDispatcher) { repository.receiverListFlow.collect { emissions.send(it) } }

            try {
                assertEquals(listOf(ACCOUNT_A_RECEIVER), emissions.receiveNames())

                blockCollectorThread {
                    authRepository.logout().getOrThrow()
                    authRepository.saveSession(accessToken = "access-account-b", refreshToken = "refresh-account-b").getOrThrow()
                }

                // 세션 경계가 전달됐다면 여기서 재시작 방출(빈 목록)이 온다. 늦게 오면 아래에서 받는다.
                val afterBoundary = withTimeoutOrNull(BOUNDARY_WAIT_MILLIS) { emissions.receive() }

                createReceiver(repository, "계정 B 수신자")
                val afterNewAccountFailure = withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() }

                // 재시작 방출이 늦게 와도 아래 단언은 그것을 받아 통과한다. 타이밍으로 red 가 되지 않는다.
                assertEquals(
                    "새 계정 세션의 조회 실패가 이전 계정 목록으로 낮아졌다 " +
                        "(세션 경계 재시작 방출=${afterBoundary?.map { it.name }}, getReceivers 호출=$requestCount)",
                    emptyList<String>(),
                    afterNewAccountFailure.map { it.name },
                )
            } finally {
                collector.cancelCollector()
            }
        }

    /**
     * 세션 식별자를 토큰 값에서 끌어오면(해시 등) 이 경우에 이전 세션과 값이 같아져 캐시가 그대로 살아난다.
     * 로그아웃이 저장소를 통째로 비우므로 "직전 값 + 1" 같은 증가 카운터도 같은 함정에 빠진다.
     */
    @Test
    fun `로그아웃 뒤 같은 토큰 값으로 다시 로그인해도 이전 계정 목록은 남지 않는다`() =
        runBlocking {
            var requestCount = 0
            val userApiService =
                StubUserApiService(
                    onGetReceivers = {
                        requestCount += 1
                        if (requestCount == 1) {
                            receiverListResponse(ACCOUNT_A_RECEIVER)
                        } else {
                            throw UnknownHostException("재로그인 세션 조회 실패")
                        }
                    },
                )
            val authRepository = authRepository()
            val repository = UserReceiverRepositoryImpl(userApiService, sessionStore.tokenDataSource, NoopErrorReporter)

            authRepository.saveSession(accessToken = SAME_ACCESS_TOKEN, refreshToken = SAME_REFRESH_TOKEN).getOrThrow()

            val emissions = Channel<List<Receiver>>(capacity = Channel.UNLIMITED)
            val collector = launch(collectorDispatcher) { repository.receiverListFlow.collect { emissions.send(it) } }

            try {
                assertEquals(listOf(ACCOUNT_A_RECEIVER), emissions.receiveNames())

                blockCollectorThread {
                    authRepository.logout().getOrThrow()
                    authRepository.saveSession(accessToken = SAME_ACCESS_TOKEN, refreshToken = SAME_REFRESH_TOKEN).getOrThrow()
                }

                withTimeoutOrNull(BOUNDARY_WAIT_MILLIS) { emissions.receive() }
                createReceiver(repository, "재로그인 세션 수신자")

                assertEquals(
                    "같은 토큰 값으로 다시 로그인한 세션이 이전 세션으로 취급됐다 (getReceivers 호출=$requestCount)",
                    emptyList<String>(),
                    withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() }.map { it.name },
                )
            } finally {
                collector.cancelCollector()
            }
        }

    /** 토큰 회전은 계정이 바뀌는 사건이 아니다. 여기서 캐시를 버리면 화면이 보고 있던 목록이 빈다. */
    @Test
    fun `같은 세션의 토큰 회전 뒤에도 마지막으로 성공한 목록을 유지한다`() =
        runBlocking {
            var requestCount = 0
            val userApiService =
                StubUserApiService(
                    onGetReceivers = {
                        requestCount += 1
                        if (requestCount == 1) {
                            receiverListResponse(ACCOUNT_A_RECEIVER)
                        } else {
                            throw UnknownHostException("회전 뒤 갱신 조회 실패")
                        }
                    },
                )
            val authRepository = authRepository(onReissue = { ReissueDto(accessToken = "회전된 액세스", refreshToken = "회전된 리프레시") })
            val repository = UserReceiverRepositoryImpl(userApiService, sessionStore.tokenDataSource, NoopErrorReporter)

            authRepository.saveSession(accessToken = "access-account-a", refreshToken = "refresh-account-a").getOrThrow()

            val emissions = Channel<List<Receiver>>(capacity = Channel.UNLIMITED)
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) { repository.receiverListFlow.collect { emissions.send(it) } }

            try {
                assertEquals(listOf(ACCOUNT_A_RECEIVER), emissions.receiveNames())

                authRepository.rotateToken().getOrThrow()
                createReceiver(repository, "회전 뒤 등록한 수신자")

                assertEquals(
                    "토큰 회전이 세션 교체로 취급돼 캐시가 버려졌다 (getReceivers 호출=$requestCount)",
                    listOf(ACCOUNT_A_RECEIVER),
                    withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() }.map { it.name },
                )
                assertEquals(2, requestCount)
            } finally {
                collector.cancelCollector()
            }
        }

    /** 세션 식별자 키가 생기기 전에 저장된 설치본이다. 로그인이 풀려서도, 회전이 세션 교체로 보여서도 안 된다. */
    @Test
    fun `세션 식별자 없이 저장된 기존 토큰도 로그인 상태이고 회전 뒤 목록을 유지한다`() =
        runBlocking {
            var requestCount = 0
            val userApiService =
                StubUserApiService(
                    onGetReceivers = {
                        requestCount += 1
                        if (requestCount == 1) {
                            receiverListResponse(LEGACY_RECEIVER)
                        } else {
                            throw UnknownHostException("회전 뒤 갱신 조회 실패")
                        }
                    },
                )
            val authRepository = authRepository(onReissue = { ReissueDto(accessToken = "회전된 액세스", refreshToken = "회전된 리프레시") })
            val repository = UserReceiverRepositoryImpl(userApiService, sessionStore.tokenDataSource, NoopErrorReporter)

            sessionStore.writeLegacyTokens(accessToken = "기존 설치본 액세스", refreshToken = "기존 설치본 리프레시")

            val emissions = Channel<List<Receiver>>(capacity = Channel.UNLIMITED)
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) { repository.receiverListFlow.collect { emissions.send(it) } }

            try {
                assertEquals(
                    "세션 식별자 없이 저장된 토큰이 로그아웃으로 읽혔다 (getReceivers 호출=$requestCount)",
                    listOf(LEGACY_RECEIVER),
                    emissions.receiveNames(),
                )

                authRepository.rotateToken().getOrThrow()
                createReceiver(repository, "회전 뒤 등록한 수신자")

                assertEquals(
                    listOf(LEGACY_RECEIVER),
                    withTimeout(TEST_TIMEOUT_MILLIS) { emissions.receive() }.map { it.name },
                )
            } finally {
                collector.cancelCollector()
            }
        }

    /** 세션이 바뀌는 순간 매달려 있던 이전 세션의 조회는 취소돼야 하고, 늦게 끝나도 새 세션에 방출되면 안 된다. */
    @Test
    fun `이전 세션의 늦은 조회 결과는 새 세션 목록으로 새지 않는다`() =
        runBlocking {
            var requestCount = 0
            val previousSessionFetchStarted = CompletableDeferred<Unit>()
            val previousSessionFetchCancelled = CompletableDeferred<Unit>()
            val previousSessionResponse = CompletableDeferred<BaseResponse<List<ReceiverListDto>>>()
            val userApiService =
                StubUserApiService(
                    onGetReceivers = {
                        requestCount += 1
                        if (requestCount == 1) {
                            previousSessionFetchStarted.complete(Unit)
                            try {
                                previousSessionResponse.await()
                            } catch (cancellation: CancellationException) {
                                previousSessionFetchCancelled.complete(Unit)
                                throw cancellation
                            }
                        } else {
                            receiverListResponse(ACCOUNT_B_RECEIVER)
                        }
                    },
                )
            val authRepository = authRepository()
            val repository = UserReceiverRepositoryImpl(userApiService, sessionStore.tokenDataSource, NoopErrorReporter)

            authRepository.saveSession(accessToken = "access-account-a", refreshToken = "refresh-account-a").getOrThrow()

            val emissions = Channel<List<Receiver>>(capacity = Channel.UNLIMITED)
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) { repository.receiverListFlow.collect { emissions.send(it) } }

            try {
                withTimeout(TEST_TIMEOUT_MILLIS) { previousSessionFetchStarted.await() }

                authRepository.logout().getOrThrow()
                authRepository.saveSession(accessToken = "access-account-b", refreshToken = "refresh-account-b").getOrThrow()

                withTimeout(TEST_TIMEOUT_MILLIS) { previousSessionFetchCancelled.await() }
                previousSessionResponse.complete(receiverListResponse(ACCOUNT_A_RECEIVER))

                assertEquals(listOf(ACCOUNT_B_RECEIVER), emissions.receiveFirstNonEmptyNames())
                assertNull(
                    "이전 세션의 늦은 응답이 새 세션 목록으로 방출됐다",
                    withTimeoutOrNull(BOUNDARY_WAIT_MILLIS) { emissions.receive() },
                )
            } finally {
                collector.cancelCollector()
            }
        }

    /**
     * 수집자가 이전 계정 목록을 들고 있는 동안 세션이 바뀌면, 새 세션 조회가 끝나기 전에 목록이 먼저
     * 비어야 한다. 하류 `stateIn(WhileSubscribed(5000), emptyList())` 의 initialValue 는 내부 flow 가
     * 교체돼도 되돌아가지 않으므로(ReceiverListViewModel · RecipientListViewModel 둘 다 이 조립이다),
     * 여기서 안 비우면 새 계정 화면에 이전 계정 수신인이 조회가 끝날 때까지 떠 있다.
     *
     * 이 테스트는 새 세션 조회를 응답 전 상태로 붙잡아 둔 채 단언한다. 조회 실패를 기다려서 비는 것과
     * 전환 즉시 비는 것을 가르기 위해서다.
     */
    @Test
    fun `세션이 바뀌면 새 세션 조회가 끝나기 전에 이전 계정 목록을 먼저 비운다`() =
        runBlocking {
            val fetches = ScriptedReceiverFetches()
            val authRepository = authRepository()
            val repository =
                UserReceiverRepositoryImpl(
                    StubUserApiService(onGetReceivers = { fetches.fetch() }),
                    sessionStore.tokenDataSource,
                    NoopErrorReporter,
                )

            authRepository.saveSession(accessToken = "access-account-a", refreshToken = "refresh-account-a").getOrThrow()

            val emissions = Channel<List<Receiver>>(capacity = Channel.UNLIMITED)
            val collector = launch(collectorDispatcher) { repository.receiverListFlow.collect { emissions.send(it) } }

            try {
                fetches.awaitFetchStarted()
                fetches.respondWith(ACCOUNT_A_RECEIVER)
                assertEquals(listOf(ACCOUNT_A_RECEIVER), emissions.receiveNames())

                // 수집자가 중간 로그아웃을 못 보는 구간. 재개되면 세션 식별자가 A 에서 B 로 바뀐 것만 보인다.
                sessionStore.holdObservation()
                authRepository.logout().getOrThrow()
                authRepository.saveSession(accessToken = "access-account-b", refreshToken = "refresh-account-b").getOrThrow()
                sessionStore.releaseObservation()

                fetches.awaitFetchStarted()
                assertEquals(
                    "새 세션 조회가 매달린 동안 이전 계정 목록이 이 수집자의 현재 값으로 남았다",
                    emptyList<String>(),
                    emissions.receiveNames(),
                )
                assertEquals(
                    "이 단언은 새 세션 조회가 아직 응답하기 전의 방출을 봐야 한다",
                    1,
                    fetches.answeredCount,
                )

                fetches.respondWith(ACCOUNT_B_RECEIVER)
                assertEquals(listOf(ACCOUNT_B_RECEIVER), emissions.receiveNames())
            } finally {
                collector.cancelCollector()
            }
        }

    /**
     * 세션 전환이 저장소에는 이미 커밋됐지만 상위 [com.afternote.core.datastore.TokenDataSource.sessionId]
     * 관측이 아직 새 식별자를 전달하지 못한 구간이 있다. 그 구간에서는 이전 세션의 내부 flow 가 아직
     * 취소되지 않았으므로, 그때 끝난 조회의 성공도 실패 폴백도 이전 계정 목록을 새 계정으로 흘려보낸다.
     *
     * 이 테스트가 증명하는 창은 취소 이전이 아니라 **관측 이전** 이다. 취소를 먼저 기다리는 테스트는 이미
     * 취소된 경로만 보므로 이 구멍을 못 본다. 한 시나리오에서 실패 폴백(401 아님)과 성공 응답을 차례로
     * 태워 둘 다 고정한다. 이미 방출된 값을 되돌린다는 뜻은 아니다. 전환 뒤에 새로 방출되지 않는다는 뜻이다.
     */
    @Test
    fun `상위 관측이 새 세션을 받기 전에 끝난 이전 세션 조회는 목록에도 캐시에도 반영되지 않는다`() =
        runBlocking {
            val fetches = ScriptedReceiverFetches()
            val authRepository = authRepository()
            val repository =
                UserReceiverRepositoryImpl(
                    StubUserApiService(onGetReceivers = { fetches.fetch() }),
                    sessionStore.tokenDataSource,
                    NoopErrorReporter,
                )

            authRepository.saveSession(accessToken = "access-account-a", refreshToken = "refresh-account-a").getOrThrow()

            val emissions = Channel<List<Receiver>>(capacity = Channel.UNLIMITED)
            val collector = launch(collectorDispatcher) { repository.receiverListFlow.collect { emissions.send(it) } }

            try {
                fetches.awaitFetchStarted()
                fetches.respondWith(ACCOUNT_A_RECEIVER)
                assertEquals(listOf(ACCOUNT_A_RECEIVER), emissions.receiveNames())

                // 여기서부터 상위 관측만 멈춘다. 쓰기는 그대로 커밋되고 즉시 재조회는 새 값을 본다.
                sessionStore.holdObservation()

                createReceiver(repository, "전환 직전 등록")
                fetches.awaitFetchStarted()
                authRepository.logout().getOrThrow()
                authRepository.saveSession(accessToken = "access-account-b", refreshToken = "refresh-account-b").getOrThrow()

                fetches.failWith(UnknownHostException("이전 세션 조회 실패"))
                assertNull(
                    "이전 세션 조회의 실패 폴백이 새 세션에서 이전 계정 목록으로 방출됐다",
                    withTimeoutOrNull(BOUNDARY_WAIT_MILLIS) { emissions.receive() },
                )

                createReceiver(repository, "전환 뒤 등록")
                fetches.awaitFetchStarted()
                fetches.respondWith(ACCOUNT_A_RECEIVER)
                assertNull(
                    "이전 세션 조회의 늦은 성공 응답이 새 세션 목록으로 방출됐다",
                    withTimeoutOrNull(BOUNDARY_WAIT_MILLIS) { emissions.receive() },
                )

                // 관측이 풀리면 비로소 새 세션의 목록이 선다. 그 전까지 받은 값에 이전 계정은 없어야 한다.
                sessionStore.releaseObservation()
                fetches.awaitFetchStarted()
                fetches.respondWith(ACCOUNT_B_RECEIVER)
                assertEquals(listOf(ACCOUNT_B_RECEIVER), emissions.receiveFirstNonEmptyNames())
            } finally {
                collector.cancelCollector()
            }
        }

    /**
     * 수집자 스레드를 잠근 채 [duringBlackout] 을 실행한다. 그 구간의 세션 변화는 수집자에게 뭉개져 전달된다.
     * 빗장은 어떤 경로로 빠져나가든 반드시 풀어야 한다. 안 풀면 뒤따르는 수집자 취소가 그 스레드를 영영 기다린다.
     */
    private suspend fun blockCollectorThread(duringBlackout: suspend () -> Unit) {
        collectorExecutor.execute { collectorGate.await() }
        try {
            duringBlackout()
        } finally {
            collectorGate.countDown()
        }
    }

    private suspend fun Job.cancelCollector() {
        collectorGate.countDown()
        cancelAndJoin()
    }

    private suspend fun Channel<List<Receiver>>.receiveNames(): List<String> =
        withTimeout(TEST_TIMEOUT_MILLIS) { receive() }.map { it.name }

    private suspend fun Channel<List<Receiver>>.receiveFirstNonEmptyNames(): List<String> {
        while (true) {
            val names = receiveNames()
            if (names.isNotEmpty()) return names
        }
    }

    private suspend fun createReceiver(
        repository: UserReceiverRepositoryImpl,
        name: String,
    ) = repository.createReceiver(
        name = name,
        relation = "친구",
        phone = null,
        email = "receiver@example.com",
        message = null,
    )

    private fun authRepository(onReissue: suspend () -> ReissueDto = { error("reissue 는 이 테스트에서 호출되면 안 됨") }): AuthRepository =
        AuthRepositoryImpl(
            tokenDataSource = sessionStore.tokenDataSource,
            authApiService = StubAuthApiService,
            tokenApiService = StubTokenApiService(onReissue),
            expiryTracker = AccessTokenExpiryTracker(),
            // 실제 레지스트리와 같은 정리 방식이다. SESSION 스코프 저장소의 키를 비운다.
            localStoreRegistry =
                object : LocalStoreRegistry {
                    override fun store(
                        name: String,
                        scope: StoreScope,
                    ) = error("store 는 이 테스트에서 호출되면 안 됨")

                    override suspend fun clearScope(scope: StoreScope) {
                        if (scope == StoreScope.SESSION) sessionStore.clearSession()
                    }
                },
            pushTargetRepository =
                object : PushTargetRepository {
                    override suspend fun register(targetId: String): Result<Unit> = error("register 는 이 테스트에서 호출되면 안 됨")

                    override suspend fun unregister(targetId: String): Result<Unit> = Result.success(Unit)
                },
            devicePushTargetProvider =
                object : DevicePushTargetProvider {
                    override suspend fun currentTargetId(): String? = error("currentTargetId 는 이 테스트에서 호출되면 안 됨")

                    override suspend fun existingTargetId(): String? = null
                },
        )

    /**
     * `getReceivers` 를 테스트가 한 건씩 열어 주는 조회로 바꾼다. 조회가 시작된 사실과 응답 시점을 테스트가
     * 따로 쥐고 있어야 "새 세션 조회가 아직 안 끝난 상태" 를 단언할 수 있다.
     */
    private class ScriptedReceiverFetches {
        private val started = Channel<Unit>(capacity = Channel.UNLIMITED)
        private val responses = Channel<Result<BaseResponse<List<ReceiverListDto>>>>(capacity = Channel.UNLIMITED)
        private val answered = AtomicInteger()

        /** 응답까지 끝난 조회 수. 조회 코루틴이 다른 스레드라 원자 카운터로 읽는다. */
        val answeredCount: Int get() = answered.get()

        suspend fun fetch(): BaseResponse<List<ReceiverListDto>> {
            started.send(Unit)
            val response = responses.receive()
            answered.incrementAndGet()
            return response.getOrThrow()
        }

        suspend fun awaitFetchStarted() {
            withTimeout(TEST_TIMEOUT_MILLIS) { started.receive() }
        }

        suspend fun respondWith(receiverName: String) = responses.send(Result.success(receiverListResponse(receiverName)))

        suspend fun failWith(error: Throwable) = responses.send(Result.failure(error))
    }

    private object NoopErrorReporter : ErrorReporter {
        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) = Unit
    }

    private object StubAuthApiService : AuthApiService {
        override suspend fun login(body: LoginRequestDto): BaseResponse<LoginDto.DefaultLoginDto> = error("login 은 이 테스트에서 호출되면 안 됨")

        override suspend fun socialLogin(body: SocialLoginRequestDto): BaseResponse<LoginDto.SocialLoginDto> =
            error("socialLogin 은 이 테스트에서 호출되면 안 됨")

        override suspend fun logout(body: LogoutRequestDto): BaseResponse<Unit> = BaseResponse(status = 200, code = 200)
    }

    private class StubTokenApiService(
        private val onReissue: suspend () -> ReissueDto,
    ) : TokenApiService {
        override suspend fun reissue(body: ReissueRequestDto): BaseResponse<ReissueDto> =
            BaseResponse(status = 200, code = 200, data = onReissue())
    }

    private class StubUserApiService(
        private val onGetReceivers: suspend () -> BaseResponse<List<ReceiverListDto>>,
    ) : UserApiService {
        override suspend fun getReceivers(): BaseResponse<List<ReceiverListDto>> = onGetReceivers()

        override suspend fun createReceiver(request: UserCreateReceiverRequestDto): BaseResponse<UserCreateReceiverDto> =
            BaseResponse(status = 200, code = 200, data = UserCreateReceiverDto(receiverId = 2L, authCode = "AUTH-2"))

        override suspend fun deleteAccount(): BaseResponse<Unit> = TODO("이 테스트 미사용")

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
        const val ACCOUNT_A_RECEIVER = "계정 A 수신인"
        const val ACCOUNT_B_RECEIVER = "계정 B 수신인"
        const val LEGACY_RECEIVER = "기존 설치본 수신인"
        const val SAME_ACCESS_TOKEN = "같은 값의 액세스 토큰"
        const val SAME_REFRESH_TOKEN = "같은 값의 리프레시 토큰"
        const val TEST_TIMEOUT_MILLIS = 5_000L
        const val BOUNDARY_WAIT_MILLIS = 1_000L

        fun receiverListResponse(name: String) =
            BaseResponse(
                status = 200,
                code = 200,
                data =
                    listOf(
                        ReceiverListDto(
                            receiverId = 1L,
                            name = name,
                            relation = "친구",
                            authCode = "AUTH-1",
                        ),
                    ),
            )
    }
}
