package com.afternote.feature.afternote.domain.port

import com.afternote.feature.afternote.domain.model.author.AuthorReceiverDirectoryEntry

fun interface CurrentAuthorUserIdPort {
    operator fun invoke(): Long?
}

fun interface AuthorReceiversDirectoryPort {
    suspend operator fun invoke(userId: Long): Result<List<AuthorReceiverDirectoryEntry>>
}
