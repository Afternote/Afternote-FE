package com.afternote.feature.afternote.data.repositoryimpl.receiver

import com.afternote.feature.afternote.data.repositoryimpl.stub.StubAuthorReceiversDirectoryPort
import com.afternote.feature.afternote.domain.port.AuthorReceiversDirectoryPort
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealAuthorReceiversDirectoryPort
    @Inject
    constructor(
        stub: StubAuthorReceiversDirectoryPort,
    ) : AuthorReceiversDirectoryPort by stub
