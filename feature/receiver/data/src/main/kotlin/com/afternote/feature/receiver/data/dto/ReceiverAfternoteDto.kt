package com.afternote.feature.receiver.data.dto

import com.afternote.feature.afternote.data.dto.LeaveMessageBlockDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReceivedAfternoteListDto(
    @SerialName("afternotes") val afternotes: List<ReceivedAfternoteDto> = emptyList(),
    @SerialName("totalCount") val totalCount: Int = 0,
)

@Serializable
data class ReceivedAfternoteDto(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
    @SerialName("category") val category: String? = null,
    @SerialName("leaveMessage") val leaveMessage: List<LeaveMessageBlockDto>? = null,
    @SerialName("senderId") val senderId: Long? = null,
    @SerialName("senderName") val senderName: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
)

@Serializable
data class ReceivedAfternoteDetailDto(
    @SerialName("id") val id: Long,
    @SerialName("category") val category: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("actions") val processingMethods: List<String> = emptyList(),
    @SerialName("leaveMessage") val leaveMessage: List<LeaveMessageBlockDto>? = null,
    @SerialName("senderName") val senderName: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("credentials") val credentials: ReceivedCredentialsDto? = null,
    @SerialName("playlist") val playlist: ReceivedPlaylistDto? = null,
)

@Serializable
data class ReceivedCredentialsDto(
    @SerialName("id") val id: String? = null,
    @SerialName("password") val password: String? = null,
)

@Serializable
data class ReceivedPlaylistDto(
    @SerialName("atmosphere") val atmosphere: String? = null,
    @SerialName("songs") val songs: List<ReceivedSongDto> = emptyList(),
    @SerialName("memorialVideo") val memorialVideo: ReceivedMemorialVideoDto? = null,
)

@Serializable
data class ReceivedSongDto(
    @SerialName("title") val title: String,
    @SerialName("artist") val artist: String,
    @SerialName("coverUrl") val coverUrl: String? = null,
)

@Serializable
data class ReceivedMemorialVideoDto(
    @SerialName("videoUrl") val videoUrl: String? = null,
    @SerialName("thumbnailUrl") val thumbnailUrl: String? = null,
)
