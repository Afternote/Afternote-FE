package com.afternote.core.network

import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.model.Session
import com.afternote.core.model.TokenBundle
import kotlinx.coroutines.flow.Flow

/**
 * 토큰 갱신 경로(#408) 테스트 공용 가짜.
 * 미사용 멤버는 error — 의도치 않은 호출(특히 best-effort 경로의 clearSession)을 테스트 실패로 드러낸다.
 * [accessToken] 이 var 인 이유: rotate 중 저장 토큰 교체(in-flight stale 응답 가드) 시나리오 재현용.
 */
internal class FakeAuthRepository(
    var accessToken: String? = null,
    private val onRotateToken: FakeAuthRepository.() -> Result<TokenBundle> = {
        error("rotateToken 은 이 시나리오에서 호출되면 안 됨")
    },
    private val onClearSession: () -> Result<Unit> = {
        error("clearSession 은 이 시나리오에서 호출되면 안 됨")
    },
) : AuthRepository {
    var rotateCallCount = 0
        private set

    var clearSessionCallCount = 0
        private set

    override suspend fun getAccessToken(): Result<String?> = Result.success(accessToken)

    override suspend fun rotateToken(): Result<TokenBundle> {
        rotateCallCount++
        return onRotateToken()
    }

    override suspend fun clearSession(): Result<Unit> {
        clearSessionCallCount++
        return onClearSession()
    }

    override val isLoggedIn: Flow<Boolean>
        get() = error("not used")

    override suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
    ): Result<Unit> = error("not used")

    override suspend fun updateTokens(
        accessToken: String,
        refreshToken: String,
    ): Result<Unit> = error("not used")

    override suspend fun getRefreshToken(): Result<String?> = error("not used")

    override suspend fun defaultLogin(
        email: String,
        password: String,
    ): Result<Session.DefaultSession> = error("not used")

    override suspend fun kakaoLogin(oauthToken: String): Result<Session.SocialSession> = error("not used")

    override suspend fun googleLogin(idToken: String): Result<Session.SocialSession> = error("not used")

    override suspend fun logout(): Result<Unit> = error("not used")
}
