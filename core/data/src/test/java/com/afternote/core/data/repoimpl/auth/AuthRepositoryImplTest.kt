package com.afternote.core.data.repoimpl.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.afternote.core.data.repoimpl.UserProfileRepositoryImpl
import com.afternote.core.data.session.UserProfileSessionResource
import com.afternote.core.data.session.UserReceiverCache
import com.afternote.core.data.session.UserSessionBoundary
import com.afternote.core.datastore.TokenDataSource
import com.afternote.core.datastore.UserProfileDataSource
import com.afternote.core.domain.error.LoginRejectedException
import com.afternote.core.domain.error.NetworkUnavailableException
import com.afternote.core.domain.session.SessionScopedResource
import com.afternote.core.model.user.Receiver
import com.afternote.core.network.dto.LoginDto
import com.afternote.core.network.dto.LoginRequestDto
import com.afternote.core.network.dto.LogoutRequestDto
import com.afternote.core.network.dto.ReissueDto
import com.afternote.core.network.dto.ReissueRequestDto
import com.afternote.core.network.dto.SocialLoginRequestDto
import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.service.AuthApiService
import com.afternote.core.network.service.TokenApiService
import com.afternote.core.network.token.AccessTokenExpiryTracker
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException

/**
 * [AuthRepositoryImpl] 의 선제 reissue deadline 관리 계약 회귀 가드 (#408, PR #411 리뷰 반영).
 * 로그인 경로의 실패 매핑 계약(#517)도 함께 가드한다 — 전송 계층 IO 실패 →
 * [NetworkUnavailableException], 사유 확인된 거절 → [LoginRejectedException],
 * 그 밖의 서버 실패는 치환하지 않아 소비처에서 일반 문구로 내려앉는다.
 *
 * 계약 — 발급(로그인) 응답의 `expiresIn` 은 기록하고, 생략(null)이면 이전 토큰 기준 stale
 * deadline 이 새 세션에 적용되지 않게 비우며(`TokenReissuer` 회전 경로와 같은 규칙),
 * 세션 종료(`logout`/`clearSession`)는 로컬 토큰과 deadline 을 함께 정리한다.
 *
 * [AccessTokenExpiryTracker] 는 실물 사용 — 시계(SystemClock)는 `isReturnDefaultValues` 로
 * 0 에 고정되므로, 잔여 30초 기록 = 임박 true / 비움 = false 로 상태를 관찰한다 (임계 60초).
 */
class AuthRepositoryImplTest {
    private val tracker = AccessTokenExpiryTracker()
    private val tokenDataSource = TokenDataSource(InMemoryPreferencesDataStore())
    private val sessionBoundary = UserSessionBoundary()
    private val sessionResource = RecordingSessionScopedResource()

    private fun repository(
        authApiService: AuthApiService = FakeAuthApiService(),
        tokenApiService: TokenApiService = FakeTokenApiService(),
        sessionBoundary: UserSessionBoundary = this.sessionBoundary,
        sessionResources: Set<SessionScopedResource> = setOf(sessionResource),
    ) = AuthRepositoryImpl(
        tokenDataSource = tokenDataSource,
        authApiService = authApiService,
        tokenApiService = tokenApiService,
        expiryTracker = tracker,
        sessionBoundary = sessionBoundary,
        sessionScopedResources = sessionResources,
    )

    @Test
    fun `saveSession - 새 토큰 저장 전에 이전 사용자 상태를 정리한다`() {
        runBlocking { tokenDataSource.saveTokens(accessToken = "old-access", refreshToken = "old-refresh") }
        val orderingResource =
            RecordingSessionScopedResource(
                onClear = { assertNull(tokenDataSource.getAccessToken()) },
            )
        val repository = repository(sessionResources = setOf(orderingResource))
        val result =
            runBlocking {
                repository.saveSession(
                    accessToken = "new-access",
                    refreshToken = "new-refresh",
                )
            }

        assertTrue(result.isSuccess)
        assertEquals(1, orderingResource.clearCallCount)
        assertEquals("new-access", runBlocking { tokenDataSource.getAccessToken() })
        assertTrue(runBlocking { repository.isLoggedIn.first() })
    }

