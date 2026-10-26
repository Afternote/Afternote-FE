package com.afternote.core.domain.repository.auth

import com.afternote.core.model.Session
import com.afternote.core.model.TokenBundle
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
    ): Result<Unit>

    suspend fun updateTokens(
        accessToken: String,
        refreshToken: String,
    ): Result<Unit>

    suspend fun clearSession(): Result<Unit>

    /**
     * 지금 세션이 [sessionId] 일 때만 [clearSession] 과 같은 정리를 하고, 실제로 정리했는지 돌려준다.
     * 재발급 거절처럼 특정 세션에 대한 판정을 적용할 때 쓴다. 그사이 로그인한 새 세션은 지우지 않는다 (#2238).
     */
    suspend fun clearSessionIfCurrent(sessionId: String): Result<Boolean>

    /** 지금 저장된 로그인 세션의 식별자. 세션이 없으면 `null`. 로그인할 때마다 새로 발급된다. */
    suspend fun getSessionId(): Result<String?>

    suspend fun getAccessToken(): Result<String?>

    suspend fun getRefreshToken(): Result<String?>

    suspend fun defaultLogin(
        email: String,
        password: String,
    ): Result<Session.DefaultSession>

    suspend fun kakaoLogin(oauthToken: String): Result<Session.SocialSession>

    suspend fun googleLogin(idToken: String): Result<Session.SocialSession>

    /**
     * [sessionId] 세션의 리프레시 토큰으로 재발급하고 그 세션에만 새 토큰을 저장한다 (#2237).
     *
     * 시작할 때나 저장할 때 세션이 [sessionId] 가 아니면 저장하지 않고
     * [com.afternote.core.domain.error.SessionChangedException] 으로 실패한다.
     */
    suspend fun rotateToken(sessionId: String): Result<TokenBundle>

    suspend fun logout(): Result<Unit>
}
