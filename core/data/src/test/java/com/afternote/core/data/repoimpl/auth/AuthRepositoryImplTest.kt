package com.afternote.core.data.repoimpl.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.afternote.core.datastore.LocalStoreRegistry
import com.afternote.core.datastore.StoreScope
import com.afternote.core.datastore.TokenDataSource
import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.domain.push.DevicePushTargetProvider
import com.afternote.core.domain.repository.push.PushTargetRepository
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException

/**
 * [AuthRepositoryImpl] 의 선제 reissue deadline 관리 계약 회귀 가드 (#408, PR #411 리뷰 반영).
 * 로그인 경로의 실패 매핑 계약(#628)도 함께 가드한다 — 전송 계층 IO 실패 →
 * [CoreAuthFailure.NetworkUnavailable], 자격 거절(1201·1202) → [CoreAuthFailure.InvalidLoginCredentials],
 * 소셜 거절(1208·1209) → [CoreAuthFailure.SocialLoginRejected], 소셜 가입 계정(1702) →
 * [CoreAuthFailure.SocialSignUpAccount], 그 밖의 서버 실패는 치환하지 않아 소비처에서 일반 문구로
 * 내려앉는다. 서버 `message` 는 판정에 쓰지 않는다(BE#92).
 *
 * 계약 — 발급(로그인) 응답의 `expiresIn` 은 기록하고, 생략(null)이면 이전 토큰 기준 stale
 * deadline 이 새 세션에 적용되지 않게 비우며(`TokenReissuer` 회전 경로와 같은 규칙),
 * 세션 종료(`logout`/`clearSession`)는 SESSION 스코프 저장소 일괄 정리(#912)와 deadline
 * 정리를 함께 수행한다.
 *
 * [AccessTokenExpiryTracker] 는 실물 사용 — 시계(SystemClock)는 `isReturnDefaultValues` 로
 * 0 에 고정되므로, 잔여 30초 기록 = 임박 true / 비움 = false 로 상태를 관찰한다 (임계 60초).
 */
class AuthRepositoryImplTest {
    private val tracker = AccessTokenExpiryTracker()
    private val tokenStore = InMemoryPreferencesDataStore()
    private val tokenDataSource = TokenDataSource(tokenStore)

    // 실제 레지스트리처럼 SESSION 정리가 토큰 저장소를 비우게 흉내 낸다 — 그래야 아래
    // 토큰 잔존·서버 호출 순서(정리가 먼저면 refresh 가 없어 서버 호출 불가) 단언이 유효하다.
    private val localStoreRegistry =
        FakeLocalStoreRegistry(
            onClearScope = { scope ->
                if (scope == StoreScope.SESSION) tokenStore.updateData { emptyPreferences() }
            },
        )

