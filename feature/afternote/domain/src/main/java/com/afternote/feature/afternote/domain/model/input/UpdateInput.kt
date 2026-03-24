package com.afternote.feature.afternote.domain.model.input

// AfternoteRepository.update() 인자
data class UpdateInput(
    val category: String,
    val title: String,
    val processMethod: String? = null,
    val actions: List<String>? = null,
    val leaveMessage: String? = null,
    val credentials: CredentialsInput? = null,
    val receivers: List<ReceiverRefInput>? = null,
    val playlist: PlaylistInput? = null,
)

data class CredentialsInput(
    val id: String? = null,
    val password: String? = null,
)

data class ReceiverRefInput(
    val receiverId: Long,
)
