package com.afternote.feature.afternote.domain.model.author

data class AfternoteUpdatePayload(
    val category: String,
    val title: String,
    val processMethod: String? = null,
    val actions: List<String>? = null,
    val leaveMessage: String? = null,
    val credentials: AfternoteAccountCredentials? = null,
    val receivers: List<ReceiverRefPayload>? = null,
    val playlist: PlaylistWritePayload? = null,
)

data class AfternoteAccountCredentials(
    val id: String? = null,
    val password: String? = null,
)

data class ReceiverRefPayload(
    val receiverId: Long,
)
