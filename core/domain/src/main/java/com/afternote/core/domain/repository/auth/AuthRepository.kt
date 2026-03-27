package com.afternote.core.domain.repository.auth

import com.afternote.core.model.LoginResult
import com.afternote.core.model.RotateTokenResult
import com.afternote.core.model.SocialLoginResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: Long,
    )

    suspend fun updateTokens(
        accessToken: String,
        refreshToken: String,
    )

    suspend fun clearSession()

    suspend fun getAccessToken(): String?

    suspend fun getRefreshToken(): String?

    suspend fun getUserId(): Long?
    // 레거시 레포 기준

    suspend fun login(
        email: String,
        password: String,
    ): Result<LoginResult>

    suspend fun kakaoLogin(): Result<SocialLoginResult>

    suspend fun rotateToken(refreshToken: String): Result<RotateTokenResult>

    suspend fun logout(refreshToken: String): Result<Unit>
}
