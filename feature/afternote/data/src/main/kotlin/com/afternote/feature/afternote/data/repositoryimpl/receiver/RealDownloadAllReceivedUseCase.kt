package com.afternote.feature.afternote.data.repositoryimpl.receiver

import com.afternote.feature.afternote.data.repositoryimpl.stub.StubDownloadAllReceivedUseCase
import com.afternote.feature.afternote.domain.usecase.receiver.DownloadAllReceivedUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealDownloadAllReceivedUseCase
    @Inject
    constructor(
        stub: StubDownloadAllReceivedUseCase,
    ) : DownloadAllReceivedUseCase by stub