    @Test
    fun `saveSession - 사용자 상태 정리가 실패하면 새 토큰을 저장하지 않는다`() {
        runBlocking { tokenDataSource.saveTokens(accessToken = "old-access", refreshToken = "old-refresh") }
        val failing = RecordingSessionScopedResource(IllegalStateException("clear 실패"))
        val repository = repository(sessionResources = setOf(failing))

        val result =
            runBlocking {
                repository.saveSession(
                    accessToken = "new-access",
                    refreshToken = "new-refresh",
                )
            }

        assertTrue(result.isFailure)
        assertNull(runBlocking { tokenDataSource.getAccessToken() })
        assertFalse(runBlocking { repository.isLoggedIn.first() })
    }

    @Test
    fun `updateTokens - 같은 세션의 토큰 회전은 사용자 상태를 유지한다`() {
        val result =
            runBlocking {
                repository().updateTokens(
                    accessToken = "rotated-access",
                    refreshToken = "rotated-refresh",
                )
            }

        assertTrue(result.isSuccess)
        assertEquals(0, sessionResource.clearCallCount)
    }

    @Test
    fun `rotateToken - 세션 종료 뒤 완료된 이전 회전은 토큰을 복원하지 않는다`() =
        runBlocking {
            tokenDataSource.saveTokens(accessToken = "old-access", refreshToken = "old-refresh")
            val requestStarted = CompletableDeferred<Unit>()
            val releaseResponse = CompletableDeferred<Unit>()
            val repository =
                repository(
                    tokenApiService =
                        FakeTokenApiService {
                            requestStarted.complete(Unit)
                            releaseResponse.await()
                            success(
                                ReissueDto(
                                    accessToken = "late-access",
                                    refreshToken = "late-refresh",
                                ),
                            )
                        },
                )
            val rotation = async { repository.rotateToken() }

            requestStarted.await()
            assertTrue(repository.clearSession().isSuccess)
            releaseResponse.complete(Unit)

            assertTrue(rotation.await().isFailure)
            assertNull(tokenDataSource.getAccessToken())
        }

    @Test
    fun `defaultLogin - 발급 응답 expiresIn 을 deadline 으로 기록`() {
        val repository =
            repository(
                FakeAuthApiService(
                    onLogin = { success(LoginDto.DefaultLoginDto("access", "refresh", expiresIn = 30)) },
                ),
            )

        val result = runBlocking { repository.defaultLogin("user@example.com", "pw") }

        assertTrue(result.isSuccess)
        assertTrue(tracker.isExpiringSoon())
    }

    @Test
    fun `defaultLogin - expiresIn 미동봉이면 기존 deadline 폐기 (stale 방지)`() {
        tracker.record(expiresInSeconds = 30)
        val repository =
            repository(
                FakeAuthApiService(
                    onLogin = { success(LoginDto.DefaultLoginDto("access", "refresh")) },
                ),
            )

        val result = runBlocking { repository.defaultLogin("user@example.com", "pw") }

        assertTrue(result.isSuccess)
        assertFalse(tracker.isExpiringSoon())
    }

    @Test
    fun `socialLogin - expiresIn 미동봉이면 기존 deadline 폐기 (로그인 전 경로 공통 규칙)`() {
        tracker.record(expiresInSeconds = 30)
        val repository =
            repository(
                FakeAuthApiService(
                    onSocialLogin = { success(LoginDto.SocialLoginDto("access", "refresh")) },
                ),
            )

        val result = runBlocking { repository.kakaoLogin("oauth-token") }

        assertTrue(result.isSuccess)
        assertFalse(tracker.isExpiringSoon())
    }

