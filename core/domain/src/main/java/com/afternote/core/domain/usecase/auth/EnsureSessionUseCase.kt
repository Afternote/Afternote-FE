package com.afternote.core.domain.usecase.auth

import com.afternote.core.domain.repository.auth.AuthRepository
import javax.inject.Inject

/**
 * 토큰 재발급 UseCase.
 */
class EnsureSessionUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) {
        suspend operator fun invoke(refreshToken: String) {
            // TODO:토큰의 남은 유효 기간 알려 주는 기능이 API에 추가된 후 구현 예정
            authRepository.rotateToken(refreshToken)
        }
    }
