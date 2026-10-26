package com.afternote.core.data.repoimpl.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.afternote.core.datastore.LocalStoreRegistry
import com.afternote.core.datastore.StoreScope
import com.afternote.core.datastore.TokenDataSource
import com.afternote.core.domain.error.SessionChangedException
import com.afternote.core.domain.push.DevicePushTargetProvider
import com.afternote.core.domain.repository.push.PushTargetRepository
import com.afternote.core.network.dto.LoginDto
import com.afternote.core.network.dto.LoginRequestDto
import com.afternote.core.network.dto.LogoutRequestDto
import com.afternote.core.network.dto.ReissueDto
import com.afternote.core.network.dto.ReissueRequestDto
import com.afternote.core.network.dto.SocialLoginRequestDto
import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.service.AuthApiService
import com.afternote.core.network.service.TokenApiService
import com.afternote.core.network.token.AccessTokenExpiryTracker
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * 재발급 응답은 그 재발급을 시작한 세션에만 적용된다 (#2237).
 *
 * 재발급 HTTP 가 떠 있는 동안 로그아웃이나 새 로그인이 끝날 수 있다. 둘 다 재발급 락을 거치지 않기 때문이다.
 * 그 뒤 도착한 성공 응답을 그대로 쓰면 로그아웃한 계정이 되살아나거나(세션 식별자가 없어 legacy 세션으로 읽힌다)
 * 새 계정의 토큰이 이전 계정 토큰으로 덮인다. 저장소는 실제 파일 DataStore 이고, 쓰기 직전에 다른 쓰기를
 * 끼워 넣는 장치로 확인과 저장 사이의 창까지 결정적으로 연다.
 */
class SessionBoundReissueTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tracker = AccessTokenExpiryTracker()
    private val reissue = ScriptedReissue()
    private lateinit var store: InterleavingDataStore
    private lateinit var tokenDataSource: TokenDataSource
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        val file = File(temporaryFolder.root, "Token.preferences_pb")
        store = InterleavingDataStore(PreferenceDataStoreFactory.create(scope = storeScope) { file })
        tokenDataSource = TokenDataSource(store)
        repository =
            AuthRepositoryImpl(
                tokenDataSource = tokenDataSource,
                authApiService = LogoutOnlyAuthApiService,
                tokenApiService = reissue,
                expiryTracker = tracker,
                // 실제 레지스트리처럼 SESSION 정리가 토큰 저장소의 키를 비운다.
                localStoreRegistry =
                    object : LocalStoreRegistry {
                        override fun store(
                            name: String,
                            scope: StoreScope,
                        ): DataStore<Preferences> = error("store 는 이 테스트에서 호출되면 안 됨")

                        override suspend fun clearScope(scope: StoreScope) {
                            if (scope == StoreScope.SESSION) store.edit { it.clear() }
                        }
                    },
                pushTargetRepository = NoopPushTargetRepository,
                devicePushTargetProvider = NoDevicePushTargetProvider,
            )
    }

    @After
    fun tearDown() = storeScope.cancel()

    @Test
    fun `로그아웃 뒤 도착한 재발급 성공은 이전 세션을 되살리지 않는다`() =
        runBlocking {
            repository.saveSession(accessToken = "A-access", refreshToken = "A-refresh").getOrThrow()
            val rotation = async(Dispatchers.IO) { repository.rotateToken(currentSessionId()) }
            reissue.awaitStarted()

            repository.logout().getOrThrow()
            assertNull(tokenDataSource.getAccessToken())

            reissue.respond(ReissueDto(accessToken = "A-rotated", refreshToken = "A-refresh-rotated", expiresIn = 3599))
            val result = withTimeout(TIMEOUT_MILLIS) { rotation.await() }

            assertTrue("쓰이지 않은 회전이 성공으로 보고됐다: $result", result.exceptionOrNull() is SessionChangedException)
            assertNull(tokenDataSource.getAccessToken())
            assertNull(tokenDataSource.getRefreshToken())
            assertNull(tokenDataSource.sessionId.first())
            assertFalse(tokenDataSource.isLoggedIn.first())
        }

    @Test
    fun `재발급 대기 중 새로 로그인한 세션은 늦은 성공에 덮이지 않는다`() =
        runBlocking {
            repository.saveSession(accessToken = "A-access", refreshToken = "A-refresh").getOrThrow()
            val rotation = async(Dispatchers.IO) { repository.rotateToken(currentSessionId()) }
            reissue.awaitStarted()

            repository.logout().getOrThrow()
            repository.saveSession(accessToken = "B-access", refreshToken = "B-refresh").getOrThrow()
            val sessionB = tokenDataSource.sessionId.first()

            reissue.respond(ReissueDto(accessToken = "A-rotated", refreshToken = "A-refresh-rotated"))
            val result = withTimeout(TIMEOUT_MILLIS) { rotation.await() }

            assertTrue(result.exceptionOrNull() is SessionChangedException)
            assertEquals("B-access", tokenDataSource.getAccessToken())
            assertEquals("B-refresh", tokenDataSource.getRefreshToken())
            assertEquals(sessionB, tokenDataSource.sessionId.first())
        }

    @Test
    fun `세션 확인과 저장 사이에 끼어든 새 로그인을 덮지 않는다`() =
        runBlocking {
            repository.saveSession(accessToken = "A-access", refreshToken = "A-refresh").getOrThrow()
            val rotation = async(Dispatchers.IO) { repository.rotateToken(currentSessionId()) }
            reissue.awaitStarted()

            // 응답 뒤 저장소에 처음 닿는 순간(읽기든 쓰기든)에 로그아웃과 B 로그인을 끼워 넣는다.
            // 읽기라면 읽은 값(A)을 돌려준 뒤가 아니라 돌려주기 직전에 끼워, 확인한 값과 저장 시점의 값이 갈리게 한다.
            store.interleaveOnce {
                store.edit { it.clear() }
                tokenDataSource.saveTokens(accessToken = "B-access", refreshToken = "B-refresh")
            }
            reissue.respond(ReissueDto(accessToken = "A-rotated", refreshToken = "A-refresh-rotated"))
            val result = withTimeout(TIMEOUT_MILLIS) { rotation.await() }

            assertTrue("끼워 넣기가 실행되지 않았다", store.interleaved)
            assertTrue(result.exceptionOrNull() is SessionChangedException)
            assertEquals("B-access", tokenDataSource.getAccessToken())
            assertEquals("B-refresh", tokenDataSource.getRefreshToken())
        }

    @Test
    fun `같은 세션의 회전은 토큰만 바꾸고 세션 식별자를 유지한다`() =
        runBlocking {
            repository.saveSession(accessToken = "A-access", refreshToken = "A-refresh").getOrThrow()
            val sessionA = tokenDataSource.sessionId.first()
            assertNotNull(sessionA)
            val rotation = async(Dispatchers.IO) { repository.rotateToken(currentSessionId()) }

            assertEquals(ReissueRequestDto("A-refresh"), reissue.awaitStarted())
            reissue.respond(ReissueDto(accessToken = "A-rotated", refreshToken = "A-refresh-rotated", expiresIn = 3599))
            val bundle = withTimeout(TIMEOUT_MILLIS) { rotation.await() }.getOrThrow()

            assertEquals("A-rotated", bundle.accessToken)
            assertEquals("A-rotated", tokenDataSource.getAccessToken())
            assertEquals("A-refresh-rotated", tokenDataSource.getRefreshToken())
            assertEquals(sessionA, tokenDataSource.sessionId.first())
        }

    @Test
    fun `식별자 없이 저장된 기존 세션도 회전하고 식별자 없는 채로 남는다`() =
        runBlocking {
            store.edit { prefs ->
                prefs[stringPreferencesKey("access_token")] = "legacy-access"
                prefs[stringPreferencesKey("refresh_token")] = "legacy-refresh"
            }
            val legacySession = currentSessionId()
            val rotation = async(Dispatchers.IO) { repository.rotateToken(legacySession) }

            assertEquals(ReissueRequestDto("legacy-refresh"), reissue.awaitStarted())
            reissue.respond(ReissueDto(accessToken = "legacy-rotated", refreshToken = "legacy-refresh-rotated"))
            withTimeout(TIMEOUT_MILLIS) { rotation.await() }.getOrThrow()

            assertEquals("legacy-rotated", tokenDataSource.getAccessToken())
            assertEquals(legacySession, tokenDataSource.sessionId.first())
        }

    @Test
    fun `이전 세션의 정리 요청은 새로 로그인한 세션과 그 deadline 을 지우지 않는다`() =
        runBlocking {
            repository.saveSession(accessToken = "A-access", refreshToken = "A-refresh").getOrThrow()
            val sessionA = currentSessionId()
            repository.logout().getOrThrow()
            repository.saveSession(accessToken = "B-access", refreshToken = "B-refresh").getOrThrow()
            val sessionB = currentSessionId()
            tracker.record(expiresInSeconds = 30)

            val cleared = repository.clearSessionIfCurrent(sessionA).getOrThrow()

            assertFalse(cleared)
            assertEquals("B-access", tokenDataSource.getAccessToken())
            assertEquals("B-refresh", tokenDataSource.getRefreshToken())
            assertEquals(sessionB, tokenDataSource.sessionId.first())
            assertTrue("B 의 선제 갱신 deadline 이 지워졌다", tracker.isExpiringSoon())
        }

    @Test
    fun `세션 확인과 정리 사이에 끼어든 새 로그인을 지우지 않는다`() =
        runBlocking {
            repository.saveSession(accessToken = "A-access", refreshToken = "A-refresh").getOrThrow()
            val sessionA = currentSessionId()
            store.interleaveOnce {
                store.edit { it.clear() }
                tokenDataSource.saveTokens(accessToken = "B-access", refreshToken = "B-refresh")
            }

            val cleared = repository.clearSessionIfCurrent(sessionA).getOrThrow()

            assertTrue("끼워 넣기가 실행되지 않았다", store.interleaved)
            assertFalse(cleared)
            assertEquals("B-access", tokenDataSource.getAccessToken())
        }

    @Test
    fun `현재 세션의 정리 요청은 토큰과 deadline 을 함께 정리한다`() =
        runBlocking {
            repository.saveSession(accessToken = "A-access", refreshToken = "A-refresh").getOrThrow()
            tracker.record(expiresInSeconds = 30)

            val cleared = repository.clearSessionIfCurrent(currentSessionId()).getOrThrow()

            assertTrue(cleared)
            assertNull(tokenDataSource.getAccessToken())
            assertNull(tokenDataSource.getRefreshToken())
            assertFalse(tokenDataSource.isLoggedIn.first())
            assertFalse(tracker.isExpiringSoon())
        }

    private suspend fun currentSessionId(): String = checkNotNull(repository.getSessionId().getOrThrow())

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}

