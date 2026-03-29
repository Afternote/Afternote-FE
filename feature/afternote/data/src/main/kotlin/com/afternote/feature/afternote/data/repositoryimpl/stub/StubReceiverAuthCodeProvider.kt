package com.afternote.feature.afternote.data.repositoryimpl.stub

import com.afternote.feature.afternote.domain.port.ReceiverAuthCodeProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubReceiverAuthCodeProvider
    @Inject
    constructor() : ReceiverAuthCodeProvider {
        override fun currentAuthCode(): String? = null
    }
