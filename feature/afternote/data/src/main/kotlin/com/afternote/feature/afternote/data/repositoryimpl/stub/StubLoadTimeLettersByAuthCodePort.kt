package com.afternote.feature.afternote.data.repositoryimpl.stub

import com.afternote.feature.afternote.domain.model.receiver.LoadCountResult
import com.afternote.feature.afternote.domain.port.LoadTimeLettersByAuthCodePort
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubLoadTimeLettersByAuthCodePort
    @Inject
    constructor() : LoadTimeLettersByAuthCodePort {
        override suspend fun invoke(authCode: String): Result<LoadCountResult> = Result.success(LoadCountResult(0))
    }
