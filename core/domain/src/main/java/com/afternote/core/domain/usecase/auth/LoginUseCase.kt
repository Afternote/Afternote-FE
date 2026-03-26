package com.afternote.core.domain.usecase.auth

import com.afternote.core.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) {
        suspend operator fun invoke(
            accessToken: String,
            refreshToken: String,
            userId: Long,
        ) = authRepository.saveSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId,
        )
    }
