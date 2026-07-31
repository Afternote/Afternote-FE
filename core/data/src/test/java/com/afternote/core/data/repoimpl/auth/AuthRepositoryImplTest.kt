package com.afternote.core.data.repoimpl.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.afternote.core.datastore.TokenDataSource
import com.afternote.core.domain.error.NetworkUnavailableException
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

/**
 * [AuthRepositoryImpl] 의 선제 reissue deadline 관리 계약 회귀 가드 (#408, PR #411 리뷰 반영).
 * 로그인 경로의 전송 계층 IO 실패 → [NetworkUnavailableException] 치환 계약(#517)도 함께 가드한다.
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

    private fun repository(authApiService: AuthApiService = FakeAuthApiService()) =
        AuthRepositoryImpl(
            tokenDataSource = tokenDataSource,
            authApiService = authApiService,
            tokenApiService = FakeTokenApiService(),
            expiryTracker = tracker,
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
    }

    @Test
    fun `logout - 서버 호출 실패해도 (best-effort) 토큰·deadline 정리는 진행`() {
        runBlocking { tokenDataSource.saveTokens(accessToken = "access", refreshToken = "stored-refresh") }
        tracker.record(expiresInSeconds = 30)
        val authApiService =
            FakeAuthApiService(
                onLogout = { throw ApiException(code = 500, serverMessage = null, message = "서버 오류") },
            )

        val result = runBlocking { repository(authApiService).logout() }

        assertTrue(result.isSuccess)
        assertNull(runBlocking { tokenDataSource.getRefreshToken() })
        assertFalse(tracker.isExpiringSoon())
    }

    @Test
    fun `clearSession - 로컬 토큰·deadline 함께 정리`() {
        runBlocking { tokenDataSource.saveTokens(accessToken = "access", refreshToken = "refresh") }
        tracker.record(expiresInSeconds = 30)

        val result = runBlocking { repository().clearSession() }

        assertTrue(result.isSuccess)
        assertNull(runBlocking { tokenDataSource.getAccessToken() })
        assertFalse(tracker.isExpiringSoon())
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
    fun `defaultLogin - 서버 응답 실패(ApiException)는 치환 없이 통과 (서버 message 보존)`() {
        val serverMessage = "비밀번호가 일치하지 않습니다."
        val repository =
            repository(
                FakeAuthApiService(
                    onLogin = { throw ApiException(code = 401, serverMessage = serverMessage, message = serverMessage) },
                ),
            )

        val result = runBlocking { repository.defaultLogin("user@example.com", "pw") }

        val exception = result.exceptionOrNull()
        assertTrue(exception is ApiException)
        assertEquals(serverMessage, exception?.message)
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

private class FakeTokenApiService : TokenApiService {
    override suspend fun reissue(body: ReissueRequestDto): BaseResponse<ReissueDto> = error("reissue 는 이 시나리오에서 호출되면 안 됨")
}

/** 단위 테스트용 in-memory `DataStore<Preferences>` — 디스크 없이 [TokenDataSource] 실물을 구동한다. */
private class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow<Preferences>(emptyPreferences())

    override val data: Flow<Preferences> get() = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val transformed = transform(state.value)
        state.value = transformed
        return transformed
    }
}
