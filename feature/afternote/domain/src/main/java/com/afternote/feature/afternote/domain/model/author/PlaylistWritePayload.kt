package com.afternote.feature.afternote.domain.model.author

data class CreatePlaylistPayload(
    val title: String,
    val playlist: PlaylistWritePayload,
    val receiverIds: List<Long> = emptyList(),
)

data class PlaylistWritePayload(
    val profilePhoto: String? = null,
    val atmosphere: String? = null,
    val memorialPhotoUrl: String? = null,
    val songs: List<PlaylistSongPayload> = emptyList(),
    val memorialVideo: MemorialVideoPayload? = null,
)

data class MemorialVideoPayload(
    val videoUrl: String? = null,
    val thumbnailUrl: String? = null,
)

data class PlaylistSongPayload(
    val id: Long? = null,
    val title: String,
    val artist: String,
    val coverUrl: String? = null,
)
