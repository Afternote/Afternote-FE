package com.afternote.feature.timeletter.domain.usecase

import com.afternote.feature.timeletter.domain.model.TimeLetterList
import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
import javax.inject.Inject

class GetTimeLettersUseCase
@Inject
constructor(
    private val timeLetterRepository: TimeLetterRepository,
) {
    suspend operator fun invoke(): TimeLetterList = timeLetterRepository.getTimeLetters()
}