    @Test
    fun `logout - 저장돼 있던 refresh 로 서버 호출 후 로컬 토큰·deadline 정리`() {
        runBlocking { tokenDataSource.saveTokens(accessToken = "access", refreshToken = "stored-refresh") }
        tracker.record(expiresInSeconds = 30)
        val authApiService = FakeAuthApiService()

        val result = runBlocking { repository(authApiService).logout() }

        assertTrue(result.isSuccess)
        assertEquals(listOf(LogoutRequestDto("stored-refresh")), authApiService.logoutRequests)
        assertNull(runBlocking { tokenDataSource.getRefreshToken() })
        assertFalse(tracker.isExpiringSoon())
        assertEquals(1, sessionResource.clearCallCount)
    }

    @Test
    fun `logout - 서버 호출 실패해도 (best-effort) 토큰·deadline 정리는 진행`() {
        runBlocking { tokenDataSource.saveTokens(accessToken = "access", refreshToken = "stored-refresh") }
        tracker.record(expiresInSeconds = 30)
        val authApiService =
            FakeAuthApiService(
                onLogout = { throw ApiException(status = 500, code = 500, serverMessage = null, message = "서버 오류") },
            )

        val result = runBlocking { repository(authApiService).logout() }

        assertTrue(result.isSuccess)
        assertNull(runBlocking { tokenDataSource.getRefreshToken() })
        assertFalse(tracker.isExpiringSoon())
        assertEquals(1, sessionResource.clearCallCount)
    }

    @Test
    fun `logout - 서버 호출 중 취소돼도 사용자 상태와 토큰을 정리하고 취소를 전파한다`() =
        runBlocking {
            tokenDataSource.saveTokens(accessToken = "access", refreshToken = "stored-refresh")
            val requestStarted = CompletableDeferred<Unit>()
            val authApiService =
                FakeAuthApiService(
                    onLogout = {
                        requestStarted.complete(Unit)
                        awaitCancellation()
                    },
                )
            val logoutRequest = async { repository(authApiService).logout() }

            requestStarted.await()
            logoutRequest.cancel(CancellationException("로그아웃 요청 취소"))
            val thrown =
                try {
                    logoutRequest.await()
                    null
                } catch (expected: CancellationException) {
                    expected
                }

            assertTrue(thrown is CancellationException)
            assertEquals(1, sessionResource.clearCallCount)
            assertNull(tokenDataSource.getAccessToken())
        }

    @Test
    fun `clearSession - 로컬 토큰·deadline 함께 정리`() {
        runBlocking { tokenDataSource.saveTokens(accessToken = "access", refreshToken = "refresh") }
        tracker.record(expiresInSeconds = 30)

        val result = runBlocking { repository().clearSession() }

        assertTrue(result.isSuccess)
        assertNull(runBlocking { tokenDataSource.getAccessToken() })
        assertFalse(tracker.isExpiringSoon())
        assertEquals(1, sessionResource.clearCallCount)
        val anonymousSession = runBlocking { sessionBoundary.currentActive() }
        assertNotNull(anonymousSession)
        assertFalse(requireNotNull(anonymousSession).isAuthenticated)
    }

    @Test
    fun `clearSession - 수신자와 프로필 캐시를 함께 정리한다`() =
        runBlocking {
            val sessionBoundary = UserSessionBoundary()
            val receiverCache = UserReceiverCache(sessionBoundary)
            val activeSession = requireNotNull(sessionBoundary.currentActive())
            receiverCache.update(
                session = activeSession,
                receivers =
                    listOf(
                        Receiver(
                            receiverId = 1L,
                            name = "계정 A 수신자",
                            relation = "친구",
                            authCode = "AUTH-CODE-A",
                        ),
                    ),
            )
            val profileDataSource = UserProfileDataSource(InMemoryPreferencesDataStore())
            profileDataSource.saveUserName("계정 A")
            val profileResource = UserProfileSessionResource(UserProfileRepositoryImpl(profileDataSource))

            val result =
                repository(
                    sessionBoundary = sessionBoundary,
                    sessionResources = setOf(receiverCache, profileResource),
                ).clearSession()

            assertTrue(result.isSuccess)
            assertNull(receiverCache.receiversFor(sessionBoundary.current))
            assertNull(profileDataSource.getCachedUserName())
        }

