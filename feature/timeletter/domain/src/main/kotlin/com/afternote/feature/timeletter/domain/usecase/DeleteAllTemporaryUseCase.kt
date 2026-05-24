package com.afternote.feature.timeletter.domain.usecase

import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
import javax.inject.Inject

class DeleteAllTemporaryUseCase
    @Inject
    constructor(
        private val timeLetterRepository: TimeLetterRepository,
    ) {
    suspend operator fun invoke() = timeLetterRepository.deleteAllTemporary()
}
