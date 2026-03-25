package com.afternote.core.domain.usecase

import com.afternote.core.domain.repository.AuthRepository
import javax.inject.Inject

// 사용자 ID 조회 UseCase.
class GetUserIdUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) {
        /**
         * @return userId (Long) 또는 null (토큰이 없거나 userId를 찾을 수 없는 경우)
         */
        suspend operator fun invoke(): Long? = authRepository.getUserId()
    }
