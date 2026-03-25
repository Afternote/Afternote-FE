package com.afternote.core.data

import com.afternote.core.datastore.TokenManager
import com.afternote.core.domain.usecase.UserSessionRepository
import javax.inject.Inject

class UserSessionRepositoryImpl
    @Inject
    constructor(
        val tokenManager: TokenManager,
    ) : UserSessionRepository {
        override suspend fun getUserId() = tokenManager.getUserId()
    }
