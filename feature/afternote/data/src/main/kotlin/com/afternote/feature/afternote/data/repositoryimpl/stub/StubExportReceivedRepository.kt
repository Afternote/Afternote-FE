package com.afternote.feature.afternote.data.repositoryimpl.stub

import com.afternote.feature.afternote.domain.model.receiver.ReceivedExportBundle
import com.afternote.feature.afternote.domain.port.ExportReceivedRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubExportReceivedRepository
    @Inject
    constructor() : ExportReceivedRepository {
        override suspend fun saveToFile(bundle: ReceivedExportBundle): Result<Unit> = Result.success(Unit)
    }
