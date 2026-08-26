package com.afternote.core.network

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.model.Session
import com.afternote.core.model.TokenBundle
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.atomic.AtomicInteger

/**
 * 토큰 갱신 경로(#408) 테스트 공용 가짜.
 * 미사용 멤버는 error — 의도치 않은 호출(특히 best-effort 경로의 clearSession)을 테스트 실패로 드러낸다.
 * [accessToken] 이 var 이고 두 훅이 리시버를 받는 이유: rotate 중 저장 토큰 교체(in-flight stale
 * 응답 가드)와 clearSession 이 토큰을 지운 뒤의 재진입(#1126) 시나리오를 재현하기 위함이다.
 */
internal class FakeAuthRepository(
    var accessToken: String? = null,
    private val onRotateToken: FakeAuthRepository.() -> Result<TokenBundle> = {
        error("rotateToken 은 이 시나리오에서 호출되면 안 됨")
    },
    private val onClearSession: FakeAuthRepository.() -> Result<Unit> = {
        error("clearSession 은 이 시나리오에서 호출되면 안 됨")
    },
) : AuthRepository {
    // 동시 진입 시나리오(#1126)에서도 세지므로 원자 카운터여야 한다.
    private val rotateCalls = AtomicInteger()
    private val clearSessionCalls = AtomicInteger()

    val rotateCallCount: Int get() = rotateCalls.get()

    val clearSessionCallCount: Int get() = clearSessionCalls.get()

    override suspend fun getAccessToken(): Result<String?> = Result.success(accessToken)

    override suspend fun rotateToken(): Result<TokenBundle> {
        rotateCalls.incrementAndGet()
        return onRotateToken()
    }

    override suspend fun clearSession(): Result<Unit> {
        clearSessionCalls.incrementAndGet()
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

internal class FakeErrorReporter : ErrorReporter {
    val writtenFailures = mutableListOf<Pair<Throwable, Map<String, String>>>()

    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) {
        writtenFailures += throwable to attributes
    }
}