    private fun repository(
        authApiService: AuthApiService = FakeAuthApiService(),
        tokenApiService: TokenApiService = FakeTokenApiService(),
        pushTargetRepository: FakePushTargetRepository = FakePushTargetRepository(),
        deviceTargetId: String? = "device-token",
        devicePushTargetProvider: DevicePushTargetProvider = RecordingDevicePushTargetProvider(deviceTargetId),
    ) = AuthRepositoryImpl(
        tokenDataSource = tokenDataSource,
        authApiService = authApiService,
        tokenApiService = tokenApiService,
        expiryTracker = tracker,
        localStoreRegistry = localStoreRegistry,
        pushTargetRepository = pushTargetRepository,
        devicePushTargetProvider = devicePushTargetProvider,
    )

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
                    onSocialLogin = { success(LoginDto.SocialLoginDto("access", "refresh", isNewUser = false)) },
                ),
            )

        val result = runBlocking { repository.kakaoLogin("oauth-token") }

        assertTrue(result.isSuccess)
        assertFalse(tracker.isExpiringSoon())
    }

    @Test
    fun `logout - 저장돼 있던 refresh 로 서버 호출 후 SESSION 스코프·deadline 정리`() {
        runBlocking { tokenDataSource.saveTokens(accessToken = "access", refreshToken = "stored-refresh") }
        tracker.record(expiresInSeconds = 30)
        val authApiService = FakeAuthApiService()

        val result = runBlocking { repository(authApiService).logout() }

        assertTrue(result.isSuccess)
        // 정리가 서버 호출보다 먼저였다면 refresh 가 이미 없어 이 요청 자체가 못 나간다 (순서 가드).
        assertEquals(listOf(LogoutRequestDto("stored-refresh")), authApiService.logoutRequests)
        assertEquals(listOf(StoreScope.SESSION), localStoreRegistry.clearedScopes)
        assertNull(runBlocking { tokenDataSource.getRefreshToken() })
        assertFalse(tracker.isExpiringSoon())
    }

    @Test
    fun `logout - 서버 호출 실패해도 (best-effort) SESSION 스코프·deadline 정리는 진행`() {
        runBlocking { tokenDataSource.saveTokens(accessToken = "access", refreshToken = "stored-refresh") }
        tracker.record(expiresInSeconds = 30)
        val authApiService =
            FakeAuthApiService(
                onLogout = {
                    throw ApiException(status = 500, code = 500, serverMessage = null, fallbackMessage = "서버 오류")
                },
            )

        val result = runBlocking { repository(authApiService).logout() }

        assertTrue(result.isSuccess)
        assertEquals(listOf(StoreScope.SESSION), localStoreRegistry.clearedScopes)
        assertNull(runBlocking { tokenDataSource.getRefreshToken() })
        assertFalse(tracker.isExpiringSoon())
    }

    @Test
    fun `clearSession - SESSION 스코프·deadline 함께 정리 (탈퇴 경로도 이 메서드를 쓴다)`() {
        runBlocking { tokenDataSource.saveTokens(accessToken = "access", refreshToken = "refresh") }
        tracker.record(expiresInSeconds = 30)

        val result = runBlocking { repository().clearSession() }

        assertTrue(result.isSuccess)
        assertEquals(listOf(StoreScope.SESSION), localStoreRegistry.clearedScopes)
        assertNull(runBlocking { tokenDataSource.getAccessToken() })
        assertFalse(tracker.isExpiringSoon())
    }

    @Test
    fun `rotateToken - 빈 액세스 토큰 응답은 기존 토큰을 덮어쓰지 않고 실패`() {
        runBlocking { tokenDataSource.saveTokens(accessToken = "old-access", refreshToken = "old-refresh") }
        val repository =
            repository(
                tokenApiService =
                    FakeTokenApiService {
                        success(ReissueDto(accessToken = "", refreshToken = "new-refresh"))
                    },
            )

        val result = runBlocking { repository.rotateToken() }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals("old-access", runBlocking { tokenDataSource.getAccessToken() })
        assertEquals("old-refresh", runBlocking { tokenDataSource.getRefreshToken() })
    }

    @Test
    fun `defaultLogin - 전송 계층 IO 실패는 NetworkUnavailable 로 치환 (원인 보존)`() {
        val repository =
            repository(
                FakeAuthApiService(
                    onLogin = { throw UnknownHostException("Unable to resolve host") },
                ),
            )

        val result = runBlocking { repository.defaultLogin("user@example.com", "pw") }

        val exception = result.exceptionOrNull()
        assertTrue(exception is CoreAuthFailure.NetworkUnavailable)
        assertTrue(exception?.cause is UnknownHostException)
    }

    @Test
    fun `defaultLogin - 자격 거절(1202)은 InvalidLoginCredentials 로 치환 (원인 보존)`() {
        val repository =
            repository(
                FakeAuthApiService(
                    onLogin = {
                        throw ApiException(
                            status = 401,
                            code = 1202,
                            serverMessage = "서버 문구",
                            fallbackMessage = "서버 문구",
                        )
                    },
                ),
            )

        val result = runBlocking { repository.defaultLogin("user@example.com", "pw") }

        val exception = result.exceptionOrNull()
        assertTrue(exception is CoreAuthFailure.InvalidLoginCredentials)
        assertTrue(exception?.cause is ApiException)
    }

    @Test
    fun `defaultLogin - 소셜 가입 계정(1702)은 SocialSignUpAccount 로 치환 (자격 거절과 가름)`() {
        val repository =
            repository(
                FakeAuthApiService(
                    onLogin = {
                        throw ApiException(
                            status = 400,
                            code = 1702,
                            serverMessage = "소셜 로그인으로 가입한 계정입니다. 소셜 로그인을 이용해주세요.",
                            fallbackMessage = "소셜 로그인으로 가입한 계정입니다. 소셜 로그인을 이용해주세요.",
                        )
                    },
                ),
            )

        val result = runBlocking { repository.defaultLogin("social@example.com", "pw") }

        val exception = result.exceptionOrNull()
        assertTrue(exception is CoreAuthFailure.SocialSignUpAccount)
        assertTrue(exception?.cause is ApiException)
    }

    @Test
    fun `defaultLogin - allowlist 밖 코드는 치환하지 않음 (5xx 내부 문구가 표시 경로로 못 감)`() {
        val internalMessage = "ERROR: duplicate key value violates unique constraint"
        val repository =
            repository(
                FakeAuthApiService(
                    onLogin = {
                        throw ApiException(
                            status = 500,
                            code = 1904,
                            serverMessage = internalMessage,
                            fallbackMessage = internalMessage,
                        )
                    },
                ),
            )

        val result = runBlocking { repository.defaultLogin("user@example.com", "pw") }

        // 치환되지 않으므로 소비처(LoginViewModel)의 else 갈래 = 일반 문구로 내려앉는다.
        assertTrue(result.exceptionOrNull() is ApiException)
    }

    @Test
    fun `defaultLogin - 서버 message 가 없어도 code 만으로 치환 (message 비의존)`() {
        val repository =
            repository(
                FakeAuthApiService(
                    onLogin = {
                        throw ApiException(
                            status = 401,
                            code = 1201,
                            serverMessage = null,
                            fallbackMessage = "클라 폴백 문구",
                        )
                    },
                ),
            )

        val result = runBlocking { repository.defaultLogin("user@example.com", "pw") }

        assertTrue(result.exceptionOrNull() is CoreAuthFailure.InvalidLoginCredentials)
    }

    @Test
    fun `socialLogin - 소셜 거절(1208)은 SocialLoginRejected 로 치환`() {
        val repository =
            repository(
                FakeAuthApiService(
                    onSocialLogin = {
                        throw ApiException(
                            status = 400,
                            code = 1208,
                            serverMessage = "소셜 로그인에 실패했습니다.",
                            fallbackMessage = "소셜 로그인에 실패했습니다.",
                        )
                    },
                ),
            )

        val result = runBlocking { repository.kakaoLogin("oauth-token") }

        assertTrue(result.exceptionOrNull() is CoreAuthFailure.SocialLoginRejected)
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

        assertTrue(result.exceptionOrNull() is CoreAuthFailure.NetworkUnavailable)
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

        assertTrue(result.exceptionOrNull() is CoreAuthFailure.NetworkUnavailable)
    }

    @Test
    fun `logout - 이 기기 푸시 대상 식별자를 해제한다`() {
        val pushTargetRepository = FakePushTargetRepository()
        val repository = repository(pushTargetRepository = pushTargetRepository, deviceTargetId = "device-token")

        val result = runBlocking { repository.logout() }

        assertTrue(result.isSuccess)
        assertEquals(listOf("device-token"), pushTargetRepository.unregistered)
    }

    @Test
    fun `logout - 해제하려고 FCM 등록 시퀀스를 강제하지는 않는다`() {
        // currentTargetId() 을 쓰면 지우기 직전에 기기를 FCM 에 다시 등록하고, 그 회전 통보가 아직
        // 살아 있는 세션을 타고 재등록으로 돌아와 이 DELETE 와 경합한다 (#1498 리뷰).
        val provider = RecordingDevicePushTargetProvider("device-token")
        val pushTargetRepository = FakePushTargetRepository()
        val repository = repository(pushTargetRepository = pushTargetRepository, devicePushTargetProvider = provider)

        runBlocking { repository.logout() }

        assertEquals(listOf("device-token"), pushTargetRepository.unregistered)
        assertEquals(0, provider.currentTargetIdCalls)
        assertEquals(1, provider.existingTargetIdCalls)
    }

    @Test
    fun `logout - 기기 식별자 조회가 던져도 로그아웃은 성공한다`() {
        val repository = repository(devicePushTargetProvider = ThrowingDevicePushTargetProvider())

        val result = runBlocking { repository.logout() }

        assertTrue(result.isSuccess)
    }

    @Test
    fun `logout - 기기 식별자를 못 얻으면 해제를 건너뛴다`() {
        val pushTargetRepository = FakePushTargetRepository()
        val repository = repository(pushTargetRepository = pushTargetRepository, deviceTargetId = null)

        val result = runBlocking { repository.logout() }

        assertTrue(result.isSuccess)
        assertTrue(pushTargetRepository.unregistered.isEmpty())
    }

    @Test
    fun `logout - 푸시 대상 해제가 실패해도 로그아웃은 끝난다`() {
        val pushTargetRepository = FakePushTargetRepository(failing = true)
        val repository = repository(pushTargetRepository = pushTargetRepository, deviceTargetId = "device-token")

        val result = runBlocking { repository.logout() }

        assertTrue(result.isSuccess)
        assertNull(runBlocking { tokenDataSource.getAccessToken() })
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
    private val onLogout: () -> BaseResponse<Unit> = { success(Unit) },
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
    private val onReissue: (ReissueRequestDto) -> BaseResponse<ReissueDto> = {
        error("reissue 는 이 시나리오에서 호출되면 안 됨")
    },
) : TokenApiService {
    override suspend fun reissue(body: ReissueRequestDto): BaseResponse<ReissueDto> = onReissue(body)
}

