package com.afternote.feature.afternote.data.repositoryimpl.receiver

import com.afternote.feature.afternote.data.repositoryimpl.stub.StubLoadSenderMessageByAuthCodePort
import com.afternote.feature.afternote.domain.port.LoadSenderMessageByAuthCodePort
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealLoadSenderMessageByAuthCodePort
    @Inject
    constructor(
        stub: StubLoadSenderMessageByAuthCodePort,
    ) : LoadSenderMessageByAuthCodePort by stub
