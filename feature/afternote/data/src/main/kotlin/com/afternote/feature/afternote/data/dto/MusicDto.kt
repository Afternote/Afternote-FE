package com.afternote.feature.afternote.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MusicSearchResponseDto(
    @SerialName("tracks") val tracks: List<MusicTrackDto> = emptyList(),
)

@Serializable
data class MusicTrackDto(
    @SerialName("artist") val artist: String,
    @SerialName("title") val title: String,
    @SerialName("albumImageUrl") val albumImageUrl: String? = null,
)

@Serializable
data class AfternotePlaylistDto(
    @SerialName("profilePhoto") val profilePhoto: String? = null,
    @SerialName("atmosphere") val atmosphere: String? = null,
    @SerialName("memorialPhotoUrl") val memorialPhotoUrl: String? = null,
    @SerialName("songs") val songs: List<AfternoteSongDto> = emptyList(),
    @SerialName("memorialVideo") val memorialVideo: AfternoteMemorialVideoDto? = null,
)

@Serializable
data class AfternoteSongDto(
    @SerialName("id") val id: Long? = null,
    @SerialName("title") val title: String,
    @SerialName("artist") val artist: String,
    @SerialName("coverUrl") val coverUrl: String? = null,
)

@Serializable
data class AfternoteDetailReceiverDto(
    @SerialName("receiverId") val receiverId: Long? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("relation") val relation: String? = null,
    @SerialName("phone") val phone: String? = null,
)

@Serializable
data class AfternoteListItemDto(
    @SerialName("afternoteId") val afternoteId: Long,
    @SerialName("title") val title: String,
    @SerialName("category") val category: String,
    @SerialName("createdAt") val createdAt: String,
)

@Serializable
data class AfternoteMemorialVideoDto(
    @SerialName("videoUrl") val videoUrl: String? = null,
    @SerialName("thumbnailUrl") val thumbnailUrl: String? = null,
)
