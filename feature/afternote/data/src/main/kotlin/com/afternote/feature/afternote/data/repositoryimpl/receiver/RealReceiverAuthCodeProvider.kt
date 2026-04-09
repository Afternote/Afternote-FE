package com.afternote.feature.afternote.data.repositoryimpl.receiver

import com.afternote.feature.afternote.data.repositoryimpl.stub.StubReceiverAuthCodeProvider
import com.afternote.feature.afternote.domain.port.ReceiverAuthCodeProvider
import javax.inject.Inject
import javax.inject.Singleton

/** Placeholder for API-backed auth code; delegates to stub until wired. */
@Singleton
class RealReceiverAuthCodeProvider
    @Inject
    constructor(
        stub: StubReceiverAuthCodeProvider,
    ) : ReceiverAuthCodeProvider by stub
