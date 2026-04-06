package com.afternote.feature.afternote.domain.usecase.receiver

import com.afternote.feature.afternote.domain.model.receiver.AfterNotesListResult

fun interface GetAfterNotesByAuthCodeUseCase {
    suspend operator fun invoke(authCode: String): Result<AfterNotesListResult>
}
