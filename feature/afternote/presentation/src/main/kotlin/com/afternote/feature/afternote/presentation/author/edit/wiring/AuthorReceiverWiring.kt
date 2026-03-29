package com.afternote.feature.afternote.presentation.author.edit.wiring

import javax.inject.Inject
import javax.inject.Singleton

data class AuthorReceiverDirectoryEntry(
    val receiverId: Long,
    val name: String,
    val relation: String,
    val phone: String = "",
)

fun interface CurrentAuthorUserIdPort {
    operator fun invoke(): Long?
}

fun interface AuthorReceiversDirectoryPort {
    suspend operator fun invoke(userId: Long): Result<List<AuthorReceiverDirectoryEntry>>
}

@Singleton
class StubCurrentAuthorUserIdPort
    @Inject
    constructor() : CurrentAuthorUserIdPort {
        override fun invoke(): Long? = null
    }

@Singleton
class StubAuthorReceiversDirectoryPort
    @Inject
    constructor() : AuthorReceiversDirectoryPort {
        override suspend fun invoke(userId: Long): Result<List<AuthorReceiverDirectoryEntry>> = Result.success(emptyList())
    }
