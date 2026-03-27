package com.afternote.core.domain.usecase.auth

import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.model.RotateTokenResult
import javax.inject.Inject

/**
 * 토큰 재발급 UseCase.
 */
class RotateTokenUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) {
        suspend operator fun invoke(refreshToken: String): Result<RotateTokenResult> = authRepository.rotateToken(refreshToken)
    }
