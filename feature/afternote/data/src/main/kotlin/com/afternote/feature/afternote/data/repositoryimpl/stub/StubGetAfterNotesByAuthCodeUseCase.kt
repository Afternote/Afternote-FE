package com.afternote.feature.afternote.data.repositoryimpl.stub

import com.afternote.feature.afternote.domain.model.receiver.AfterNotesListResult
import com.afternote.feature.afternote.domain.usecase.receiver.GetAfterNotesByAuthCodeUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubGetAfterNotesByAuthCodeUseCase
    @Inject
    constructor() : GetAfterNotesByAuthCodeUseCase {
        override suspend fun invoke(authCode: String): Result<AfterNotesListResult> =
            Result.success(
                AfterNotesListResult(
                    items = emptyList(),
                    totalCount = 0,
                ),
            )
    }
