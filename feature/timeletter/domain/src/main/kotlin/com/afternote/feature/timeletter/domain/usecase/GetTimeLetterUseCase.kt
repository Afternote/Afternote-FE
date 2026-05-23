package com.afternote.feature.timeletter.domain.usecase

import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
import javax.inject.Inject

class GetTimeLetterUseCase @Inject constructor(
    private val timeLetterRepository: TimeLetterRepository,
) {
    suspend operator fun invoke(timeLetterId: Long): TimeLetter =
        timeLetterRepository.getTimeLetter(timeLetterId)
}