/** 재발급 요청이 들어온 사실과 응답 시점을 테스트가 따로 쥔다. */
private class ScriptedReissue : TokenApiService {
    private val started = CompletableDeferred<ReissueRequestDto>()
    private val response = CompletableDeferred<ReissueDto>()

    override suspend fun reissue(body: ReissueRequestDto): BaseResponse<ReissueDto> {
        started.complete(body)
        return BaseResponse(status = 200, code = 200, message = "성공", data = response.await())
    }

    suspend fun awaitStarted(): ReissueRequestDto = withTimeout(5_000L) { started.await() }

    fun respond(data: ReissueDto) {
        response.complete(data)
    }
}

/**
 * 다음 한 번의 접근에 다른 쓰기를 끼워 넣는 DataStore.
 *
 * 쓰기라면 그 쓰기보다 먼저 끼워 넣고, 읽기라면 읽은 값이 호출자에게 전달되기 직전에 끼워 넣는다. 읽기 쪽은
 * 호출자가 끼워 넣기 이전 값을 받으므로, 확인과 저장을 따로 하는 구현은 낡은 값으로 판정하게 된다.
 */
private class InterleavingDataStore(
    private val delegate: DataStore<Preferences>,
) : DataStore<Preferences> {
    @Volatile
    private var pending: (suspend () -> Unit)? = null

    @Volatile
    var interleaved: Boolean = false
        private set

    fun interleaveOnce(action: suspend () -> Unit) {
        pending = action
    }

    private suspend fun runPending() {
        val action = synchronized(this) { pending.also { pending = null } } ?: return
        action()
        interleaved = true
    }

    override val data: Flow<Preferences>
        get() = delegate.data.onEach { runPending() }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        runPending()
        return delegate.updateData(transform)
    }
}

private object LogoutOnlyAuthApiService : AuthApiService {
    override suspend fun login(body: LoginRequestDto): BaseResponse<LoginDto.DefaultLoginDto> = error("login 은 이 테스트에서 호출되면 안 됨")

    override suspend fun socialLogin(body: SocialLoginRequestDto): BaseResponse<LoginDto.SocialLoginDto> =
        error("socialLogin 은 이 테스트에서 호출되면 안 됨")

    override suspend fun logout(body: LogoutRequestDto): BaseResponse<Unit> =
        BaseResponse(status = 200, code = 200, message = "성공", data = Unit)
}

private object NoopPushTargetRepository : PushTargetRepository {
    override suspend fun register(targetId: String): Result<Unit> = error("register 는 이 테스트에서 호출되면 안 됨")

    override suspend fun unregister(targetId: String): Result<Unit> = Result.success(Unit)
}

private object NoDevicePushTargetProvider : DevicePushTargetProvider {
    override suspend fun currentTargetId(): String? = error("currentTargetId 는 이 테스트에서 호출되면 안 됨")

    override suspend fun existingTargetId(): String? = null
}
