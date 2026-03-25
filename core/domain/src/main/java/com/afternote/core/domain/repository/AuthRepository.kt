package com.afternote.core.domain.repository

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
}
