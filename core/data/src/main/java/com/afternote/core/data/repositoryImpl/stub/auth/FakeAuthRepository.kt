package com.afternote.core.data.repositoryImpl.stub.auth

import com.afternote.core.datastore.TokenManager
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.model.Session
import com.afternote.core.model.TokenBundle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeAuthRepository
    @Inject
    constructor(
        private val tokenManager: TokenManager,
    ) : AuthRepository {
        override val isLoggedIn: Flow<Boolean> = tokenManager.isLoggedInFlow

        override suspend fun saveSession(
            accessToken: String,
            refreshToken: String,
            userId: Long,
        ) = runCatching { tokenManager.saveTokens(accessToken, refreshToken, userId) }

        override suspend fun updateTokens(
            accessToken: String,
            refreshToken: String,
        ) = runCatching { tokenManager.updateTokens(accessToken, refreshToken) }

        override suspend fun clearSession() = runCatching { tokenManager.clearTokens() }

        override suspend fun getAccessToken() = runCatching { tokenManager.getAccessToken() }

        override suspend fun getRefreshToken() = runCatching { tokenManager.getRefreshToken() }

        override suspend fun defaultLogin(
            email: String,
            password: String,
        ): Result<Session.DefaultSession> {
            delay(1000)
            return Result.success(
                Session.DefaultSession(
                    accessToken = "fake_access_token",
                    refreshToken = "fake_refresh_token",
                    userId = 1L,
                ),
            )
        }

        override suspend fun kakaoLogin(): Result<Session.SocialSession> {
            delay(1000)
            return Result.success(
                Session.SocialSession(
                    accessToken = "fake_kakao_access",
                    refreshToken = "fake_kakao_refresh",
                    userId = 1L,
                    isNewUser = false,
                ),
            )
        }

        override suspend fun rotateToken(refreshToken: String): Result<TokenBundle> =
            Result.success(TokenBundle("fake_new_access", "fake_new_refresh"))

        override suspend fun logout(refreshToken: String): Result<Unit> {
            tokenManager.clearTokens()
            return Result.success(Unit)
        }
    }
