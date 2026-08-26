package com.afternote.core.domain.testing

import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.model.Session
import com.afternote.core.model.TokenBundle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * [AuthRepository] fake 정본 (#1030, #1041).
 *
 * 기본은 토큰과 로그인 상태를 메모리에 저장하며 모든 호출을 기록한다. 특정 실패, 경합,
 * 서버 응답은 `onX` 로 갈아끼운다. 호출 금지 경계가 필요한 테스트는 [strict] 로 시작해
 * 실제로 쓰는 경로만 연다.
 */
class FakeAuthRepository(
    loggedIn: Boolean = false,
    @Volatile var accessToken: String? = null,
    @Volatile var refreshToken: String? = null,
    var defaultSession: Session.DefaultSession = Session.DefaultSession(DEFAULT_ACCESS_TOKEN, DEFAULT_REFRESH_TOKEN),
    var kakaoSession: Session.SocialSession = Session.SocialSession(DEFAULT_ACCESS_TOKEN, DEFAULT_REFRESH_TOKEN, false),
    var googleSession: Session.SocialSession = Session.SocialSession(DEFAULT_ACCESS_TOKEN, DEFAULT_REFRESH_TOKEN, false),
    var rotatedTokens: TokenBundle = TokenBundle(DEFAULT_ACCESS_TOKEN, DEFAULT_REFRESH_TOKEN),
    var onIsLoggedIn: (() -> Flow<Boolean>)? = null,
    var onSaveSession: (suspend (String, String) -> Result<Unit>)? = null,
    var onUpdateTokens: (suspend (String, String) -> Result<Unit>)? = null,
    var onClearSession: (suspend () -> Result<Unit>)? = null,
    var onGetAccessToken: (suspend () -> Result<String?>)? = null,
    var onGetRefreshToken: (suspend () -> Result<String?>)? = null,
    var onDefaultLogin: (suspend (String, String) -> Result<Session.DefaultSession>)? = null,
    var onKakaoLogin: (suspend (String) -> Result<Session.SocialSession>)? = null,
    var onGoogleLogin: (suspend (String) -> Result<Session.SocialSession>)? = null,
    var onRotateToken: (suspend FakeAuthRepository.() -> Result<TokenBundle>)? = null,
    var onLogout: (suspend () -> Result<Unit>)? = null,
) : AuthRepository {
    val loggedInState = MutableStateFlow(loggedIn)

    private val isLoggedInCounter = AtomicInteger()

    var loggedIn: Boolean
        get() = loggedInState.value
        set(value) {
            loggedInState.value = value
        }

    override val isLoggedIn: Flow<Boolean>
        get() {
            isLoggedInCounter.incrementAndGet()
            return onIsLoggedIn?.invoke() ?: loggedInState
        }

    val savedSessions = CopyOnWriteArrayList<Pair<String, String>>()
    val updatedTokens = CopyOnWriteArrayList<Pair<String, String>>()
    val attemptedEmailLogins = CopyOnWriteArrayList<Pair<String, String>>()
    val attemptedKakaoLogins = CopyOnWriteArrayList<String>()
    val attemptedGoogleLogins = CopyOnWriteArrayList<String>()

    private val clearSessionCounter = AtomicInteger()
    private val accessTokenCounter = AtomicInteger()
    private val refreshTokenCounter = AtomicInteger()
    private val rotateTokenCounter = AtomicInteger()
    private val logoutCounter = AtomicInteger()

    val saveSessionCalls: Int get() = savedSessions.size
    val saveSessionCallCount: Int get() = savedSessions.size
    val saveSessionArgs: Pair<String, String>? get() = savedSessions.lastOrNull()
    val defaultLoginArgs: Pair<String, String>? get() = attemptedEmailLogins.lastOrNull()
    val kakaoArg: String? get() = attemptedKakaoLogins.lastOrNull()
    val googleArg: String? get() = attemptedGoogleLogins.lastOrNull()
    val isLoggedInCalls: Int get() = isLoggedInCounter.get()
    val clearSessionCalls: Int get() = clearSessionCounter.get()
    val clearSessionCallCount: Int get() = clearSessionCounter.get()
    val getAccessTokenCalls: Int get() = accessTokenCounter.get()
    val getRefreshTokenCalls: Int get() = refreshTokenCounter.get()
    val rotateTokenCalls: Int get() = rotateTokenCounter.get()
    val rotateCallCount: Int get() = rotateTokenCounter.get()
    val logoutCalls: Int get() = logoutCounter.get()

    override suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
    ): Result<Unit> {
        savedSessions += accessToken to refreshToken
        onSaveSession?.let { return it(accessToken, refreshToken) }
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        loggedIn = true
        return Result.success(Unit)
    }

    override suspend fun updateTokens(
        accessToken: String,
        refreshToken: String,
    ): Result<Unit> {
        updatedTokens += accessToken to refreshToken
        onUpdateTokens?.let { return it(accessToken, refreshToken) }
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        loggedIn = true
        return Result.success(Unit)
    }

    override suspend fun clearSession(): Result<Unit> {
        clearSessionCounter.incrementAndGet()
        onClearSession?.let { return it() }
        accessToken = null
        refreshToken = null
        loggedIn = false
        return Result.success(Unit)
    }

    override suspend fun getAccessToken(): Result<String?> {
        accessTokenCounter.incrementAndGet()
        onGetAccessToken?.let { return it() }
        return Result.success(accessToken)
    }

    override suspend fun getRefreshToken(): Result<String?> {
        refreshTokenCounter.incrementAndGet()
        onGetRefreshToken?.let { return it() }
        return Result.success(refreshToken)
    }

    override suspend fun defaultLogin(
        email: String,
        password: String,
    ): Result<Session.DefaultSession> {
        attemptedEmailLogins += email to password
        onDefaultLogin?.let { return it(email, password) }
        return Result.success(defaultSession)
    }

    override suspend fun kakaoLogin(oauthToken: String): Result<Session.SocialSession> {
        attemptedKakaoLogins += oauthToken
        onKakaoLogin?.let { return it(oauthToken) }
        return Result.success(kakaoSession)
    }

    override suspend fun googleLogin(idToken: String): Result<Session.SocialSession> {
        attemptedGoogleLogins += idToken
        onGoogleLogin?.let { return it(idToken) }
        return Result.success(googleSession)
    }

    override suspend fun rotateToken(): Result<TokenBundle> {
        rotateTokenCounter.incrementAndGet()
        onRotateToken?.let { return it(this) }
        if (refreshToken == null) {
            return Result.failure(IllegalStateException("리프레시 토큰이 존재하지 않습니다."))
        }
        if (rotatedTokens.accessToken.isEmpty()) {
            return Result.failure(IllegalStateException("Token rotation returned an empty access token"))
        }
        accessToken = rotatedTokens.accessToken
        refreshToken = rotatedTokens.refreshToken
        loggedIn = true
        return Result.success(rotatedTokens)
    }

    override suspend fun logout(): Result<Unit> {
        logoutCounter.incrementAndGet()
        onLogout?.let { return it() }
        accessToken = null
        refreshToken = null
        loggedIn = false
        return Result.success(Unit)
    }

    companion object {
        private const val DEFAULT_ACCESS_TOKEN = "access"
        private const val DEFAULT_REFRESH_TOKEN = "refresh"

        /** 모든 경로를 닫고, 테스트가 쓰는 `onX` 만 명시적으로 연다. */
        fun strict(
            loggedIn: Boolean = false,
            accessToken: String? = null,
            refreshToken: String? = null,
        ): FakeAuthRepository =
            FakeAuthRepository(
                loggedIn = loggedIn,
                accessToken = accessToken,
                refreshToken = refreshToken,
                onIsLoggedIn = { unexpectedCall("AuthRepository.isLoggedIn") },
                onSaveSession = { _, _ -> unexpectedCall("AuthRepository.saveSession") },
                onUpdateTokens = { _, _ -> unexpectedCall("AuthRepository.updateTokens") },
                onClearSession = { unexpectedCall("AuthRepository.clearSession") },
                onGetAccessToken = { unexpectedCall("AuthRepository.getAccessToken") },
                onGetRefreshToken = { unexpectedCall("AuthRepository.getRefreshToken") },
                onDefaultLogin = { _, _ -> unexpectedCall("AuthRepository.defaultLogin") },
                onKakaoLogin = { unexpectedCall("AuthRepository.kakaoLogin") },
                onGoogleLogin = { unexpectedCall("AuthRepository.googleLogin") },
                onRotateToken = { unexpectedCall("AuthRepository.rotateToken") },
                onLogout = { unexpectedCall("AuthRepository.logout") },
            )
    }
}
