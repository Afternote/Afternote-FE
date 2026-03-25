package com.afternote.core.data

import com.afternote.core.datastore.TokenManager
import com.afternote.core.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl
    @Inject
    constructor(
        val tokenManager: TokenManager,
    ) : AuthRepository {
        override suspend fun getUserId() = tokenManager.getUserId()
    }
