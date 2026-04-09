package com.afternote.feature.afternote.data.repositoryimpl.receiver

import com.afternote.feature.afternote.data.repositoryimpl.stub.StubLoadTimeLettersByAuthCodePort
import com.afternote.feature.afternote.domain.port.LoadTimeLettersByAuthCodePort
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealLoadTimeLettersByAuthCodePort
    @Inject
    constructor(
        stub: StubLoadTimeLettersByAuthCodePort,
    ) : LoadTimeLettersByAuthCodePort by stub
