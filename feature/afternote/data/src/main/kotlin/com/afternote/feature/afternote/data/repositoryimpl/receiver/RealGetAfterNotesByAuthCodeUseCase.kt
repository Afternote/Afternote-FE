package com.afternote.feature.afternote.data.repositoryimpl.receiver

import com.afternote.feature.afternote.data.repositoryimpl.stub.StubGetAfterNotesByAuthCodeUseCase
import com.afternote.feature.afternote.domain.usecase.receiver.GetAfterNotesByAuthCodeUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealGetAfterNotesByAuthCodeUseCase
    @Inject
    constructor(
        stub: StubGetAfterNotesByAuthCodeUseCase,
    ) : GetAfterNotesByAuthCodeUseCase by stub