    @Test
    fun `clearSession - 한 사용자 상태 정리가 실패해도 나머지와 토큰을 모두 정리한다`() =
        runBlocking {
            tokenDataSource.saveTokens(accessToken = "access", refreshToken = "refresh")
            val failing = RecordingSessionScopedResource(IllegalStateException("clear 실패"))
            val succeeding = RecordingSessionScopedResource()

            val result =
                repository(
                    sessionResources = linkedSetOf(failing, succeeding),
                ).clearSession()

            assertTrue(result.isFailure)
            assertEquals(1, failing.clearCallCount)
            assertEquals(1, succeeding.clearCallCount)
            assertNull(tokenDataSource.getAccessToken())
        }

    @Test
    fun `defaultLogin - 전송 계층 IO 실패는 NetworkUnavailableException 으로 치환 (원인 보존)`() {
        val repository =
            repository(
                FakeAuthApiService(
                    onLogin = { throw UnknownHostException("Unable to resolve host") },
                ),
            )

        val result = runBlocking { repository.defaultLogin("user@example.com", "pw") }

        val exception = result.exceptionOrNull()
        assertTrue(exception is NetworkUnavailableException)
        assertTrue(exception?.cause is UnknownHostException)
    }

    @Test
    fun `defaultLogin - 사유 확인된 거절(1202)은 LoginRejectedException 으로 치환 (서버 문구 운반)`() {
        val serverMessage = "아이디 또는 비밀번호가 일치하지 않습니다."
        val repository =
            repository(
                FakeAuthApiService(
                    onLogin = { throw ApiException(status = 400, code = 1202, serverMessage = serverMessage, message = serverMessage) },
                ),
            )

        val result = runBlocking { repository.defaultLogin("user@example.com", "pw") }

        val exception = result.exceptionOrNull()
        assertTrue(exception is LoginRejectedException)
        assertEquals(serverMessage, (exception as LoginRejectedException).displayMessage)
    }

    @Test
    fun `defaultLogin - allowlist 밖 코드는 치환하지 않음 (5xx 내부 문구가 표시 경로로 못 감)`() {
        val internalMessage = "ERROR: duplicate key value violates unique constraint"
        val repository =
            repository(
                FakeAuthApiService(
                    onLogin = { throw ApiException(status = 500, code = 1904, serverMessage = internalMessage, message = internalMessage) },
                ),
            )

        val result = runBlocking { repository.defaultLogin("user@example.com", "pw") }

        // 치환되지 않으므로 소비처(LoginViewModel)의 else 갈래 = 일반 문구로 내려앉는다.
        assertTrue(result.exceptionOrNull() is ApiException)
    }

    @Test
    fun `defaultLogin - allowlist 코드라도 서버 message 가 없으면 치환하지 않음`() {
        val repository =
            repository(
                FakeAuthApiService(
                    onLogin = { throw ApiException(status = 400, code = 1201, serverMessage = null, message = "클라 폴백 문구") },
                ),
            )

        val result = runBlocking { repository.defaultLogin("user@example.com", "pw") }

        assertTrue(result.exceptionOrNull() is ApiException)
    }

    @Test
    fun `socialLogin - 사유 확인된 거절(1208)은 소셜 경로에도 적용`() {
        val serverMessage = "소셜 로그인에 실패했습니다."
        val repository =
            repository(
                FakeAuthApiService(
                    onSocialLogin = {
                        throw ApiException(
                            status = 400,
                            code = 1208,
                            serverMessage = serverMessage,
                            message = serverMessage,
                        )
                    },
                ),
            )

        val result = runBlocking { repository.kakaoLogin("oauth-token") }

        assertEquals(serverMessage, (result.exceptionOrNull() as LoginRejectedException).displayMessage)
    }

