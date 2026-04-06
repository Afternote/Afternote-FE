package com.afternote.feature.afternote.data.repositoryimpl.stub

import com.afternote.feature.afternote.domain.model.author.AuthorReceiverDirectoryEntry
import com.afternote.feature.afternote.domain.port.AuthorReceiversDirectoryPort
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubAuthorReceiversDirectoryPort
    @Inject
    constructor() : AuthorReceiversDirectoryPort {
        override suspend fun invoke(userId: Long): Result<List<AuthorReceiverDirectoryEntry>> = Result.success(emptyList())
    }
