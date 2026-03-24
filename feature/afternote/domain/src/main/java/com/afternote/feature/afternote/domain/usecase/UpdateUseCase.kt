package com.afternote.feature.afternote.domain.usecase

import com.afternote.feature.afternote.domain.model.input.UpdateInput
import com.afternote.feature.afternote.domain.repository.AfternoteRepository
import javax.inject.Inject

/**
 * 애프터노트 수정 UseCase.
 *
 * PATCH /api/afternotes/{afternoteId}
 */
class UpdateUseCase
    @Inject
    constructor(
        private val repository: AfternoteRepository,
    ) {
        suspend operator fun invoke(
            id: Long,
            body: UpdateInput,
        ): Result<Long> =
            repository.update(
                id = id,
                input = body,
            )
    }
