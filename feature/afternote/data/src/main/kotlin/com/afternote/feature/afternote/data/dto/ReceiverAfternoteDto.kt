package com.afternote.feature.afternote.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReceivedAfternoteListResponse(
    @SerialName("afternotes") val afternotes: List<ReceivedAfternoteResponse> = emptyList(),
    @SerialName("totalCount") val totalCount: Int = 0,
)

@Serializable
data class ReceivedAfternoteResponse(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("leaveMessage") val leaveMessage: String? = null,
    @SerialName("senderId") val senderId: Long? = null,
    @SerialName("senderName") val senderName: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
)

@Serializable
data class ReceivedAfternoteDetailResponse(
    @SerialName("id") val id: Long,
    @SerialName("category") val category: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("actions") val actions: List<String> = emptyList(),
    @SerialName("leaveMessage") val leaveMessage: String? = null,
    @SerialName("senderName") val senderName: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("credentials") val credentials: ReceivedCredentialsInfo? = null,
    @SerialName("playlist") val playlist: ReceivedPlaylistInfo? = null,
)

@Serializable
data class ReceivedCredentialsInfo(
    @SerialName("id") val id: String? = null,
    @SerialName("password") val password: String? = null,
)

@Serializable
data class ReceivedPlaylistInfo(
    @SerialName("atmosphere") val atmosphere: String? = null,
    @SerialName("songs") val songs: List<ReceivedSongInfo> = emptyList(),
    @SerialName("memorialVideo") val memorialVideo: ReceivedMemorialVideoInfo? = null,
)

@Serializable
data class ReceivedSongInfo(
    @SerialName("title") val title: String,
    @SerialName("artist") val artist: String,
    @SerialName("coverUrl") val coverUrl: String? = null,
)

@Serializable
data class ReceivedMemorialVideoInfo(
    @SerialName("videoUrl") val videoUrl: String? = null,
    @SerialName("thumbnailUrl") val thumbnailUrl: String? = null,
)
