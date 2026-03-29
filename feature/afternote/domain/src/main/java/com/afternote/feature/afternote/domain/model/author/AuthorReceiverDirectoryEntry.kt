package com.afternote.feature.afternote.domain.model.author

data class AuthorReceiverDirectoryEntry(
    val receiverId: Long,
    val name: String,
    val relation: String,
    val phone: String = "",
)
