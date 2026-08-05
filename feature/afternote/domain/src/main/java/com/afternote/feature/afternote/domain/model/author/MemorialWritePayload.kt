package com.afternote.feature.afternote.domain.model.author

data class CreateMemorialPayload(
    val title: String,
    val memorial: MemorialWritePayload,
    val receiverIds: List<Long> = emptyList(),
)

data class MemorialWritePayload(
    val memorialPhotoUrl: String? = null,
    val songs: List<MemorialSongPayload> = emptyList(),
    val memorialVideo: MemorialVideoPayload? = null,
)

data class MemorialVideoPayload(
    val videoUrl: String? = null,
    val thumbnailUrl: String? = null,
)

data class MemorialSongPayload(
    val id: Long? = null,
    val title: String,
    val artist: String,
    val coverUrl: String? = null,
)
