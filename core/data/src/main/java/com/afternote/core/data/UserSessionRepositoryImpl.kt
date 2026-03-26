package com.afternote.core.data

import com.afternote.core.domain.repository.AuthRepository
import com.afternote.core.domain.usecase.UserSessionRepository
import javax.inject.Inject

class UserSessionRepositoryImpl
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : UserSessionRepository {
        override suspend fun getUserId() = authRepository.getUserId()
    }
