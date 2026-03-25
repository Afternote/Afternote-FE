package com.afternote.core.domain.usecase

import com.afternote.core.domain.model.setting.ReceiverListItem
import com.afternote.core.domain.repository.UserRepository
import javax.inject.Inject

/**
 * 수신인 목록 조회 UseCase.
 * GET /users/receivers — 로그인한 사용자가 등록한 수신인 목록을 조회합니다.
 */
class GetReceiversUseCase
    @Inject
    constructor(
        private val userRepository: UserRepository,
    ) {
        /**
         * @param userId 사용자 ID (query, required)
         * @return 수신인 목록 [ReceiverListItem]
         */
        suspend operator fun invoke(userId: Long): Result<List<ReceiverListItem>> = userRepository.getReceivers(userId = userId)
    }
