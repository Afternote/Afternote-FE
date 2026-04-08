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
    // 레거시 레포 기준

    suspend fun defaultLogin(
        email: String,
        password: String,
    ): Result<Session.DefaultSession>

    suspend fun kakaoLogin(): Result<Session.SocialSession>

    suspend fun rotateToken(refreshToken: String): Result<TokenBundle>

    suspend fun logout(refreshToken: String): Result<Unit>
}
