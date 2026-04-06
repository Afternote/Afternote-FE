package com.afternote.feature.afternote.data.repositoryimpl.stub

import com.afternote.feature.afternote.domain.model.receiver.ReceivedAfternoteDetail
import com.afternote.feature.afternote.domain.usecase.receiver.GetAfternoteDetailByAuthCodeUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubGetAfternoteDetailByAuthCodeUseCase
    @Inject
    constructor() : GetAfternoteDetailByAuthCodeUseCase {
        override suspend fun invoke(
            authCode: String,
            afternoteId: Long,
        ): Result<ReceivedAfternoteDetail> = Result.failure(IllegalStateException("Receiver afternote detail not wired"))
    }
