package com.afternote.feature.afternote.data.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AfternoteCreateGalleryRequest(
    @EncodeDefault @SerialName("category") val category: String = "GALLERY",
    @SerialName("title") val title: String,
    @SerialName("actions") val actions: List<String>,
    @SerialName("leaveMessage") val leaveMessage: String? = null,
    @SerialName("receivers") val receivers: List<AfternoteReceiverRef>,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AfternoteCreatePlaylistRequest(
    @EncodeDefault @SerialName("category") val category: String = "PLAYLIST",
    @SerialName("title") val title: String,
    @SerialName("playlist") val playlist: AfternotePlaylist,
    @SerialName("receivers") val receivers: List<AfternoteReceiverRef> = emptyList(),
)

/** SOCIAL·BUSINESS 공용 생성 요청 — 두 카테고리는 바디 스키마가 동일해 [category] 값으로만 구분된다. */
@Serializable
data class AfternoteCreateAccountRequest(
    @SerialName("category") val category: String,
    @SerialName("title") val title: String,
    @SerialName("actions") val actions: List<String>,
    @SerialName("leaveMessage") val leaveMessage: String? = null,
    @SerialName("credentials") val credentials: AfternoteCredentials? = null,
    @SerialName("receivers") val receivers: List<AfternoteReceiverRef> = emptyList(),
)

@Serializable
data class AfternoteUpdateRequest(
    @SerialName("category") val category: String,
    @SerialName("title") val title: String,
    @SerialName("actions") val actions: List<String>? = null,
    @SerialName("leaveMessage") val leaveMessage: String? = null,
    @SerialName("credentials") val credentials: AfternoteCredentials? = null,
    @SerialName("receivers") val receivers: List<AfternoteReceiverRef>? = null,
    @SerialName("playlist") val playlist: AfternotePlaylist? = null,
)

@Serializable
data class AfternoteDetailResponse(
    @SerialName("afternoteId") val afternoteId: Long,
    @SerialName("category") val category: String,
    @SerialName("title") val title: String,
    @SerialName("createdAt") val createdAt: String = "",
    @SerialName("updatedAt") val updatedAt: String = "",
    @SerialName("credentials") val credentials: AfternoteCredentials? = null,
    @SerialName("receivers") val receivers: List<AfternoteDetailReceiver>? = null,
    @SerialName("actions") val actions: List<String>? = null,
    @SerialName("leaveMessage") val leaveMessage: String? = null,
    @SerialName("playlist") val playlist: AfternotePlaylist? = null,
)

@Serializable
data class AfternoteIdResponse(
    @SerialName("afternoteId") val afternoteId: Long,
)

@Serializable
data class AfternoteListResponse(
    @SerialName("content") val content: List<AfternoteListItem> = emptyList(),
    @SerialName("page") val page: Int = 0,
    @SerialName("size") val size: Int = 10,
    @SerialName("hasNext") val hasNext: Boolean = false,
)

@Serializable
data class AfternoteCredentials(
    @SerialName("id") val id: String? = null,
    @SerialName("password") val password: String? = null,
)

@Serializable
data class AfternoteReceiverRef(
    @SerialName("receiverId") val receiverId: Long,
)
