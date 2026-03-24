package com.kuit.afternote.feature.afternote.domain.usecase

import com.afternote.feature.afternote.domain.model.input.UpdateInput
import com.afternote.feature.afternote.domain.repository.AfternoteRepository
import javax.inject.Inject

/**
 * 애프터노트 수정 UseCase.
 *
 * PATCH /api/afternotes/{afternoteId}
 */
class UpdateAfternoteUseCase
    @Inject
    constructor(
        private val repository: AfternoteRepository,
    ) {
        suspend operator fun invoke(
            afternoteId: Long,
            body: UpdateInput,
        ): Result<Long> =
            repository.update(
                id = afternoteId,
                input = body,
            )
    }
