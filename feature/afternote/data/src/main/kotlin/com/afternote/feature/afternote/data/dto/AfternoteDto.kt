package com.afternote.feature.afternote.data.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AfternoteCreateGalleryRequestDto(
    @EncodeDefault @SerialName("category") val category: String = "GALLERY",
    @SerialName("title") val title: String,
    @SerialName("actions") val actions: List<String>,
    @SerialName("leaveMessage") val leaveMessage: String? = null,
    @SerialName("receivers") val receivers: List<AfternoteReceiverRefDto>,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AfternoteCreatePlaylistRequestDto(
    @EncodeDefault @SerialName("category") val category: String = "PLAYLIST",
    @SerialName("title") val title: String,
    @SerialName("playlist") val playlist: AfternotePlaylistDto,
    @SerialName("receivers") val receivers: List<AfternoteReceiverRefDto> = emptyList(),
)

/** SOCIAL·BUSINESS 공용 생성 요청 — 두 카테고리는 바디 스키마가 동일해 [category] 값으로만 구분된다. */
@Serializable
data class AfternoteCreateAccountRequestDto(
    @SerialName("category") val category: String,
    @SerialName("title") val title: String,
    @SerialName("actions") val actions: List<String>,
    @SerialName("leaveMessage") val leaveMessage: String? = null,
    @SerialName("credentials") val credentials: AfternoteCredentialsDto? = null,
    @SerialName("receivers") val receivers: List<AfternoteReceiverRefDto> = emptyList(),
)

@Serializable
data class AfternoteUpdateRequestDto(
    @SerialName("category") val category: String,
    @SerialName("title") val title: String,
    @SerialName("actions") val actions: List<String>? = null,
    @SerialName("leaveMessage") val leaveMessage: String? = null,
    @SerialName("credentials") val credentials: AfternoteCredentialsDto? = null,
    @SerialName("receivers") val receivers: List<AfternoteReceiverRefDto>? = null,
    @SerialName("playlist") val playlist: AfternotePlaylistDto? = null,
)

@Serializable
data class AfternoteDetailDto(
    @SerialName("afternoteId") val afternoteId: Long,
    @SerialName("category") val category: String,
    @SerialName("title") val title: String,
    @SerialName("createdAt") val createdAt: String = "",
    @SerialName("updatedAt") val updatedAt: String = "",
    @SerialName("credentials") val credentials: AfternoteCredentialsDto? = null,
    @SerialName("receivers") val receivers: List<AfternoteDetailReceiverDto>? = null,
    @SerialName("actions") val actions: List<String>? = null,
    @SerialName("leaveMessage") val leaveMessage: String? = null,
    @SerialName("playlist") val playlist: AfternotePlaylistDto? = null,
)

@Serializable
data class AfternoteIdDto(
    @SerialName("afternoteId") val afternoteId: Long,
)

@Serializable
data class AfternotePageDto(
    @SerialName("content") val content: List<AfternoteListItemDto> = emptyList(),
    @SerialName("page") val page: Int = 0,
    @SerialName("size") val size: Int = 10,
    @SerialName("hasNext") val hasNext: Boolean = false,
)

@Serializable
data class AfternoteCredentialsDto(
    @SerialName("id") val id: String? = null,
    @SerialName("password") val password: String? = null,
)

@Serializable
data class AfternoteReceiverRefDto(
    @SerialName("receiverId") val receiverId: Long,
)
