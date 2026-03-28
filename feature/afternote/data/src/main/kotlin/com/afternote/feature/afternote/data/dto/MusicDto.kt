package com.afternote.feature.afternote.data.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MusicSearchResponse(
    @SerialName("tracks") val tracks: List<MusicTrack> = emptyList(),
)

@Serializable
data class MusicTrack(
    @SerialName("artist") val artist: String,
    @SerialName("title") val title: String,
    @SerialName("albumImageUrl") val albumImageUrl: String? = null,
)

@Serializable
data class AfternotePlaylist(
    @SerialName("profilePhoto") val profilePhoto: String? = null,
    @SerialName("atmosphere") val atmosphere: String? = null,
    @SerialName("memorialPhotoUrl") val memorialPhotoUrl: String? = null,
    @SerialName("songs") val songs: List<AfternoteSong> = emptyList(),
    @SerialName("memorialVideo") val memorialVideo: AfternoteMemorialVideo? = null,
)

@Serializable
data class AfternoteSong(
    @SerialName("id") val id: Long? = null,
    @SerialName("title") val title: String,
    @SerialName("artist") val artist: String,
    @SerialName("coverUrl") val coverUrl: String? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AfternoteDetailReceiver(
    @SerialName("receiverId") val receiverId: Long? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("relation") val relation: String? = null,
    @SerialName("phone") val phone: String? = null,
)

@Serializable
data class AfternoteListItem(
    @SerialName("afternoteId") val afternoteId: Long,
    @SerialName("title") val title: String,
    @SerialName("category") val category: String,
    @SerialName("createdAt") val createdAt: String,
)

@Serializable
data class AfternoteMemorialVideo(
    @SerialName("videoUrl") val videoUrl: String? = null,
    @SerialName("thumbnailUrl") val thumbnailUrl: String? = null,
)
