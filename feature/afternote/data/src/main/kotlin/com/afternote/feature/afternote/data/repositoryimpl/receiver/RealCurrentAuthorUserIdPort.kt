package com.afternote.feature.afternote.data.repositoryimpl.receiver

import com.afternote.feature.afternote.data.repositoryimpl.stub.StubCurrentAuthorUserIdPort
import com.afternote.feature.afternote.domain.port.CurrentAuthorUserIdPort
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealCurrentAuthorUserIdPort
    @Inject
    constructor(
        stub: StubCurrentAuthorUserIdPort,
    ) : CurrentAuthorUserIdPort by stub
