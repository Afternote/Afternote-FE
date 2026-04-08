package com.afternote.feature.afternote.data.repositoryimpl.stub

import com.afternote.feature.afternote.domain.model.receiver.ReceivedExportBundle
import com.afternote.feature.afternote.domain.usecase.receiver.DownloadAllReceivedUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubDownloadAllReceivedUseCase
    @Inject
    constructor() : DownloadAllReceivedUseCase {
        override suspend fun invoke(authCode: String): Result<ReceivedExportBundle> = Result.success(ReceivedExportBundle())
    }