/** [LocalStoreRegistry] 테스트 대역 — clearScope 호출을 기록하고, 주입된 동작으로 실제 정리를 흉내 낸다. */
private class FakeLocalStoreRegistry(
    private val onClearScope: suspend (StoreScope) -> Unit = {},
) : LocalStoreRegistry {
    val clearedScopes = mutableListOf<StoreScope>()

    override fun store(
        name: String,
        scope: StoreScope,
    ): DataStore<Preferences> = error("store 는 이 테스트에서 호출되면 안 됨")

    override suspend fun clearScope(scope: StoreScope) {
        clearedScopes += scope
        onClearScope(scope)
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

private class RecordingDevicePushTargetProvider(
    private val targetId: String?,
) : DevicePushTargetProvider {
    var currentTargetIdCalls = 0
    var existingTargetIdCalls = 0

    override suspend fun currentTargetId(): String? {
        currentTargetIdCalls++
        return targetId
    }

    override suspend fun existingTargetId(): String? {
        existingTargetIdCalls++
        return targetId
    }
}

private class ThrowingDevicePushTargetProvider : DevicePushTargetProvider {
    override suspend fun currentTargetId(): String? = throw IllegalStateException("API disabled")

    override suspend fun existingTargetId(): String? = throw IllegalStateException("API disabled")
}

private class FakePushTargetRepository(
    private val failing: Boolean = false,
) : PushTargetRepository {
    val unregistered = mutableListOf<String>()

    override suspend fun register(targetId: String): Result<Unit> =
        if (failing) Result.failure(IllegalStateException("등록 실패")) else Result.success(Unit)

    override suspend fun unregister(targetId: String): Result<Unit> {
        unregistered += targetId
        return if (failing) Result.failure(IllegalStateException("해제 실패")) else Result.success(Unit)
    }
}
