package com.afternote.core.data

import com.afternote.core.datastore.TokenManager
import com.afternote.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AuthRepositoryImpl
    @Inject
    constructor(
        val tokenManager: TokenManager,
    ) : AuthRepository {
        override suspend fun clearSession() = tokenManager.clearTokens()

        override suspend fun getAccessToken() = tokenManager.getAccessToken()

        override suspend fun getRefreshToken() = tokenManager.getRefreshToken()

        override suspend fun saveSession(
            accessToken: String,
            refreshToken: String,
            userId: Long,
        ) = tokenManager.saveTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId,
        )

        override val isLoggedIn: Flow<Boolean>
            get() = tokenManager.isLoggedInFlow

        override suspend fun updateTokens(
            accessToken: String,
            refreshToken: String,
        ) = tokenManager.updateTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
        )

        override suspend fun getUserId() = tokenManager.getUserId()
    }
