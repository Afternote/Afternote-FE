package com.afternote.core.domain.usecase.auth

import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.model.Session
import com.afternote.core.model.TokenBundle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [LoginUseCase] 비즈니스 로직 회귀 가드.
 *
 * 검증 핵심:
 * 1. 로그인 타입(Email/Kakao/Google)에 맞는 [AuthRepository] 메서드로 분기하는지
 * 2. 로그인 성공 시 받은 세션 토큰을 그대로 [AuthRepository.saveSession]에 전달하는지
 * 3. 로그인 실패면 **saveSession을 호출하지 않고** 그 실패를 그대로 반환(short-circuit)하는지
 * 4. saveSession 실패면 그 실패를 반환하는지
 * 5. 반환값(신규 가입자 여부) — 소셜은 서버 `isNewUser` 그대로, 이메일 로그인은 항상 false
 *
 * 외부 라이브러리(mockk 등) 없이 호출 인자/횟수를 기록하는 직접 작성 fake를 사용한다.
 */
class LoginUseCaseTest {
    @Test
    fun `Email 로그인 성공 - defaultLogin 호출 후 세션 토큰으로 saveSession`() {
        val repo =
            FakeAuthRepository().apply {
                defaultLoginResult = Result.success(Session.DefaultSession(accessToken = "AT", refreshToken = "RT"))
            }
        val result = runBlocking { LoginUseCase(repo)(LoginType.Email(email = "a@b.com", password = "pw")) }

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow()) // 이메일 로그인 = 기존 유저(false)
        assertEquals("a@b.com" to "pw", repo.defaultLoginArgs)
        assertEquals("AT" to "RT", repo.saveSessionArgs)
        assertEquals(1, repo.saveSessionCallCount)
    }

    @Test
    fun `Kakao 로그인 성공 - kakaoLogin 호출 + saveSession`() {
        val repo =
            FakeAuthRepository().apply {
                kakaoResult =
                    Result.success(Session.SocialSession(accessToken = "KAT", refreshToken = "KRT", isNewUser = true))
            }
        val result = runBlocking { LoginUseCase(repo)(LoginType.Kakao(oauthToken = "kakao-token")) }

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow()) // isNewUser=true → 신규
        assertEquals("kakao-token", repo.kakaoArg)
        assertEquals("KAT" to "KRT", repo.saveSessionArgs)
    }

    @Test
    fun `Google 로그인 성공 - googleLogin 호출 + saveSession`() {
        val repo =
            FakeAuthRepository().apply {
                googleResult =
                    Result.success(Session.SocialSession(accessToken = "GAT", refreshToken = "GRT", isNewUser = false))
            }
        val result = runBlocking { LoginUseCase(repo)(LoginType.Google(idToken = "google-id-token")) }

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow()) // isNewUser=false → 기존
        assertEquals("google-id-token", repo.googleArg)
        assertEquals("GAT" to "GRT", repo.saveSessionArgs)
    }

    @Test
    fun `로그인 실패면 saveSession 호출하지 않고 실패를 그대로 반환`() {
        val loginError = IllegalStateException("login failed")
        val repo =
            FakeAuthRepository().apply {
                defaultLoginResult = Result.failure(loginError)
            }
        val result = runBlocking { LoginUseCase(repo)(LoginType.Email(email = "a@b.com", password = "pw")) }

        assertTrue(result.isFailure)
        assertSame(loginError, result.exceptionOrNull())
        assertEquals(0, repo.saveSessionCallCount)
        assertNull(repo.saveSessionArgs)
    }

    @Test
    fun `saveSession 실패면 그 실패를 반환`() {
        val saveError = IllegalStateException("save failed")
        val repo =
            FakeAuthRepository().apply {
                defaultLoginResult = Result.success(Session.DefaultSession(accessToken = "AT", refreshToken = "RT"))
                saveSessionResult = Result.failure(saveError)
            }
        val result = runBlocking { LoginUseCase(repo)(LoginType.Email(email = "a@b.com", password = "pw")) }

        assertFalse(result.isSuccess)
        assertSame(saveError, result.exceptionOrNull())
        assertEquals(1, repo.saveSessionCallCount)
    }

    private class FakeAuthRepository : AuthRepository {
        var defaultLoginArgs: Pair<String, String>? = null
        var kakaoArg: String? = null
        var googleArg: String? = null
        var saveSessionArgs: Pair<String, String>? = null
        var saveSessionCallCount = 0

        var defaultLoginResult: Result<Session.DefaultSession> =
            Result.success(Session.DefaultSession(accessToken = "at", refreshToken = "rt"))
        var kakaoResult: Result<Session.SocialSession> =
            Result.success(Session.SocialSession(accessToken = "at", refreshToken = "rt", isNewUser = false))
        var googleResult: Result<Session.SocialSession> =
            Result.success(Session.SocialSession(accessToken = "at", refreshToken = "rt", isNewUser = false))
        var saveSessionResult: Result<Unit> = Result.success(Unit)

        override val isLoggedIn: Flow<Boolean> = flowOf(false)

        override suspend fun saveSession(
            accessToken: String,
            refreshToken: String,
        ): Result<Unit> {
            saveSessionCallCount++
            saveSessionArgs = accessToken to refreshToken
            return saveSessionResult
        }

        override suspend fun updateTokens(
            accessToken: String,
            refreshToken: String,
        ): Result<Unit> = Result.success(Unit)

        override suspend fun clearSession(): Result<Unit> = Result.success(Unit)

        override suspend fun getAccessToken(): Result<String?> = Result.success(null)

        override suspend fun getRefreshToken(): Result<String?> = Result.success(null)

        override suspend fun defaultLogin(
            email: String,
            password: String,
        ): Result<Session.DefaultSession> {
            defaultLoginArgs = email to password
            return defaultLoginResult
        }

        override suspend fun kakaoLogin(oauthToken: String): Result<Session.SocialSession> {
            kakaoArg = oauthToken
            return kakaoResult
        }

        override suspend fun googleLogin(idToken: String): Result<Session.SocialSession> {
            googleArg = idToken
            return googleResult
        }

        override suspend fun rotateToken(): Result<TokenBundle> = Result.success(TokenBundle(accessToken = "at", refreshToken = "rt"))

        override suspend fun logout(): Result<Unit> = Result.success(Unit)
    }
}
