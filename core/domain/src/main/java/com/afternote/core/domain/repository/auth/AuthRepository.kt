package com.afternote.core.domain.repository.auth

import com.afternote.core.model.Login
import com.afternote.core.model.RotateToken
import com.afternote.core.model.SocialLogin
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
    // 레거시 레포 기준

    suspend fun login(
        email: String,
        password: String,
    ): Result<Login>

    suspend fun kakaoLogin(): Result<SocialLogin>

    suspend fun rotateToken(refreshToken: String): Result<RotateToken>

    suspend fun logout(refreshToken: String): Result<Unit>
}
