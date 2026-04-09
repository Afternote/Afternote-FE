package com.afternote.feature.afternote.domain.model.author

data class CreateSocialPayload(
    val title: String,
    val processMethod: String,
    val actions: List<String>,
    val leaveMessage: String? = null,
    val credentials: AfternoteAccountCredentials? = null,
    val receiverIds: List<Long> = emptyList(),
)
