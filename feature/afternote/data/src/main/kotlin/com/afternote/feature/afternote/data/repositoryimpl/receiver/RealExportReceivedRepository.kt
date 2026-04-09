package com.afternote.feature.afternote.data.repositoryimpl.receiver

import com.afternote.feature.afternote.data.repositoryimpl.stub.StubExportReceivedRepository
import com.afternote.feature.afternote.domain.port.ExportReceivedRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealExportReceivedRepository
    @Inject
    constructor(
        stub: StubExportReceivedRepository,
    ) : ExportReceivedRepository by stub
