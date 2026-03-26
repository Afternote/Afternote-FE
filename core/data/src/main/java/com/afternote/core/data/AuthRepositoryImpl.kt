package com.afternote.core.data

import com.afternote.core.datastore.TokenManager
import com.afternote.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AuthRepositoryImpl
    @Inject
    constructor(
        private val tokenManager: TokenManager,
    ) : AuthRepository {
        override val isLoggedInFlow: Flow<Boolean>
            get() = tokenManager.isLoggedInFlow

        override suspend fun saveTokens(
            accessToken: String,
            refreshToken: String,
            userId: Long,
        ) = tokenManager.saveTokens(accessToken, refreshToken, userId)

        override suspend fun clearTokens() = tokenManager.clearTokens()

        override suspend fun updateTokens(
            accessToken: String,
            refreshToken: String,
        ) = tokenManager.updateTokens(accessToken, refreshToken)

        override suspend fun getAccessToken(): String? = tokenManager.getAccessToken()

        override suspend fun getRefreshToken(): String? = tokenManager.getRefreshToken()

        override suspend fun getUserId(): Long? = tokenManager.getUserId()
    }
