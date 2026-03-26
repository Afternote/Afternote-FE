package com.afternote.core.domain.repository

import com.afternote.core.model.LoginResult
import com.afternote.core.model.ReissueResult
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

    suspend fun login(
        email: String,
        password: String,
    ): Result<LoginResult>

    suspend fun kakaoLogin(): Result<LoginResult>

    suspend fun reissue(refreshToken: String): Result<ReissueResult>

    suspend fun logout(refreshToken: String): Result<Unit>
}
