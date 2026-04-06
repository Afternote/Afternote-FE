package com.afternote.feature.afternote.data.repositoryimpl.stub

import com.afternote.feature.afternote.domain.port.CurrentAuthorUserIdPort
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubCurrentAuthorUserIdPort
    @Inject
    constructor() : CurrentAuthorUserIdPort {
        override fun invoke(): Long? = null
    }
