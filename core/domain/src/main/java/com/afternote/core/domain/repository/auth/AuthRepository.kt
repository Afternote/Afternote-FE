package com.afternote.core.domain.repository.auth

import com.afternote.core.model.Session
import com.afternote.core.model.TokenBundle
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: Long,
    ): Result<Unit>

    suspend fun updateTokens(
        accessToken: String,
        refreshToken: String,
    ): Result<Unit>

    suspend fun clearSession(): Result<Unit>

    suspend fun getAccessToken(): Result<String?>

    suspend fun getRefreshToken(): Result<String?>

    suspend fun getUserId(): Result<Long?>

    suspend fun defaultLogin(
        email: String,
        password: String,
    ): Result<Session.DefaultSession>

    suspend fun kakaoLogin(oauthToken: String): Result<Session.SocialSession>

    suspend fun googleLogin(idToken: String): Result<Session.SocialSession>

    suspend fun rotateToken(): Result<TokenBundle>

    suspend fun logout(): Result<Unit>
}
