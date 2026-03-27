package com.afternote.core.domain.usecase

import javax.inject.Inject

// 사용자 ID 조회 UseCase.
class GetUserIdUseCase
    @Inject
    constructor(
        private val userSessionRepository: UserSessionRepository,
    ) {
        /**
         * @return userId (Long) 또는 null (토큰이 없거나 userId를 찾을 수 없는 경우)
         */
        suspend operator fun invoke(): Long? = userSessionRepository.getUserId()
    }
