package com.afternote.feature.afternote.data.repositoryimpl.stub

import com.afternote.feature.afternote.domain.port.LoadSenderMessageByAuthCodePort
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubLoadSenderMessageByAuthCodePort
    @Inject
    constructor() : LoadSenderMessageByAuthCodePort {
        override suspend fun invoke(authCode: String): Result<String?> = Result.success(null)
    }
