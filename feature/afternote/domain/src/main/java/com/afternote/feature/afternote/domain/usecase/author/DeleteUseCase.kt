package com.afternote.feature.afternote.domain.usecase.author

import com.afternote.feature.afternote.domain.repository.AfternoteRepository
import javax.inject.Inject

/**
 * 애프터노트 삭제 UseCase.
 *
 * DELETE /api/afternotes/{afternoteId}
 */
class DeleteUseCase
    @Inject
    constructor(
        private val repository: AfternoteRepository,
    ) {
        suspend operator fun invoke(id: Long): Result<Unit> = repository.delete(id = id)
    }