    @Test
    fun `socialLogin - 전송 계층 IO 실패 치환은 소셜 경로에도 적용`() {
        val repository =
            repository(
                FakeAuthApiService(
                    onSocialLogin = { throw UnknownHostException("Unable to resolve host") },
                ),
            )

        val result = runBlocking { repository.kakaoLogin("oauth-token") }

        assertTrue(result.exceptionOrNull() is NetworkUnavailableException)
    }

    @Test
    fun `defaultLogin - 취소는 Result 로 소비하지 않고 다시 던짐 (코루틴 취소 보존)`() {
        val repository =
            repository(
                FakeAuthApiService(
                    onLogin = { throw CancellationException("작업 취소") },
                ),
            )

        val thrown =
            try {
                runBlocking { repository.defaultLogin("user@example.com", "pw") }
                null
            } catch (expected: CancellationException) {
                expected
            }

        assertTrue(thrown is CancellationException)
    }

    @Test
    fun `googleLogin - 전송 계층 IO 실패 치환 (메서드별 독립 호출이라 개별 가드)`() {
        val repository =
            repository(
                FakeAuthApiService(
                    onSocialLogin = { throw UnknownHostException("Unable to resolve host") },
                ),
            )

        val result = runBlocking { repository.googleLogin("id-token") }

        assertTrue(result.exceptionOrNull() is NetworkUnavailableException)
    }
}

private fun <T> success(data: T) = BaseResponse(status = 200, code = 200, message = "성공", data = data)

/**
 * [AuthApiService] 테스트 공용 가짜 — 미지정 경로 호출은 error 로 드러낸다
 * (core:network 의 FakeAuthRepository 와 같은 규칙).
 */
private class FakeAuthApiService(
    private val onLogin: () -> BaseResponse<LoginDto.DefaultLoginDto> = {
        error("login 은 이 시나리오에서 호출되면 안 됨")
    },
    private val onSocialLogin: () -> BaseResponse<LoginDto.SocialLoginDto> = {
        error("socialLogin 은 이 시나리오에서 호출되면 안 됨")
    },
    private val onLogout: suspend () -> BaseResponse<Unit> = { success(Unit) },
) : AuthApiService {
    val logoutRequests = mutableListOf<LogoutRequestDto>()

    override suspend fun login(body: LoginRequestDto): BaseResponse<LoginDto.DefaultLoginDto> = onLogin()

    override suspend fun socialLogin(body: SocialLoginRequestDto): BaseResponse<LoginDto.SocialLoginDto> = onSocialLogin()

    override suspend fun logout(body: LogoutRequestDto): BaseResponse<Unit> {
        logoutRequests += body
        return onLogout()
    }
}

private class FakeTokenApiService(
    private val onReissue: suspend (ReissueRequestDto) -> BaseResponse<ReissueDto> = {
        error("reissue 는 이 시나리오에서 호출되면 안 됨")
    },
) : TokenApiService {
    override suspend fun reissue(body: ReissueRequestDto): BaseResponse<ReissueDto> = onReissue(body)
}

private class RecordingSessionScopedResource(
    private val failure: Throwable? = null,
    private val onClear: suspend () -> Unit = {},
) : SessionScopedResource {
    var clearCallCount = 0
        private set

    override suspend fun clear() {
        clearCallCount += 1
        onClear()
        failure?.let { throw it }
    }
}

/** 단위 테스트용 in-memory `DataStore<Preferences>` — 디스크 없이 [TokenDataSource] 실물을 구동한다. */
private class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> get() = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val transformed = transform(state.value)
        state.value = transformed
        return transformed
    }
}
