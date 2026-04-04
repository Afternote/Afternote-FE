package com.afternote.feature.afternote.domain.usecase.author

import com.afternote.feature.afternote.domain.repository.AfternoteRepository
import javax.inject.Inject

/**
 * 애프터노트 상세 조회 UseCase.
 *
 * GET /api/afternotes/{afternoteId}. When the API returns receivers with only receiverId,
 * name/relation are resolved from GET /users/receivers so they appear on the detail screen.
 */
class GetDetailUseCase
    @Inject
    constructor(
        private val repository: AfternoteRepository,
    ) {
        // 애프터노트 상세화면 수신자와 설정 상의 수신자가 충돌하는 문제를 서버에서 해결해야 함
        suspend operator fun invoke(id: Long) = repository.getDetail(id = id)
    }
